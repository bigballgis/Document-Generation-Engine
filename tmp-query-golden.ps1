$ErrorActionPreference = 'Stop'
$login = Invoke-RestMethod -Uri 'http://localhost:8080/api/management/v1/auth/login' -Method POST -ContentType 'application/json' -Body '{"username":"10000001","password":"ChangeMe123!"}'
$token = $login.result.accessToken
$headers = @{ Authorization = "Bearer $token" }
$page = 0
$found = $null
while ($page -lt 20 -and -not $found) {
  $list = Invoke-RestMethod -Uri "http://localhost:8080/api/management/v1/templates?page=$page&size=50" -Headers $headers
  $items = @()
  if ($list.result.content) { $items = $list.result.content }
  elseif ($list.result.items) { $items = $list.result.items }
  elseif ($list.result -is [System.Array]) { $items = $list.result }
  foreach ($t in $items) {
    if ($t.externalId -eq 'CDP-MVP-GOLDEN') {
      $found = $t
      break
    }
  }
  if (-not $list.result -or ($list.result.last -eq $true) -or ($items.Count -eq 0)) { break }
  $page++
}
if (-not $found) {
  Write-Host 'CDP-MVP-GOLDEN not found'
  exit 0
}
Write-Host ("FOUND id={0} status={1} name={2}" -f $found.id, $found.lifecycleStatus, $found.name)
$detail = Invoke-RestMethod -Uri ("http://localhost:8080/api/management/v1/templates/{0}" -f $found.id) -Headers $headers
$detail.result | ConvertTo-Json -Depth 6
