// Generate a real FOL letter via runtime API with executive variables.
// Loads the FOL demo test variables, picks the executive sample, calls runtime generate.
const http = require('http');
const fs = require('fs');

const BASE = 'http://127.0.0.1:8080';
const MGMT = BASE + '/api/management/v1';
const RUNTIME = BASE + '/api/dev/v1';

function req(method, url, body, token, headers, raw){
  return new Promise((resolve, reject) => {
    const u = new URL(url); const h = { ...(headers || {}) }; if (token) h.Authorization = 'Bearer ' + token;
    const data = body ? JSON.stringify(body) : null;
    if (data && !h['Content-Type']) { h['Content-Type'] = 'application/json'; h['Content-Length'] = Buffer.byteLength(data); }
    const r = http.request({ method, hostname: u.hostname, port: u.port, path: u.pathname + u.search, headers: h, timeout: 180000 }, (res) => {
      const ch = []; res.on('data', c => ch.push(c)); res.on('end', () => {
        const buf = Buffer.concat(ch);
        if (raw) return resolve({ status: res.statusCode, headers: res.headers, body: buf });
        const t = buf.toString('utf8'); let j; try { j = t ? JSON.parse(t) : null; } catch (e) { j = t; }
        resolve({ status: res.statusCode, headers: res.headers, body: j });
      });
    }); r.on('error', reject); r.on('timeout', () => r.destroy(new Error('timeout'))); if (data) r.write(data); r.end();
  });
}
function list(b){ if(!b) return []; if(Array.isArray(b.result)) return b.result; if(b.result&&Array.isArray(b.result.content)) return b.result.content; if(Array.isArray(b.content)) return b.content; return []; }

(async () => {
  const ext = process.argv[2] || 'CORP-FOL-OFFER';
  const outFile = process.argv[3] || ('.tmp/generated_' + ext + '.docx');
  const ga = await req('POST', MGMT + '/auth/login', { username: '10000002', password: 'ChangeMe123!' });
  const tok = ga.body.result.accessToken;
  const tplRes = await req('GET', MGMT + '/templates?size=200', null, tok);
  const target = list(tplRes.body).find(x => x.externalId === ext);
  if (!target) { console.log('template not found'); process.exit(1); }
  console.log('template', ext, target.id, target.lifecycleStatus, 'rv=' + target.releaseVersion);
  // credential
  const cred = await req('POST', MGMT + '/templates/' + target.id + '/api/credentials', null, tok);
  const c = cred.body.result;
  console.log('credential', c.externalId);
  // Load executive test variables
  let variables = {};
  try {
    const raw = fs.readFileSync('deploy/demo-fol/config/fol-demo-test-variables.json', 'utf8').replace(/^﻿/, '');
    const tv = JSON.parse(raw);
    variables = tv.variables || tv;
  } catch (e) { console.log('warn: could not load FOL test vars, using minimal:', e.message); variables = { customerName: 'Pacific Rim Holdings Ltd.' }; }
  const body = {
    output: { format: 'DOCX', mode: 'SYNC_STREAM' },
    variables,
    requestId: 'req-real-' + Date.now(),
    idempotencyKey: 'idem-real-' + Date.now(),
  };
  const headers = {
    'X-Api-Credential-Id': c.externalId,
    'X-Api-Credential-Secret': c.secret,
    'X-Access-Account': 'svc-caller',
  };
  const r = await req('POST', RUNTIME + '/templates/' + ext + '/default/generate', body, null, headers, true);
  console.log('GENERATE status=' + r.status);
  console.log('documentId=' + (r.headers['documentid'] || r.headers['documentId']));
  console.log('invocationId=' + (r.headers['invocationid'] || r.headers['invocationId']));
  console.log('fidelityWarnings=' + (r.headers['fidelitywarningcount']));
  if (r.status === 200 && r.body && r.body.length) {
    fs.writeFileSync(outFile, r.body);
    console.log('saved ' + outFile + ' (' + r.body.length + ' bytes)');
  } else {
    console.log('body:', r.body.toString('utf8').slice(0, 800));
  }
})().catch(e => { console.error('ERROR:', e.message); process.exit(1); });
