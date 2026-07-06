// Lifecycle driver: advance a template from current state to PUBLISHED + API policy + credential.
// Usage: node .tmp/lifecycle.js <externalId> [releaseVersion]
const http = require('http');
const fs = require('fs');

const BASE = 'http://127.0.0.1:8080';
const MGMT = BASE + '/api/management/v1';
const RUNTIME = BASE + '/api/dev/v1';

function req(method, url, body, token, raw){
  return new Promise((resolve, reject) => {
    const u = new URL(url); const h = {}; if (token) h.Authorization = 'Bearer ' + token;
    const data = body ? JSON.stringify(body) : null;
    if (data) { h['Content-Type'] = 'application/json'; h['Content-Length'] = Buffer.byteLength(data); }
    const r = http.request({ method, hostname: u.hostname, port: u.port, path: u.pathname + u.search, headers: h, timeout: 120000 }, (res) => {
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
async function login(role){ const m={admin:'10000001',group_admin:'10000002',author:'10000003',tester:'10000006',approver:'10000007'}; const u=m[role]||'10000002'; const r=await req('POST',MGMT+'/auth/login',{username:u,password:'ChangeMe123!'}); return r.body.result.accessToken; }

(async () => {
  const ext = process.argv[2];
  const releaseVersion = process.argv[3] || '1.0.0';
  if (!ext) { console.log('usage: node lifecycle.js <externalId> [releaseVersion]'); process.exit(1); }
  const author = await login('author');
  const tester = await login('tester');
  const approver = await login('approver');
  const ga = await login('group_admin');

  const tplRes = await req('GET', MGMT + '/templates?size=200', null, author);
  const target = list(tplRes.body).find(x => x.externalId === ext);
  if (!target) { console.log('template not found:', ext); process.exit(1); }
  const tid = target.id;
  console.log(`[${ext}] id=${tid} status=${target.lifecycleStatus} subState=${target.approvalSubState} releaseVersion=${target.releaseVersion}`);

  // Helper to call lifecycle endpoint and report
  async function step(name, method, path, body, token, okStatus) {
    const r = await req(method, MGMT + path, body, token);
    const ok = r.status === (okStatus || 200);
    console.log(`  ${name}: ${r.status} ${ok?'OK':'FAIL'} ${typeof r.body==='string'?r.body.slice(0,200):JSON.stringify(r.body&&(r.body.error||r.body.result||r.body)).slice(0,200)}`);
    return r;
  }

  // 1. Ensure test data set exists (required sample)
  const tdsRes = await req('GET', MGMT + `/templates/${tid}/test-data-sets`, null, author);
  const tds = list(tdsRes.body);
  console.log(`  existing test data sets: ${tds.length}`);
  if (tds.length === 0) {
    await step('create test-data-set', 'POST', `/templates/${tid}/test-data-sets`, { name: 'Executive sample', required: true, variables: { customerName: 'Pacific Rim Holdings Ltd.' } }, author, 201);
  }

  // 2. preview test-generate
  await step('preview test-generate', 'POST', `/templates/${tid}/previews/test-generate`, { variables: { customerName: 'Pacific Rim Holdings Ltd.' } }, author);

  // 3. batch test
  const tdsAfter = list((await req('GET', MGMT + `/templates/${tid}/test-data-sets`, null, author)).body);
  const tdsId = tdsAfter[0] && (tdsAfter[0].testDataSetId || tdsAfter[0].id);
  if (tdsId) await step('batch test', 'POST', `/templates/${tid}/previews/batch-test`, { testDataSetIds: [tdsId] }, author);

  // 4. submit-test (if DRAFT)
  if (target.lifecycleStatus === 'DRAFT') {
    await step('submit-test', 'POST', `/templates/${tid}/lifecycle/submit-test`, { commentSummary: 'Real-bank demo ready for test' }, author);
  }

  // 5. test-decision PASSED (if TESTING)
  await step('test-decision PASSED', 'POST', `/templates/${tid}/lifecycle/test-decision`, { decision: 'PASSED', commentSummary: 'Real-bank demo test passed', fidelityViewedConfirmed: true, coverageViewedConfirmed: true, previewViewedConfirmed: true }, tester);

  // 6. approval-decision APPROVED
  await step('approval-decision APPROVED', 'POST', `/templates/${tid}/lifecycle/approval-decision`, { decision: 'APPROVED', commentSummary: 'Real-bank demo approved', keyEvidenceConfirmed: true }, approver);

  // 7. set API policy (required before publish)
  const groupCode = target.groupCode;
  const adGroupMap = { RETAIL: 'RETAIL_API', CORP: 'CORP_API', TRADE: 'TRADE_API', WEALTH: 'WEALTH_API' };
  const adGroup = adGroupMap[groupCode] || 'RETAIL_API';
  await step('set API policy', 'PUT', `/templates/${tid}/api/policy`, {
    allowedAdGroups: [adGroup, 'CORP_API', 'TRADE_API', 'WEALTH_API', 'RETAIL_API'],
    defaultRouteReleaseVersion: releaseVersion,
    outputFormats: ['DOCX', 'PDF'],
    outputModes: ['SYNC_STREAM', 'ASYNC_TASK'],
    batchEnabled: true,
    maxBatchSize: 50,
    docxEncryptionEnabled: false,
    pdfEncryptionEnabled: false,
  }, ga);

  // 8. publish
  await step('publish', 'POST', `/templates/${tid}/lifecycle/publish`, { releaseVersion }, ga);

  // 9. verify
  const finalRes = await req('GET', MGMT + '/templates?size=200', null, ga);
  const finalT = list(finalRes.body).find(x => x.externalId === ext);
  console.log(`[${ext}] FINAL status=${finalT.lifecycleStatus} releaseVersion=${finalT.releaseVersion}`);

  if (finalT.lifecycleStatus === 'PUBLISHED') {
    // 10. create credential
    const cred = await req('POST', MGMT + `/templates/${tid}/api/credentials`, null, ga);
    console.log(`  credential: externalId=${cred.body.result.externalId}`);
    fs.writeFileSync('.tmp/cred_' + ext + '.json', JSON.stringify({ externalId: cred.body.result.externalId, secret: cred.body.result.secret, templateExternalId: ext }, null, 2));
    console.log('  saved .tmp/cred_' + ext + '.json');
  }
})().catch(e => { console.error('ERROR:', e.message); process.exit(1); });
