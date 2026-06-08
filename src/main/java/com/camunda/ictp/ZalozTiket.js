/*
    client_id: 'camunda-identity-service',
    client_secret: 'B6wXVJAf6U4Wc5crYXXlladXM7lXQQnV',
    username: 'admin',
    password: 'BezpecneHeslo.123!',
*/

import https from 'https';
import fetch from 'node-fetch';

const TOKEN_ENDPOINT = 'https://dev-mgmt.prg1paas.t-cloud.eu/realms/camunda/protocol/openid-connect/token';
const ENGINE_REST = 'https://dev-kube.prg1paas.t-cloud.eu/engine-rest';

let accessToken = null;
let tokenExpiresAt = 0;
const TOKEN_REFRESH_MARGIN_MS = 30000;
const insecureAgent = new https.Agent({ rejectUnauthorized: false });

function decodeJwtPayload(token) {
  const payload = token.split('.')[1];
  return JSON.parse(Buffer.from(payload, 'base64url').toString('utf8'));
}

async function getAccessToken() {
  const now = Date.now();
  if (accessToken && now < tokenExpiresAt - TOKEN_REFRESH_MARGIN_MS) {
    return accessToken;
  }

  const body = new URLSearchParams({
    grant_type: 'password',
    client_id: 'camunda-identity-service',
    client_secret: 'B6wXVJAf6U4Wc5crYXXlladXM7lXQQnV',
    username: 'admin',
    password: 'BezpecneHeslo.123!',
    scope: 'openid profile email'
  });

  const res = await fetch(TOKEN_ENDPOINT, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
    agent: insecureAgent
  });

  const text = await res.text();
  if (!res.ok) {
    throw new Error(`Token request failed: ${res.status} ${text}`);
  }

  const json = JSON.parse(text);
  accessToken = json.access_token;

  const payload = decodeJwtPayload(accessToken);
  tokenExpiresAt = payload.exp ? payload.exp * 1000 : now + (json.expires_in * 1000);

  //console.log('TOKEN PREFIX:', accessToken.slice(0, 20));
  //console.log('TOKEN EXPIRES AT:', new Date(tokenExpiresAt).toISOString());
  //console.log('TOKEN LEFT MS:', tokenExpiresAt - Date.now());
  //console.log('CAMUNDA ROLES:', payload.resource_access?.camunda?.roles);

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
        topicName: 'servicecalls',
        lockDuration: 50000
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
  if (!res.ok) {
    throw new Error(`Complete failed ${res.status}: ${text}`);
  }
}

const WORKER_ID = 'node414-dev-mgmt';

function getVar(task, name) {
  const v = task.variables?.[name];
  return v?.value ?? v;
}

async function main() {
  while (true) {
    try {
      const tasks = await fetchAndLock();

      for (const task of tasks) {
        console.log('Received task:', task.id, 'Task topic:', task.topicName);
        console.log('Variables:', {
          service: getVar(task, 'service'),
          externalID: getVar(task, 'externalID'),
          category: getVar(task, 'category'),
          description: getVar(task, 'description'),
          information: getVar(task, 'information'),
          priority: getVar(task, 'priority')
        });

        await completeTask(task, {
          ticketNumber: { value: 'TCK-123', type: 'String' },
          status: { value: 'IN_PROGRESS', type: 'String' }
        });

        console.log('Completed task:', task.id);
      }
    } catch (err) {
      console.error('ERROR:', err);
    }

    await new Promise(resolve => setTimeout(resolve, 3000));
  }
}

main();