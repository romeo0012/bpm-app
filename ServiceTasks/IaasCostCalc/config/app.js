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
const WORKER_ID = process.env.WORKER_ID || 'node-calculate-cost';
const TOPIC_NAME = process.env.TOPIC_NAME || 'IaasCostCalc-calculate-cost';
const POLL_INTERVAL_MS = Number(process.env.POLL_INTERVAL_MS || 3000);
const LOCK_DURATION_MS = Number(process.env.LOCK_DURATION_MS || 50000);
const TOKEN_REFRESH_MARGIN_MS = 30000;

const DB_HOST = process.env.DB_HOST;
const DB_PORT = Number(process.env.DB_PORT || 5432);
const DB_USER = process.env.DB_USER;
const DB_PASSWORD = process.env.DB_PASSWORD;
const DB_NAME = process.env.DB_NAME;
const DB_TABLE = process.env.DB_TABLE;
const DB_SCHEMA = process.env.DB_SCHEMA || 'public';

console.log({
  ENGINE_REST: process.env.ENGINE_REST,
  TOKEN_ENDPOINT: process.env.TOKEN_ENDPOINT,
  TOPIC_NAME: process.env.TOPIC_NAME
});

if (!ENGINE_REST || !TOKEN_ENDPOINT || !CLIENT_ID || !CLIENT_SECRET || !USERNAME || !PASSWORD || !TOPIC_NAME || !DB_TABLE) {
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

async function loadConfigFromDatabase() {
  const sql = `
    SELECT data
    FROM ${DB_SCHEMA}.${DB_TABLE}
    ORDER BY id DESC
    LIMIT 1
  `;

  const { rows } = await pool.query(sql);

  if (!rows.length) {
    throw new Error(`No rows found in ${DB_SCHEMA}.${DB_TABLE}`);
  }

  const data = rows[0].data;
  if (!data || typeof data !== 'object') {
    throw new Error('Invalid JSONB data in database');
  }

  return data;
}

function parseDbValue(value, type) {
  if (value === null || value === undefined) return null;

  const t = String(type || '').toLowerCase();
  if (['number', 'double', 'decimal', 'int', 'integer', 'long', 'numeric'].includes(t)) {
    const n = Number(value);
    return Number.isFinite(n) ? n : 0;
  }
  if (['boolean', 'bool'].includes(t)) {
    return value === true || value === 1 || value === '1' || String(value).toLowerCase() === 'true';
  }
  if (t === 'json') {
    try {
      return typeof value === 'string' ? JSON.parse(value) : value;
    } catch {
      return value;
    }
  }
  return value;
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

function toCamundaValue(value) {
  if (typeof value === 'number') return { value, type: 'Double' };
  if (typeof value === 'boolean') return { value, type: 'Boolean' };
  if (typeof value === 'string') return { value, type: 'String' };
  if (value === null || value === undefined) return { value: null, type: 'Null' };
  return { value: JSON.stringify(value), type: 'Json' };
}

function getVar(task, name) {
  const v = task.variables?.[name];
  return v?.value ?? v;
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

async function main() {
  const input = await loadConfigFromDatabase();
  console.log('CONFIG INPUT FROM DB:', input);
  console.log(`✓ subscribed to topic ${TOPIC_NAME}`);

  while (true) {
    try {
      const tasks = await fetchAndLock();

      for (const task of tasks) {
        console.log('Received task:', task.id, 'Task topic:', task.topicName);
        console.log('Variables:', {
          BusinessKey: getVar(task, 'bk'),
          ReportName: getVar(task, 'ReportName'),
          Description: getVar(task, 'description')
        });

        const processVariables = {};
        for (const [key, item] of Object.entries(input)) {
            processVariables[key] = toCamundaValue(item.value);
            processVariables[`${key}_label`] = { value: item.label, type: 'String' };
            processVariables[`${key}_type`] = { value: item.type, type: 'String' };
        }

        await completeTask(task, processVariables);

        console.log('Completed task:', task.id);
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