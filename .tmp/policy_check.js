const http = require('http');
function req(method, url, body, token){
  return new Promise((resolve, reject) => {
    const u = new URL(url); const h = {}; if (token) h.Authorization = 'Bearer ' + token;
    const data = body ? JSON.stringify(body) : null;
    if (data) { h['Content-Type'] = 'application/json'; h['Content-Length'] = Buffer.byteLength(data); }
    const r = http.request({ method, hostname: u.hostname, port: u.port, path: u.pathname + u.search, headers: h, timeout: 30000 }, (res) => {
      const ch = []; res.on('data', c => ch.push(c)); res.on('end', () => { const t = Buffer.concat(ch).toString('utf8'); let j; try { j = JSON.parse(t); } catch (e) { j = t; } resolve({ status: res.statusCode, body: j }); });
    }); r.on('error', reject); if (data) r.write(data); r.end();
  });
}
function list(b){ if(!b) return []; if(Array.isArray(b.result)) return b.result; if(b.result&&Array.isArray(b.result.content)) return b.result.content; if(Array.isArray(b.content)) return b.content; return []; }

(async () => {
  const args = process.argv.slice(2);
  const action = args[0] || 'policy';
  const login = await req('POST', 'http://127.0.0.1:8080/api/management/v1/auth/login', { username: '10000002', password: 'ChangeMe123!' });
  const tok = login.body.result.accessToken;
  const tpl = await req('GET', 'http://127.0.0.1:8080/api/management/v1/templates?size=200', null, tok);
  const target = list(tpl.body).find(x => x.externalId === (args[1] || 'DEMO-FULL-FLOW-LETTER'));
  if (!target) { console.log('not found'); return; }
  console.log('template', target.id, target.lifecycleStatus, 'releaseVersion=', target.releaseVersion);
  if (action === 'policy') {
    const pol = await req('GET', 'http://127.0.0.1:8080/api/management/v1/templates/' + target.id + '/api/policy', null, tok);
    console.log('policy status', pol.status);
    console.log(JSON.stringify(pol.body.result, null, 2).slice(0, 1200));
  } else if (action === 'setpolicy') {
    const body = {
      allowedAdGroups: args[2] ? args[2].split(',') : ['RETAIL_API', 'CORP_API', 'TRADE_API', 'WEALTH_API'],
      defaultRouteReleaseVersion: target.releaseVersion || '1.0.0',
      outputFormats: ['DOCX', 'PDF'],
      outputModes: ['SYNC_STREAM', 'ASYNC_TASK'],
      batchEnabled: true,
      maxBatchSize: 50,
      docxEncryptionEnabled: false,
      pdfEncryptionEnabled: false,
    };
    const r = await req('PUT', 'http://127.0.0.1:8080/api/management/v1/templates/' + target.id + '/api/policy', body, tok);
    console.log('setpolicy status', r.status);
    console.log(JSON.stringify(r.body, null, 2).slice(0, 600));
  }
})();
