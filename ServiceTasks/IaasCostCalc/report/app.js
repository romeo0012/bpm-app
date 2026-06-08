import 'dotenv/config';
import https from 'https';
import fetch from 'node-fetch';
import ExcelJS from 'exceljs';
import path from 'path';

const ENGINE_REST = process.env.ENGINE_REST;
const TOKEN_ENDPOINT = process.env.TOKEN_ENDPOINT;
const CLIENT_ID = process.env.CLIENT_ID;
const CLIENT_SECRET = process.env.CLIENT_SECRET;
const USERNAME = process.env.USERNAME;
const PASSWORD = process.env.PASSWORD;
const WORKER_ID = process.env.WORKER_ID;
const TOPIC_NAME = process.env.TOPIC_NAME;
const POLL_INTERVAL_MS = Number(process.env.POLL_INTERVAL_MS || 3000);
const LOCK_DURATION_MS = Number(process.env.LOCK_DURATION_MS || 50000);
const TOKEN_REFRESH_MARGIN_MS = Number(process.env.TOKEN_REFRESH_MARGIN_MS || 30000);

console.log({
  ENGINE_REST: process.env.ENGINE_REST,
  TOKEN_ENDPOINT: process.env.TOKEN_ENDPOINT,
  TOPIC_NAME: process.env.TOPIC_NAME
});

if (!ENGINE_REST || !TOKEN_ENDPOINT || !CLIENT_ID || !CLIENT_SECRET || !USERNAME || !PASSWORD || !TOPIC_NAME) {
  throw new Error('Missing required environment variables');
}

const insecureAgent = new https.Agent({ rejectUnauthorized: false });

let accessToken = null;
let tokenExpiresAt = 0;

function decodeJwtPayload(token) {
  return JSON.parse(Buffer.from(token.split('.')[1], 'base64url').toString('utf8'));
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

function getVar(task, name) {
  const v = task.variables?.[name];
  return v?.value ?? v;
}

function normalizeValue(v) {
  return v && typeof v === 'object' && 'value' in v ? v.value : v;
}

function buildConfigFromProcessVariables(variables) {
  const cfg = {};
  for (const [key, value] of Object.entries(variables || {})) {
    cfg[key] = normalizeValue(value);
  }
  return cfg;
}

function s(obj, key, def = '') {
  const v = normalizeValue(obj?.[key]);
  return v === null || v === undefined ? def : String(v);
}

function n(obj, key) {
  const v = normalizeValue(obj?.[key]);
  if (v === null || v === undefined || v === '') return 0;
  const num = Number(String(v).replace(/,/g, ''));
  return Number.isFinite(num) ? num : 0;
}

function b(obj, key) {
  const v = normalizeValue(obj?.[key]);
  return v === true || String(v).toLowerCase() === 'true' || v === 1 || v === '1';
}

function mark(value) {
  return value ? '√' : '-';
}

function numCell(value) {
  if (value === null || value === undefined || value === '' || value === '-') return '-';
  const num = Number(String(value).replace(/,/g, ''));
  return Number.isFinite(num) && num !== 0 ? num : '-';
}

async function fetchAndLock() {
  const token = await getAccessToken();
  const body = {
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
    body: JSON.stringify(body)
  });

  const text = await res.text();
  if (!text.trim()) return [];
  if (!res.ok) throw new Error(`fetchAndLock failed ${res.status}: ${text}`);
  return JSON.parse(text);
}

async function completeTask(taskId, variables) {
  const token = await getAccessToken();
  const res = await fetch(`${ENGINE_REST}/external-task/${taskId}/complete`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: 'application/json',
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ workerId: WORKER_ID, variables })
  });

  const text = await res.text();
  if (!res.ok) throw new Error(`Complete failed ${res.status}: ${text}`);
}

function addSection(ws, startRow, title, headers, rows, styles) {
  let row = startRow;
  ws.getRow(row).values = [title, ...headers];
  for (let i = 1; i <= headers.length + 1; i++) {
    ws.getRow(row).getCell(i).style = styles.header;
  }
  row++;

  for (const r of rows) {
    ws.getRow(row).values = r;
    ws.getRow(row).getCell(1).style = styles.label;
    for (let i = 2; i <= r.length; i++) {
      ws.getRow(row).getCell(i).style = styles.cell;
    }
    row++;
  }
  return row + 1;
}

async function buildReport(cfg, task) {
  const reportName = s(cfg, 'ReportName', 'CostCalc');

  const wb = new ExcelJS.Workbook();
  const ws = wb.addWorksheet(`Costs`);

  ws.columns = [
    { width: 40 },
    { width: 16 },
    { width: 16 },
    { width: 16 },
    { width: 16 },
    { width: 16 },
    { width: 16 },
    { width: 16 }
  ];

  const headerFill = {
    type: 'pattern',
    pattern: 'solid',
    fgColor: { argb: 'D9D9D9' }
  };

  const greenFill = {
    type: 'pattern',
    pattern: 'solid',
    fgColor: { argb: '92D050' }
  };

  const headerStyle = {
    font: { name: 'Arial', size: 11, bold: true },
    alignment: { horizontal: 'center' },
    fill: headerFill
  };

  const headerStyle2 = {
    font: { name: 'Arial', size: 11, bold: true },
    alignment: { horizontal: 'center' },
    fill: greenFill
  };

  const labelStyle = {
    font: { bold: false }
  };

  const cellStyle = {
    alignment: { horizontal: 'center' },
    numFmt: '#,##0'
  };

  let row = 1;

  const sections = [
    {
      title: 'Total costs',
      headers: ['Capex', 'Opex', '3y Total'],
      rows: [
        ['Total in EUR', numCell(cfg.capex), numCell(cfg.opex), numCell(cfg.capexopex)],
        ['Total in EUR inc. VAT', numCell(cfg.capexVAT), numCell(cfg.opexVAT), numCell(cfg.capexopexVAT)],
        ['Total in CZK', numCell(cfg.capexCZ), numCell(cfg.opexCZ), numCell(cfg.capexopexCZ)],
        ['Total in CZK inc. VAT', numCell(cfg.capexCZVAT), numCell(cfg.opexCZVAT), numCell(cfg.capexopexCZVAT)]
      ]
    },
    {
      title: 'Costs per year',
      headers: ['1st year', 'Year + 2', 'Year + 3'],
      rows: [
        ['Capex in EUR (External)', numCell(cfg.capex1e), numCell(cfg.capex2e), numCell(cfg.capex3e)],
        ['Capex in EUR (Internal)', numCell(cfg.capex1i), numCell(cfg.capex2i), numCell(cfg.capex3i)],
        ['Opex in EUR', numCell(cfg.opex1), numCell(cfg.opex2), numCell(cfg.opex3)],
        ['Capex in CZK (External)', numCell(cfg.capex1eCZ), numCell(cfg.capex2eCZ), numCell(cfg.capex3eCZ)],
        ['Capex in CZK (Internal)', numCell(cfg.capex1iCZ), numCell(cfg.capex2iCZ), numCell(cfg.capex3iCZ)],
        ['Opex in CZK', numCell(cfg.opex1CZ), numCell(cfg.opex2CZ), numCell(cfg.opex3CZ)]
      ]
    }
  ];

  for (const sec of sections) {
    ws.getRow(row).getCell(1).value = sec.title;
    ws.getRow(row).getCell(1).style = headerStyle2;
    for (let i = 2; i <= sec.headers.length + 1; i++) {
      ws.getRow(row).getCell(i).value = sec.headers[i - 2];
      ws.getRow(row).getCell(i).style = headerStyle2;
    }
    row++;

    for (const r of sec.rows) {
      ws.getRow(row).values = r;
      ws.getRow(row).getCell(1).style = labelStyle;
      for (let i = 2; i <= r.length; i++) {
        ws.getRow(row).getCell(i).style = cellStyle;
      }
      row++;
    }
    row++;
  }

  const buffer = await wb.xlsx.writeBuffer();
    return {
      fileName: `${getVar(task, 'bk')} ${s(cfg, 'ReportName', 'CostCalc')}.xlsx`,
      buffer
    };
}

async function main() {
  console.log(`✓ subscribed to topic ${TOPIC_NAME}`);

  while (true) {
    try {
      const tasks = await fetchAndLock();

      for (const task of tasks) {
        const cfg = buildConfigFromProcessVariables(task.variables);
        const report = await buildReport(cfg, task);

        const variables = {
          EXCEL_REPORT: {
            value: Buffer.from(report.buffer).toString('base64'),
            type: 'File',
            valueInfo: {
              filename: report.fileName,
              mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
              encoding: 'base64'
            }
          }
        };

        console.log('Creating report:', report.fileName);
        console.log('Report size:', Math.round(report.buffer.length / 1024) + ' KB');
        
        await completeTask(task.id, variables);
        console.log(`Completed task ${getVar(task, 'bk')} ${getVar(task, 'ReportName')}, id ${task.id}, report stored in EXCEL_REPORT variable.`);
      }
    } catch (err) {
      console.error('ERROR:', err);
    }

    await new Promise(r => setTimeout(r, POLL_INTERVAL_MS));
  }
}

main();