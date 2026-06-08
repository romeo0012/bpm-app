import 'dotenv/config';
import https from 'https';
import fetch from 'node-fetch';
import pg from 'pg';

const { Pool } = pg;

const ENGINE_REST = process.env.ENGINE_REST;
const TOKEN_ENDPOINT = process.env.TOKEN_ENDPOINT;
const CLIENT_ID = process.env.CLIENT_ID;
const CLIENT_SECRET = process.env.CLIENT_SECRET;
const USERNAME = process.env.USERNAME;
const PASSWORD = process.env.PASSWORD;
const WORKER_ID = process.env.WORKER_ID || 'node-config-update';
const TOPIC_NAME = process.env.TOPIC_NAME || 'cost calc';
const POLL_INTERVAL_MS = Number(process.env.POLL_INTERVAL_MS || 3000);
const LOCK_DURATION_MS = Number(process.env.LOCK_DURATION_MS || 50000);
const TOKEN_REFRESH_MARGIN_MS = 30000;

const DB_HOST = process.env.DB_HOST;
const DB_PORT = Number(process.env.DB_PORT || 5432);
const DB_USER = process.env.DB_USER;
const DB_PASSWORD = process.env.DB_PASSWORD;
const DB_NAME = process.env.DB_NAME;
const DB_SCHEMA = process.env.DB_SCHEMA || 'public';
const DB_TABLE = process.env.DB_TABLE || 'costcalc_config';

if (!ENGINE_REST || !TOKEN_ENDPOINT || !CLIENT_ID || !CLIENT_SECRET || !USERNAME || !PASSWORD || !TOPIC_NAME) {
  throw new Error('Missing required environment variables');
}

if (!DB_HOST || !DB_USER || !DB_NAME) {
  throw new Error('Missing required database environment variables');
}

const pool = new Pool({
  host: DB_HOST,
  port: DB_PORT,
  user: DB_USER,
  password: DB_PASSWORD,
  database: DB_NAME
});

let accessToken = null;
let tokenExpiresAt = 0;
const insecureAgent = new https.Agent({ rejectUnauthorized: false });

function decodeJwtPayload(token) {
  const payload = token.split('.')[1];
  return JSON.parse(Buffer.from(payload, 'base64url').toString('utf8'));
}

function normalizeValue(v) {
  return v && typeof v === 'object' && 'value' in v ? v.value : v;
}

function toPlainValue(v) {
  const x = normalizeValue(v);
  if (x === null || x === undefined) return null;
  if (typeof x === 'number' || typeof x === 'boolean') return x;
  if (typeof x === 'string') {
    const t = x.trim();
    if (t === '') return null;
    if (t === 'true') return true;
    if (t === 'false') return false;
    const n = Number(t);
    if (Number.isFinite(n) && t === String(n)) return n;
    return x;
  }
  return x;
}

function buildJsonFromVariables(task) {
  const out = {};
  const vars = task.variables || {};

  const baseKeys = new Set();
  for (const key of Object.keys(vars)) {
    if (key.endsWith('_label') || key.endsWith('_type')) {
      const baseKey = key.replace(/_(label|type)$/, '');
      baseKeys.add(baseKey);
    }
  }

  for (const baseKey of baseKeys) {
    const valueVar = vars[baseKey];
    const labelVar = vars[`${baseKey}_label`];
    const typeVar = vars[`${baseKey}_type`];

    const obj = {};

    if (valueVar) obj.value = toPlainValue(valueVar);
    if (labelVar) obj.label = toPlainValue(labelVar);
    if (typeVar) obj.type = toPlainValue(typeVar);

    if ((obj.value === null || obj.value === undefined) && obj.type) {
      const defaults = {
        number: 0,
        Long: 0,
        boolean: false,
        Boolean: false,
        String: ''
      };
      obj.value = defaults[obj.type] ?? null;
    }

    if (Object.keys(obj).length > 0) {
      out[baseKey] = obj;
    }
  }

  return out;
}

function sortDeep(value) {
  if (Array.isArray(value)) return value.map(sortDeep);
  if (value && typeof value === 'object') {
    return Object.keys(value)
      .sort()
      .reduce((acc, key) => {
        acc[key] = sortDeep(value[key]);
        return acc;
      }, {});
  }
  return value;
}

function isEqualDeep(a, b) {
  return JSON.stringify(sortDeep(a)) === JSON.stringify(sortDeep(b));
}

function getChangedFields(oldObj, newObj) {
  const changes = [];
  const keys = new Set([
    ...Object.keys(oldObj || {}),
    ...Object.keys(newObj || {})
  ]);

  for (const key of keys) {
    const oldVal = oldObj?.[key];
    const newVal = newObj?.[key];

    if (!isEqualDeep(oldVal, newVal)) {
      changes.push({
        key,
        oldValue: oldVal,
        newValue: newVal
      });
    }
  }

  return changes;
}

async function getAccessToken() {
  const now = Date.now();
  if (accessToken && now < tokenExpiresAt - TOKEN_REFRESH_MARGIN_MS) return accessToken;

  const body = new URLSearchParams({
    grant_type: 'password',
    client_id: CLIENT_ID,
    client_secret: CLIENT_SECRET,
    username: USERNAME,
    password: PASSWORD,
    scope: 'openid profile email'
  });

  const res = await fetch(TOKEN_ENDPOINT, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
    agent: insecureAgent
  });

  const text = await res.text();
  if (!res.ok) throw new Error(`Token request failed: ${res.status} ${text}`);

  const json = JSON.parse(text);
  accessToken = json.access_token;

  const payload = decodeJwtPayload(accessToken);
  tokenExpiresAt = payload.exp ? payload.exp * 1000 : now + (json.expires_in * 1000);
  return accessToken;
}

async function fetchAndLock() {
  const token = await getAccessToken();

  const payload = {
    workerId: WORKER_ID,
    maxTasks: 1,
    usePriority: true,
    topics: [
      {
        topicName: TOPIC_NAME,
        lockDuration: LOCK_DURATION_MS
      }
    ]
  };

  const res = await fetch(`${ENGINE_REST}/external-task/fetchAndLock`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: 'application/json',
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload)
  });

  const text = await res.text();
  if (!text || !text.trim()) return [];
  if (!res.ok) throw new Error(`fetchAndLock failed ${res.status}: ${text}`);

  return JSON.parse(text);
}

async function completeTask(task, variables = {}) {
  const token = await getAccessToken();

  const res = await fetch(`${ENGINE_REST}/external-task/${task.id}/complete`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: 'application/json',
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      workerId: WORKER_ID,
      variables
    })
  });

  const text = await res.text();
  if (!res.ok) throw new Error(`Complete failed ${res.status}: ${text}`);
}

async function updateConfigJson(dataObj, configDbId) {
  const sql = `
    UPDATE ${DB_SCHEMA}.${DB_TABLE}
    SET data = $1::jsonb
    WHERE id = $2
  `;
  await pool.query(sql, [JSON.stringify(dataObj), configDbId]);
}

async function loadCurrentConfig(configDbId) {
  const { rows } = await pool.query(
    `SELECT data FROM ${DB_SCHEMA}.${DB_TABLE} WHERE id = $1 LIMIT 1`,
    [configDbId]
  );
  return rows[0]?.data ?? {};
}

async function main() {
  console.log(`✓ subscribed to topic ${TOPIC_NAME}`);

  while (true) {
    try {
      const tasks = await fetchAndLock();

      for (const task of tasks) {
        console.log('Received task:', task.id, 'topic:', task.topicName);

        const jsonData = buildJsonFromVariables(task);

        const { rows } = await pool.query(
          `SELECT id FROM ${DB_SCHEMA}.${DB_TABLE} ORDER BY id DESC LIMIT 1`
        );

        if (!rows.length) {
          throw new Error(`No rows found in ${DB_SCHEMA}.${DB_TABLE}`);
        }

        const configDbId = rows[0].id;
        const currentData = await loadCurrentConfig(configDbId);
        const changedFields = getChangedFields(currentData, jsonData);

        if (changedFields.length === 0) {
          console.log(`Task ${task.id}: no changes detected for row ${configDbId}`);
        } else {
          console.log(`Task ${task.id}: changed fields for row ${configDbId}:`);
          for (const change of changedFields) {
            console.log(
              `[CHANGED] ${change.key}:`,
              JSON.stringify(change.oldValue),
              '=>',
              JSON.stringify(change.newValue)
            );
          }
        }

        await updateConfigJson(jsonData, configDbId);

        const variables = {
          updatedConfigId: { value: Number(configDbId), type: 'Long' },
          updatedAt: { value: new Date().toISOString(), type: 'String' },
          status: { value: 'UPDATED', type: 'String' }
        };

        await completeTask(task, variables);
        console.log(`Updated DB row ${configDbId} and completed task ${task.id}`);
      }
    } catch (err) {
      console.error('ERROR:', err);
    }

    await new Promise(r => setTimeout(r, POLL_INTERVAL_MS));
  }
}

main().catch(err => {
  console.error(err);
  process.exit(1);
});