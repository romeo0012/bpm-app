import 'dotenv/config';
import https from 'https';
import fetch from 'node-fetch';

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

console.log({
  ENGINE_REST: process.env.ENGINE_REST,
  TOKEN_ENDPOINT: process.env.TOKEN_ENDPOINT,
  TOPIC_NAME: process.env.TOPIC_NAME
});

if (!ENGINE_REST || !TOKEN_ENDPOINT || !CLIENT_ID || !CLIENT_SECRET || !USERNAME || !PASSWORD || !TOPIC_NAME) {
  throw new Error('Missing required environment variables');
}

let accessToken = null;
let tokenExpiresAt = 0;
const insecureAgent = new https.Agent({ rejectUnauthorized: false });

function decodeJwtPayload(token) {
  const payload = token.split('.')[1];
  return JSON.parse(Buffer.from(payload, 'base64url').toString('utf8'));
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

function num(v) {
  const x = normalizeValue(v);
  if (x === null || x === undefined || x === '') return 0;
  const n = Number(x);
  return Number.isFinite(n) ? n : 0;
}

function bool(v) {
  const x = normalizeValue(v);
  return x === true || x === 'true' || x === 1 || x === '1';
}

function str(v) {
  const x = normalizeValue(v);
  return x === null || x === undefined ? '' : String(x);
}

function fmt(n) {
  return Number(n).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

function calc(cfg) {
  const VAT = num(cfg.VAT);
  const Eur2Czk = num(cfg.Eur2Czk);
  const VmRatio = num(cfg.VmRatio);
  const CpuBlock = num(cfg.CpuBlock);
  const StorageBlock = num(cfg.StorageBlock);
  const DesignCost = num(cfg.DesignCost);
  const ServerCost = num(cfg.ServerCost);
  const StorageCost = num(cfg.StorageCost);
  const LicenseCost = num(cfg.LicenseCost);
  const MaintanaceCost = num(cfg.MaintanaceCost);
  const PkiMgmtCost = num(cfg.CertDeployCost);
  const LeasedLineCostPerMbps = num(cfg.LeasedLineCostPerMbps);
  const Site2SsiteVpnCostPerMbps = num(cfg.Site2SsiteVpnCostPerMbps);
  const TransactionCost = num(cfg.TransactionCost);
  const BusLogCost = num(cfg.BusLogCost);
  const TechLogCost = num(cfg.TechLogCost);

  const sum = (...keys) => keys.reduce((a, k) => a + num(cfg[k]), 0);
  const on = k => bool(cfg[k]);

  let capex1 = 0, capex2 = 0, capex3 = 0;
  let capex1e = 0, capex2e = 0, capex3e = 0;
  let capex1i = 0, capex2i = 0, capex3i = 0;
  let opex1 = 0, opex2 = 0, opex3 = 0;
  let tmp = 0;

  tmp = sum('dvip', 'tvip', 'qvip', 'bvip', 'pvip') * num(cfg.VipDeployCost);
  console.log('VIP COST:', tmp);
  capex1 += tmp; on('VipIsExt') ? capex1e += tmp : capex1i += tmp;

  tmp = sum('dfw', 'tfw', 'qfw', 'pfw') * num(cfg.FwDeployCost);
  console.log('FW RULE COST:', tmp);
  capex1 += tmp; capex1i += tmp;

  tmp = sum('dsrv', 'tsrv', 'qsrv', 'psrv') * num(cfg.SrvDeployCost);
  on('srvenc') ? tmp += tmp + 4 * num(cfg.EncDeployCost) : tmp;
  console.log('SERVER COST:', tmp);
  capex1 += tmp; capex1i += tmp;

  tmp = sum('ddb', 'tdb', 'qdb', 'pdb') * num(cfg.DbDeployCost);
  on('dbenc') ? tmp += tmp + 4 * num(cfg.EncDeployCost) : tmp;
  console.log('DATABASE COST:', tmp);
  capex1 += tmp; capex1i += tmp;

  const pubCertCount = sum('dcerp', 'tcerp', 'qcerp', 'pcerp');
  tmp = pubCertCount * num(cfg.PublicCertCost) + pubCertCount * num(cfg.CertDeployCost);
  console.log('PUBLIC CERTIFICATE COST:', tmp);
  capex1 += tmp; capex1i += tmp;

  const intCertCount = sum('dceri', 'tceri', 'qceri', 'pceri');
  tmp = intCertCount * num(cfg.InternalCertCost) + intCertCount * num(cfg.CertDeployCost);
  console.log('INTERNAL CERTIFICATE COST:', tmp);
  capex1 += tmp; capex1i += tmp;

  tmp = sum('dftq', 'tftq', 'qftq', 'pftq') * num(cfg.FtqDeployCost);
  console.log('FILE TRANSFER QUEUE COST:', tmp);
  capex1 += tmp; capex1i += tmp;

  tmp = sum('dmsq', 'tmsq', 'qmsq', 'pmsq') * num(cfg.MsqDeployCost);
  console.log('KAFKA TOPIC COST:', tmp);
  capex1 += tmp; capex1i += tmp;

  tmp = num(cfg.vpn1) * num(cfg.IndividualVpnCost);
  console.log('INDIVIDUAL VPN COST:', tmp);
  capex1 += tmp; capex1i += tmp;

  tmp = sum('tdes1', 'tdes2', 'tdes3', 'tdes4', 'tdes5', 'tdes6') * DesignCost;
  capex1 += tmp; capex1i += tmp;

  tmp = sum('pdes1', 'pdes2', 'pdes3', 'pdes4', 'pdes5', 'pdes6') * DesignCost;
  capex1 += tmp; capex1i += tmp;

  const sla = str(cfg.sla);
  const continuity = str(cfg.continuity);

  if (sla.includes('8x5')) { capex1 += capex1 * num(cfg.Sla8x5costRatio) / 100; capex1i += capex1i * num(cfg.Sla8x5costRatio) / 100; capex1e += capex1e * num(cfg.Sla8x5costRatio) / 100; }
  if (sla.includes('24x7')) { capex1 += capex1 * num(cfg.Sla24x7costRatio) / 100; capex1i += capex1i * num(cfg.Sla24x7costRatio) / 100; capex1e += capex1e * num(cfg.Sla24x7costRatio) / 100; }
  if (continuity.includes('passive')) { capex1 += capex1 * num(cfg.IctPassiveCostRatio) / 100; capex1i += capex1i * num(cfg.IctPassiveCostRatio) / 100; capex1e += capex1e * num(cfg.IctPassiveCostRatio) / 100; }
  if (continuity.includes('active')) { capex1 += capex1 * num(cfg.IctActiveCostRatio) / 100; capex1i += capex1i * num(cfg.IctActiveCostRatio) / 100; capex1e += capex1e * num(cfg.IctActiveCostRatio) / 100; }

  tmp = num(cfg.prg1) * num(cfg.ProgramingCost); capex1 += tmp; on('PrgIsExt') ? capex1e += tmp : capex1i += tmp;
  tmp = num(cfg.dpl1) * num(cfg.AppDeploymentCost); capex1 += tmp; on('DplIsExt') ? capex1e += tmp : capex1i += tmp;

  tmp = num(cfg.prg2) * num(cfg.ProgramingCost); capex2 += tmp; on('PrgIsExt') ? capex2e += tmp : capex2i += tmp;
  tmp = num(cfg.dpl2) * num(cfg.AppDeploymentCost); capex2 += tmp; on('DplIsExt') ? capex2e += tmp : capex2i += tmp;

  tmp = num(cfg.prg3) * num(cfg.ProgramingCost); capex3 += tmp; on('PrgIsExt') ? capex3e += tmp : capex3i += tmp;
  tmp = num(cfg.dpl3) * num(cfg.AppDeploymentCost); capex3 += tmp; on('DplIsExt') ? capex3e += tmp : capex3i += tmp;

  opex1 += (ServerCost * num(cfg.cpu1) / VmRatio / CpuBlock) + (StorageCost * num(cfg.hdd1) / StorageBlock) + (StorageCost * num(cfg.database1) / StorageBlock);
  opex2 += (ServerCost * num(cfg.cpu2) / VmRatio / CpuBlock) + (StorageCost * num(cfg.hdd2) / StorageBlock) + (StorageCost * num(cfg.database2) / StorageBlock);
  opex3 += (ServerCost * num(cfg.cpu3) / VmRatio / CpuBlock) + (StorageCost * num(cfg.hdd3) / StorageBlock) + (StorageCost * num(cfg.database3) / StorageBlock);

  opex1 += num(cfg.lic1) * LicenseCost + num(cfg.mtn1) * MaintanaceCost;
  opex2 += num(cfg.lic2) * LicenseCost + num(cfg.mtn2) * MaintanaceCost;
  opex3 += num(cfg.lic3) * LicenseCost + num(cfg.mtn3) * MaintanaceCost;

  const pkiCount = sum('dcerp', 'tcerp', 'qcerp', 'bcerp', 'pcerp', 'dceri', 'tceri', 'qceri', 'bceri', 'pceri');
  opex1 += pkiCount * PkiMgmtCost;
  opex2 += pkiCount * PkiMgmtCost;
  opex3 += pkiCount * PkiMgmtCost;

  opex1 += num(cfg.llb1) * LeasedLineCostPerMbps;
  opex2 += num(cfg.llb2) * LeasedLineCostPerMbps;
  opex3 += num(cfg.llb3) * LeasedLineCostPerMbps;

  if (num(cfg.ssvpn1) > 0) opex1 += num(cfg.ssvpn1) * Site2SsiteVpnCostPerMbps;
  if (num(cfg.ssvpn2) > 0) opex2 += num(cfg.ssvpn2) * Site2SsiteVpnCostPerMbps;
  if (num(cfg.ssvpn3) > 0) opex3 += num(cfg.ssvpn3) * Site2SsiteVpnCostPerMbps;

  if (num(cfg.llb1) > 0) opex1 = num(cfg.llb1) * LeasedLineCostPerMbps;
  if (num(cfg.llb2) > 0) opex2 = num(cfg.llb2) * LeasedLineCostPerMbps;
  if (num(cfg.llb3) > 0) opex3 = num(cfg.llb3) * LeasedLineCostPerMbps;

  if (num(cfg.trs1) > 0) opex1 += num(cfg.trs1) * TransactionCost;
  if (num(cfg.trs2) > 0) opex2 += num(cfg.trs2) * TransactionCost;
  if (num(cfg.trs3) > 0) opex3 += num(cfg.trs3) * TransactionCost;
  if (bool(cfg.buslog1)) opex1 += BusLogCost;
  if (bool(cfg.buslog2)) opex2 += BusLogCost;
  if (bool(cfg.buslog3)) opex3 += BusLogCost;
  if (bool(cfg.techlog1)) opex1 += TechLogCost;
  if (bool(cfg.techlog2)) opex2 += TechLogCost;
  if (bool(cfg.techlog3)) opex3 += TechLogCost;

  const addNet = x => {
    if (bool(cfg.NurInt)) x += num(cfg.NurIntCost);
    if (bool(cfg.NurWin)) x += num(cfg.NurWinCost);
    if (bool(cfg.NurSia)) x += num(cfg.NurSiaCost);
    if (bool(cfg.NurFis)) x += num(cfg.NurFisCost);
    if (bool(cfg.NurWan)) x += num(cfg.NurWanCost);
    if (bool(cfg.NurPrgDcMpls)) x += num(cfg.NurPrgDcMplsCost);
    if (bool(cfg.PrgInt)) x += num(cfg.PrgIntCost);
    if (bool(cfg.PrgGpe)) x += num(cfg.PrgGpeCost);
    if (bool(cfg.PrgSia)) x += num(cfg.PrgSiaCost);
    return x;
  };

  opex1 = addNet(opex1);
  opex2 = addNet(opex2);
  opex3 = addNet(opex3);

  const capex = capex1 + capex2 + capex3;
  const opex = opex1 + opex2 + opex3;

  return {
    capex1e: fmt(capex1e), capex2e: fmt(capex2e), capex3e: fmt(capex3e),
    capex1i: fmt(capex1i), capex2i: fmt(capex2i), capex3i: fmt(capex3i),
    capex1: fmt(capex1), capex2: fmt(capex2), capex3: fmt(capex3),
    capex: fmt(capex), capexVAT: fmt(capex * (100 + VAT) / 100),
    opex1: fmt(opex1), opex2: fmt(opex2), opex3: fmt(opex3),
    opex: fmt(opex), opexVAT: fmt(opex * (100 + VAT) / 100),
    capexopex: fmt(capex + opex), capexopexVAT: fmt((capex + opex) * (100 + VAT) / 100),
    capex1eCZ: fmt(capex1e * Eur2Czk), capex2eCZ: fmt(capex2e * Eur2Czk), capex3eCZ: fmt(capex3e * Eur2Czk),
    capex1iCZ: fmt(capex1i * Eur2Czk), capex2iCZ: fmt(capex2i * Eur2Czk), capex3iCZ: fmt(capex3i * Eur2Czk),
    capex1CZ: fmt(capex * Eur2Czk), capex2CZ: fmt(0), capex3CZ: fmt(0),
    capexCZ: fmt(capex * Eur2Czk), capexCZVAT: fmt(capex * Eur2Czk * (100 + VAT) / 100),
    opex1CZ: fmt(opex1 * Eur2Czk), opex2CZ: fmt(opex2 * Eur2Czk), opex3CZ: fmt(opex3 * Eur2Czk),
    opexCZ: fmt(opex * Eur2Czk), opexCZVAT: fmt(opex * Eur2Czk * (100 + VAT) / 100),
    capexopexCZ: fmt((capex + opex) * Eur2Czk), capexopexCZVAT: fmt((capex + opex) * Eur2Czk * (100 + VAT) / 100)
  };
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
  if (!text || !text.trim()) return [];
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

async function main() {
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

        const cfg = buildConfigFromProcessVariables(task.variables);
        const result = calc(cfg);

        const variables = {};
        for (const [k, v] of Object.entries(result)) {
          variables[k] = { value: v, type: 'String' };
        }

        await completeTask(task.id, variables);
        console.log('Completed task:', task.id);
      }
    } catch (err) {
      console.error('ERROR:', err);
    }

    await new Promise(r => setTimeout(r, POLL_INTERVAL_MS));
  }
}

main();