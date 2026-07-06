const { execFileSync } = require('child_process');
const fs = require('fs');
// Login first
const loginScript = `$ErrorActionPreference='Stop';
$r = Invoke-RestMethod -Method POST -Uri 'http://127.0.0.1:8080/api/management/v1/auth/login' -ContentType 'application/json' -Body '{\"username\":\"10000002\",\"password\":\"ChangeMe123!\"}';
Write-Output $r.result.accessToken`;
const tok = execFileSync('pwsh',['-NoProfile','-Command',loginScript],{encoding:'utf8'}).trim();
console.log('tok len', tok.length);
const upScript = `$ErrorActionPreference='Stop'; $ProgressPreference='SilentlyContinue';
$h=@{Authorization='Bearer ${tok}'};
$form = @{ groupCode='RETAIL'; name='Test Mortgage Master'; description='test'; file=(Get-Item -LiteralPath 'deploy/demo-mortgage/assets/mortgage-approval-master.docx') };
try { Invoke-RestMethod -Method POST -Uri 'http://127.0.0.1:8080/api/management/v1/masters' -Headers $h -Form $form | ConvertTo-Json -Depth 50 }
catch { Write-Output ('ERR: ' + $_.Exception.Message); if ($_.ErrorDetails) { Write-Output $_.ErrorDetails.Message } }`;
const out = execFileSync('pwsh',['-NoProfile','-Command',upScript],{encoding:'utf8', maxBuffer: 20*1024*1024});
console.log('upload result:', out.slice(0,400));
