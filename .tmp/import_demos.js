// Comprehensive demo importer: imports demo packages via API directly (no PowerShell).
// For each demo: ensure master uploaded/approved, create template, upsert variables,
// apply bindings, wire content module refs, create test data set, validate.
const http = require('http');
const fs = require('fs');
const path = require('path');

const MGMT = 'http://127.0.0.1:8080/api/management/v1';

function req(method, url, body, token, multipart){
  return new Promise((resolve, reject) => {
    const u = new URL(url); const h = {}; if (token) h.Authorization = 'Bearer ' + token;
    if (multipart) {
      // Use pwsh with -Form for multipart upload
      const { execFileSync } = require('child_process');
      const formArgs = [];
      for (const [k,v] of Object.entries(multipart)) {
        if (v.file) formArgs.push(`-Form`, `'${k}=@${v.file.replace(/\\/g,'/')}'`);
        else formArgs.push(`-Form`, `'${k}=${String(v)}'`);
      }
      const psScript = `$ErrorActionPreference='Stop'; $ProgressPreference='SilentlyContinue';
$h=@{Authorization='Bearer ${token}'};
$form = @{${Object.entries(multipart).map(([k,v]) => v.file ? `'${k}'=(Get-Item -LiteralPath '${v.file.replace(/\\/g,'/')}')` : `'${k}'='${String(v).replace(/'/g,"''")}'`).join(';')}};
try { Invoke-RestMethod -Method ${method} -Uri '${url}' -Headers $h -Form $form | ConvertTo-Json -Depth 100 }
catch { Write-Output ('ERR: ' + $_.Exception.Message); if ($_.ErrorDetails) { Write-Output $_.ErrorDetails.Message } }`;
      try {
        const out = execFileSync('pwsh', ['-NoProfile','-ExecutionPolicy','Bypass','-Command', psScript], {encoding:'utf8', maxBuffer: 50*1024*1024});
        const trimmed = out.trim();
        if (trimmed.startsWith('ERR')) { resolve({status:0, body: trimmed.slice(0,600)}); return; }
        try { resolve({status:200, body:JSON.parse(trimmed)}); }
        catch(e) { resolve({status:0, body: 'parse fail: '+trimmed.slice(0,400)}); }
      } catch(e) {
        const msg = e.stderr ? e.stderr.toString().slice(0,500) : e.message;
        resolve({status:0, body: msg});
      }
      return;
    }
    const data = body ? JSON.stringify(body) : null;
    if (data) { h['Content-Type'] = 'application/json'; h['Content-Length'] = Buffer.byteLength(data); }
    const r = http.request({ method, hostname: u.hostname, port: u.port, path: u.pathname + u.search, headers: h, timeout: 120000 }, (res) => {
      const ch = []; res.on('data', c => ch.push(c)); res.on('end', () => { const t = Buffer.concat(ch).toString('utf8'); let j; try { j = t ? JSON.parse(t) : null; } catch (e) { j = t; } resolve({ status: res.statusCode, body: j }); });
    }); r.on('error', reject); r.on('timeout', () => r.destroy(new Error('timeout'))); if (data) r.write(data); r.end();
  });
}
function list(b){ if(!b) return []; if(Array.isArray(b.result)) return b.result; if(b.result&&Array.isArray(b.result.content)) return b.result.content; if(Array.isArray(b.content)) return b.content; return []; }
async function login(role){ const m={admin:'10000001',group_admin:'10000002',author:'10000003'}; const u=m[role]; const r=await req('POST',MGMT+'/auth/login',{username:u,password:'ChangeMe123!'}); return r.body.result.accessToken; }

async function ensureMaster(name, groupCode, layoutVersion, docxRelPath, masterDescription, adminToken, gaToken, anchorFilter) {
  // Derive a master name if not provided (multi-template demos share layout but need distinct masters)
  const effectiveName = name || `${groupCode} ${layoutVersion} Master`;
  const ms = list((await req('GET', MGMT + '/masters?size=200', null, gaToken)).body);
  let master = ms.find(m => m.name === effectiveName);
  const docxPath = path.join('deploy', docxRelPath);
  if (!master) {
    console.log('  uploading master', effectiveName);
    const created = await req('POST', MGMT + '/masters', null, gaToken, { groupCode, name: effectiveName, description: `${masterDescription} (${layoutVersion})`, file: {file: docxPath} });
    if (created.status !== 200 || !created.body || !created.body.result) {
      console.log('   UPLOAD FAILED status='+created.status, JSON.stringify(created.body).slice(0,400));
      throw new Error('master upload failed for '+name);
    }
    master = created.body.result;
    await req('POST', MGMT + `/masters/${master.id}/submit-review`, { changeSummary: 'Demo import' }, gaToken);
    master = (await req('POST', MGMT + `/masters/${master.id}/review`, { decision: 'APPROVED', commentSummary: 'Demo approved' }, adminToken)).body.result;
  } else {
    const needsRefresh = !(master.description || '').includes(layoutVersion);
    if (needsRefresh) {
      console.log('  refreshing master layout', effectiveName, layoutVersion);
      await req('PUT', MGMT + `/masters/${master.id}/file`, null, gaToken, { file: {file: docxPath} });
      await req('POST', MGMT + `/masters/${master.id}/submit-review`, { changeSummary: 'Demo layout refresh' }, gaToken);
      master = (await req('POST', MGMT + `/masters/${master.id}/review`, { decision: 'APPROVED', commentSummary: 'Demo approved' }, adminToken)).body.result;
    } else {
      console.log('  master exists, layout unchanged');
    }
  }
  return master;
}

async function importDemo(demoDir, cfg, tokens) {
  const marker = cfg.catalogMarker;
  console.log(`\n=== ${demoDir} (marker: ${marker}) ===`);
  const configDir = path.join('deploy', demoDir, 'config');
  const readJson = (f) => JSON.parse(fs.readFileSync(path.join(configDir, f), 'utf8').replace(/^﻿/, ''));
  const variables = readJson(`${cfg.variablesFile}`).variables;
  const bindingOverlays = readJson(`${cfg.bindingOverlaysFile}`).bindings;
  const manifest = readJson(`${cfg.manifestFile}`);
  let testData;
  try { testData = readJson(`${cfg.testDataFile}`); } catch(e) { testData = null; }

  const templates = cfg.templates;
  for (const tdef of templates) {
    console.log(` -- template ${tdef.externalId}`);
    const anchorFilter = tdef.anchorFilter || null;
    const master = await ensureMaster(cfg.masterName, cfg.groupCode, cfg.masterLayoutVersion, tdef.masterDocx, cfg.masterDescription, tokens.admin, tokens.ga, anchorFilter);
    // find or create template
    let tpl = list((await req('GET', MGMT + '/templates?size=200', null, tokens.author)).body).find(t => t.externalId === tdef.externalId);
    if (!tpl) {
      const created = await req('POST', MGMT + '/templates', {
        externalId: tdef.externalId, groupCode: cfg.groupCode, name: tdef.name,
        description: `${tdef.description || ''} [${marker}]`, masterId: master.id
      }, tokens.author);
      tpl = created.body.result;
      console.log('   created template', tpl.id);
    } else {
      console.log('   template exists', tpl.id, tpl.lifecycleStatus);
      // patch description marker if missing
      if (!(tpl.description || '').includes(`[${marker}]`)) {
        await req('PATCH', MGMT + `/templates/${tpl.id}`, { description: `${tdef.description || ''} [${marker}]` }, tokens.author);
      }
    }
    const tid = tpl.id;
    // upsert variables
    let varOk = 0, varFail = 0;
    for (const v of variables) {
      const r = await req('PUT', MGMT + `/templates/${tid}/variables/${v.key}`, {
        variableKey: v.key, variableType: v.type, required: !!v.required, description: v.label || v.key
      }, tokens.author);
      if (r.status === 200) varOk++; else { varFail++; console.log('   var fail', v.key, r.status, r.body&&r.body.error&&r.body.error.message); }
    }
    console.log(`   variables: ${varOk} ok, ${varFail} fail`);
    // bindings — filter to this template's anchors if manifest specifies
    let bindingsToApply = bindingOverlays;
    if (manifest.templates) {
      const tEntry = manifest.templates.find(t => t.externalId === tdef.externalId);
      if (tEntry && tEntry.anchorIds) {
        bindingsToApply = {};
        for (const a of tEntry.anchorIds) if (bindingOverlays[a]) bindingsToApply[a] = bindingOverlays[a];
      }
    }
    for (const [anchor, sc] of Object.entries(bindingsToApply)) {
      const r = await req('PUT', MGMT + `/templates/${tid}/bindings/${anchor}`, {
        anchorId: anchor, declaredContentType: 'TEXT', structuredContentJson: JSON.stringify(sc)
      }, tokens.author);
      if (r.status !== 200) console.log('   binding fail', anchor, r.status, r.body&&r.body.error&&r.body.error.message);
    }
    console.log(`   bindings: ${Object.keys(bindingsToApply).length} applied`);
    // content module refs
    if (manifest.contentModuleRefs) {
      for (const ref of manifest.contentModuleRefs) {
        const r = await req('PUT', MGMT + `/templates/${tid}/content-module-references/${ref.referenceKey}`, {
          referenceKey: ref.referenceKey, moduleId: ref.moduleCode, semanticVersion: ref.semanticVersion || '1.0.0'
        }, tokens.author);
        if (r.status !== 200) console.log('   modref fail', ref.referenceKey, r.status, r.body&&r.body.error&&r.body.error.message);
      }
      console.log(`   module refs: ${manifest.contentModuleRefs.length} applied`);
    }
    // test data set
    if (testData) {
      const sets = testData.testDataSets || [testData];
      for (const s of sets) {
        // only apply if this template is the right one (match by id heuristic)
        if (tdef.externalId.includes('GUARANTEE') && s.id.includes('lc')) continue;
        if (tdef.externalId.includes('LC-NOTICE') && s.id.includes('guarantee')) continue;
        if (tdef.externalId.includes('RATE-CHANGE') && s.id.includes('overdue')) continue;
        if (tdef.externalId.includes('OVERDUE') && s.id.includes('rate')) continue;
        if (tdef.externalId.includes('RENEWAL') && s.id.includes('annual-review')) continue;
        if (tdef.externalId.includes('ANNUAL-REVIEW') && s.id.includes('renewal')) continue;
        const body = { name: s.label || s.name, required: true, variables: s.variables };
        const existing = list((await req('GET', MGMT + `/templates/${tid}/test-data-sets`, null, tokens.author)).body).find(x => x.name === body.name);
        if (existing) {
          await req('PUT', MGMT + `/templates/${tid}/test-data-sets/${existing.testDataSetId || existing.id}`, body, tokens.author);
        } else {
          await req('POST', MGMT + `/templates/${tid}/test-data-sets`, body, tokens.author);
        }
      }
      console.log(`   test data sets applied`);
    }
    // validate
    const v = await req('POST', MGMT + `/templates/${tid}/bindings/validate`, {}, tokens.author);
    console.log(`   validate: ${v.status}`, v.body.result ? JSON.stringify(v.body.result.summary||{}) : '');
  }
}

(async () => {
  const tokens = { admin: await login('admin'), ga: await login('group_admin'), author: await login('author') };
  // First apply all SQL via docker
  const { execSync } = require('child_process');
  const demos = [
    'demo-credit-limit','demo-mortgage','demo-trade-lc','demo-collection','demo-annual-review','demo-wealth'
  ];
  for (const d of demos) {
    const sqlDir = path.join('deploy', d, 'sql');
    if (fs.existsSync(sqlDir)) {
      for (const f of fs.readdirSync(sqlDir).filter(x=>x.endsWith('.sql')).sort()) {
        console.log(`applying SQL ${d}/${f}`);
        try {
          execSync(`docker exec -i docgen-postgres psql -U docgen -d docgen -v ON_ERROR_STOP=1 -f - < ${path.join(sqlDir, f)}`, {stdio:'pipe'});
        } catch(e) { console.log('  SQL error:', e.stderr ? e.stderr.toString().slice(0,300) : e.message); }
      }
    }
  }
  // Define demo configs
  const configs = {
    'demo-credit-limit': {
      groupCode:'CORP', masterName:'Meridian Credit Limit Master', masterLayoutVersion:'credit-limit-layout-v2-dual-page',
      masterDescription:'Real bank-grade credit limit confirmation master', variablesFile:'credit-limit-variables.json',
      bindingOverlaysFile:'credit-limit-binding-overlays.json', manifestFile:'credit-limit-catalog-manifest.json',
      testDataFile:'credit-limit-demo-test-variables.json',
      templates:[{externalId:'DEMO-CREDIT-LIMIT-CONFIRM', name:'Credit Facility Confirmation', description:'Real foreign-bank credit facility confirmation', masterDocx:'demo-credit-limit/assets/credit-limit-master.docx'}],
    },
    'demo-mortgage': {
      groupCode:'RETAIL', masterName:'Meridian Mortgage Approval Master', masterLayoutVersion:'mortgage-layout-v2-dual-page',
      masterDescription:'Real bank-grade residential mortgage approval master', variablesFile:'mortgage-variables.json',
      bindingOverlaysFile:'mortgage-binding-overlays.json', manifestFile:'mortgage-catalog-manifest.json',
      testDataFile:'mortgage-demo-test-variables.json',
      templates:[{externalId:'DEMO-MORTGAGE-APPROVAL', name:'Residential Mortgage Approval', description:'Real foreign-bank residential mortgage approval', masterDocx:'demo-mortgage/assets/mortgage-approval-master.docx'}],
    },
    'demo-trade-lc': {
      groupCode:'TRADE', masterName:null, masterLayoutVersion:'trade-lc-layout-v2-global-page',
      masterDescription:'Real bank-grade trade finance master', variablesFile:'trade-lc-variables.json',
      bindingOverlaysFile:'trade-lc-binding-overlays.json', manifestFile:'trade-lc-catalog-manifest.json',
      testDataFile:'trade-lc-demo-test-variables.json',
      templates:[
        {externalId:'DEMO-TRADE-LC-NOTICE', name:'Documentary Credit Advice', description:'Real foreign-bank documentary credit advice', masterDocx:'demo-trade-lc/assets/trade-lc-notice-master.docx', anchorFilter:['TRADE_LC_BODY','TRADE_LC_ATTACHMENT']},
        {externalId:'DEMO-TRADE-GUARANTEE-NOTICE', name:'Bank Guarantee Notice', description:'Real foreign-bank bank guarantee notice', masterDocx:'demo-trade-lc/assets/trade-guarantee-notice-master.docx', anchorFilter:['TRADE_GUARANTEE_BODY','TRADE_GUARANTEE_ATTACHMENT']},
      ],
    },
    'demo-collection': {
      groupCode:'RETAIL', masterName:null, masterLayoutVersion:'collection-layout-v2-global-page',
      masterDescription:'Real bank-grade retail notices master', variablesFile:'collection-variables.json',
      bindingOverlaysFile:'collection-binding-overlays.json', manifestFile:'collection-catalog-manifest.json',
      testDataFile:'collection-demo-test-variables.json',
      templates:[
        {externalId:'DEMO-RATE-CHANGE-NOTICE', name:'Rate Change Notice', description:'Real foreign-bank rate change notice', masterDocx:'demo-collection/assets/rate-change-notice-master.docx', anchorFilter:['RATE_CHANGE_BODY']},
        {externalId:'DEMO-OVERDUE-COLLECTION', name:'Overdue Collection Notice', description:'Real foreign-bank overdue collection notice', masterDocx:'demo-collection/assets/overdue-collection-master.docx', anchorFilter:['OVERDUE_COLLECTION_BODY']},
      ],
    },
    'demo-annual-review': {
      groupCode:'CORP', masterName:null, masterLayoutVersion:'annual-review-layout-v2-dual-page',
      masterDescription:'Real bank-grade annual review master', variablesFile:'annual-review-variables.json',
      bindingOverlaysFile:'annual-review-binding-overlays.json', manifestFile:'annual-review-catalog-manifest.json',
      testDataFile:'annual-review-demo-test-variables.json',
      templates:[
        {externalId:'DEMO-ANNUAL-REVIEW', name:'Annual Credit Review', description:'Real foreign-bank annual credit review', masterDocx:'demo-annual-review/assets/annual-review-master.docx', anchorFilter:['ANNUAL_REVIEW_BODY','ANNUAL_REVIEW_SCHEDULE']},
        {externalId:'DEMO-FACILITY-RENEWAL', name:'Facility Renewal Letter', description:'Real foreign-bank facility renewal letter', masterDocx:'demo-annual-review/assets/facility-renewal-master.docx', anchorFilter:['FACILITY_RENEWAL_BODY','FACILITY_RENEWAL_TERMS']},
      ],
    },
    'demo-wealth': {
      groupCode:'WEALTH', masterName:'Meridian Private Wealth Statement Master', masterLayoutVersion:'wealth-layout-v2-global-page',
      masterDescription:'Real bank-grade private wealth statement master', variablesFile:'wealth-variables.json',
      bindingOverlaysFile:'wealth-binding-overlays.json', manifestFile:'wealth-catalog-manifest.json',
      testDataFile:'wealth-demo-test-variables.json',
      templates:[{externalId:'DEMO-WEALTH-STATEMENT', name:'Private Wealth Investment Statement', description:'Real foreign-bank private wealth investment statement', masterDocx:'demo-wealth/assets/wealth-statement-master.docx'}],
    },
  };
  for (const [demoDir, cfg] of Object.entries(configs)) {
    try { await importDemo(demoDir, cfg, tokens); }
    catch(e) { console.log('DEMO IMPORT FAILED', demoDir, e.message); }
  }
  console.log('\n=== ALL DEMOS IMPORTED ===');
})().catch(e => { console.error('FATAL:', e); process.exit(1); });
