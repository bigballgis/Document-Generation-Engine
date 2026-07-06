// Real-bank-grade demo driver — inspect / generate via the full pipeline.
// Usage: node .tmp/demo_driver.js inspect
//        node .tmp/demo_driver.js generate <externalId> <outFile>
const http = require('http');
const fs = require('fs');
const path = require('path');

const BASE = process.env.BACKEND_URL || 'http://127.0.0.1:8080';
const MGMT = BASE + '/api/management/v1';
const RUNTIME = BASE + '/api/dev/v1';

const USERS = {
  admin: ['10000001', 'ChangeMe123!'],
  group_admin: ['10000002', 'ChangeMe123!'],
  author: ['10000003', 'ChangeMe123!'],
};

function resultList(body) {
  if (!body) return [];
  if (Array.isArray(body.result)) return body.result;
  if (body.result && Array.isArray(body.result.content)) return body.result.content;
  if (Array.isArray(body.content)) return body.content;
  return [];
}

function request(method, urlStr, { token, body, headers: extraHeaders = {}, raw = false } = {}) {
  return new Promise((resolve, reject) => {
    const u = new URL(urlStr);
    const bodyData = body != null ? JSON.stringify(body) : null;
    const headers = { ...(extraHeaders || {}) };
    if (token) headers['Authorization'] = 'Bearer ' + token;
    if (bodyData) { headers['Content-Type'] = 'application/json'; headers['Content-Length'] = Buffer.byteLength(bodyData); }
    const req = http.request({
      method, protocol: u.protocol, hostname: u.hostname, port: u.port, path: u.pathname + u.search,
      headers, timeout: 120000,
    }, (res) => {
      const chunks = [];
      res.on('data', (c) => chunks.push(c));
      res.on('end', () => {
        const buf = Buffer.concat(chunks);
        if (raw) return resolve({ status: res.statusCode, headers: res.headers, body: buf });
        const text = buf.toString('utf8');
        let json = null;
        try { json = text ? JSON.parse(text) : null; } catch (e) { json = text; }
        resolve({ status: res.statusCode, headers: res.headers, body: json });
      });
    });
    req.on('error', reject);
    req.on('timeout', () => { req.destroy(new Error('timeout')); });
    if (bodyData) req.write(bodyData);
    req.end();
  });
}

async function login(role) {
  const [u, p] = USERS[role];
  const r = await request('POST', MGMT + '/auth/login', { body: { username: u, password: p } });
  if (r.status !== 200) throw new Error(`login ${role} failed: ${r.status} ${JSON.stringify(r.body)}`);
  return r.body.result.accessToken;
}

async function main() {
  const [, , cmd, ...rest] = process.argv;
  const action = cmd || 'inspect';
  if (action === 'inspect') {
    const tok = await login('group_admin');
    const tpl = await request('GET', MGMT + '/templates?size=200', { token: tok });
    console.log('=== TEMPLATES ===');
    const tpls = resultList(tpl.body);
    if (tpl.status === 200) {
      for (const t of tpls) {
        console.log(`${(t.externalId||'?').padEnd(32)} | ${(t.lifecycleStatus||'?').padEnd(14)} | ${(t.groupCode||'?').padEnd(8)} | ${(t.name||'').slice(0,50)}`);
      }
    } else { console.log(tpl.body); }
    const m = await request('GET', MGMT + '/masters?size=200', { token: tok });
    console.log('=== MASTERS ===');
    const masters = resultList(m.body);
    if (m.status === 200) {
      for (const x of masters) {
        console.log(`${(x.name||'').padEnd(50)} | ${(x.reviewState||'NONE').padEnd(10)} | ${x.groupCode||'?'}`);
      }
    } else { console.log(m.body); }
  } else if (action === 'generate') {
    const ext = rest[0];
    const outFile = rest[1] || `.tmp/generated_${ext}.docx`;
    const ga = await login('group_admin');
    const tpl = await request('GET', MGMT + '/templates?size=200', { token: ga });
    const target = resultList(tpl.body).find((t) => t.externalId === ext);
    if (!target) throw new Error('template ' + ext + ' not found');
    const tid = target.id;
    console.log(`template: ${ext} id=${tid} status=${target.lifecycleStatus}`);
    const cred = await request('POST', MGMT + `/templates/${tid}/api/credentials`, { token: ga });
    console.log('credential externalId=' + cred.body.result.externalId);
    const headers = {
      'X-Api-Credential-Id': cred.body.result.externalId,
      'X-Api-Credential-Secret': cred.body.result.secret,
      'X-Access-Account': 'svc-caller',
    };
    let vars = { customerName: 'Executive Sample Customer' };
    if (ext === 'DEMO-CREDIT-LIMIT-CONFIRM') {
      vars = { customerName:'Northgate Manufacturing Ltd.', facilityReference:'CORP-CL-2026-88421', creditLimit:'GBP 12,500,000.00', reviewDate:'2026-06-30', includeOverdraft:true, overdraftLimit:'GBP 1,000,000.00' };
    } else if (ext === 'DEMO-MORTGAGE-APPROVAL') {
      vars = { borrowerName:'Mr Oliver Hartley & Mrs Sarah Hartley', propertyAddress:'14 Willow Close, Bristol BS8 4QT', loanAmount:'GBP 385,000.00', interestRate:'4.29% fixed for 5 years', termYears:25, approvalDate:'2026-06-20' };
    }
    const body = {
      output: { format: 'DOCX', mode: 'SYNC_STREAM' },
      variables: vars,
      requestId: 'req-demo-' + Date.now(),
      idempotencyKey: 'idem-demo-' + Date.now(),
    };
    const r = await request('POST', RUNTIME + `/templates/${ext}/default/generate`, { headers, body, raw: true });
    console.log('GENERATE status=' + r.status);
    console.log('documentId=' + (r.headers['documentid'] || r.headers['documentId']));
    if (r.status === 200 && r.body && r.body.length) {
      fs.writeFileSync(outFile, r.body);
      console.log(`saved ${outFile} (${r.body.length} bytes)`);
    } else {
      console.log('body:', r.body.toString('utf8').slice(0, 800));
    }
  } else {
    console.log('Unknown action:', action);
  }
}

main().catch((e) => { console.error('ERROR:', e.message); process.exit(1); });
