# Regenerates M1-M14, E01-E12 task sheets and related evidence/plan files (zero baseline).
$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Leaf
$base = if (Test-Path "D:\working\Document Generation System\docs\architecture") {
    "D:\working\Document Generation System\docs\architecture"
} else {
    Join-Path $PSScriptRoot "..\docs\architecture" | Resolve-Path
}

$banner = @'
> **Zero baseline (2026-06-23):** Regenerated for project restart. No implementation
> code exists. All tasks are `Not Started`. Prior closure evidence is void per
> [PROJECT-STATUS-RESET.md](../PROJECT-STATUS-RESET.md).
'@

function New-TaskSheet {
    param(
        [string]$FileName,
        [string]$Title,
        [string]$Purpose,
        [string]$WaveOrEpic,
        [string]$Priority,
        [string[]]$DependsOn,
        [string]$ExitCriteria,
        [array]$Tasks  # @( @{ Id=''; Scope=''; Acceptance='' } )
    )
    $depLines = ($DependsOn | ForEach-Object { "- $_" }) -join "`n"
    $taskRows = ($Tasks | ForEach-Object {
        "| $($_.Id) | $($_.Priority) | $($_.Module) | $($_.Scope) | $($_.Acceptance) | Not Started |"
    }) -join "`n"

    $content = @"
---
id: DOC-ARCH-$($FileName.ToUpper().Replace('-','-'))
type: Architecture View
status: Accepted
sourceOfTruth: false
owners:
  - architecture
  - implementation
dependsOn:
  - docs/plan/master-plan.md
  - docs/PROJECT-STATUS-RESET.md
  - docs/architecture/implementation-task-plan.md
---

# $Title

$banner

## Purpose

$Purpose

## Baseline

- **Scope:** $WaveOrEpic
- **Priority:** $Priority
- **Exit criteria:** $ExitCriteria

## Dependencies

$depLines

## Task board

| Task ID | Priority | Module | Scope | Acceptance criteria | Status |
| --- | --- | --- | --- | --- | --- |
$taskRows

## Gate commands (when implementation exists)

- Backend: ``mvn -B -ntp -f backend/pom.xml verify``
- Frontend: ``pnpm -C frontend lint`` / ``type-check`` / ``test`` / ``build``

## Evidence

| Evidence slot | Status |
| --- | --- |
| Unit / integration tests | Not Started |
| Contract / OpenAPI conformance | Not Started |
| Quality gate logs | Not Started |
| Plan status sync | Not Started |
"@
    Set-Content -Path (Join-Path $base $FileName) -Value $content -Encoding UTF8
}

# --- Milestones M1-M14 ---
$milestones = @{
    'm1-task-sheet.md' = @{
        Title = 'M1 Task Sheet (Wave 0-1: Foundation & Contract Discovery)'
        Purpose = 'Establish implementation skeleton, shared conventions, and deliver read-only contract visibility endpoints.'
        Wave = 'Wave 0 + Wave 1'
        Priority = 'P0'
        Depends = @('P0 in master plan', 'OpenAPI v1 contract + examples')
        Exit = 'Module map documented; contract + versions endpoints match OpenAPI with auth/audit tests.'
        Tasks = @(
            @{ Id='M1-T01'; Priority='P0'; Module='Platform'; Scope='Backend/frontend repo skeleton, compose file, health endpoints'; Acceptance='Apps boot; README documents local setup' }
            @{ Id='M1-T02'; Priority='P0'; Module='Shared kernel'; Scope='Unified error envelope + metadata mapping'; Acceptance='Matches OpenAPI error schema in tests' }
            @{ Id='M1-T03'; Priority='P0'; Module='Authorization'; Scope='Credential header validation skeleton, fail-closed'; Acceptance='401/403 paths tested' }
            @{ Id='M1-T04'; Priority='P0'; Module='Contract publication'; Scope='GET contract + GET versions read models'; Acceptance='Schema-valid responses; group-scoped filtering' }
            @{ Id='M1-T05'; Priority='P0'; Module='Audit'; Scope='Security audit summary on contract access'; Acceptance='Non-sensitive audit fields only' }
            @{ Id='M1-T06'; Priority='P1'; Module='API adapters'; Scope='OpenAPI contract validation test suite'; Acceptance='CI fails on schema drift' }
        )
    }
    'm2-task-sheet.md' = @{
        Title = 'M2 Task Sheet (Wave 2: Synchronous Generation)'
        Purpose = 'Deliver sync generation for explicit version and default route paths including idempotency and output delivery modes.'
        Wave = 'Wave 2'
        Priority = 'P0'
        Depends = @('M1 Done', 'Published template exists (E02/E01)')
        Exit = 'Sync DOCX/PDF generation with idempotency replay and download URL mode passes integration tests.'
        Tasks = @(
            @{ Id='M2-T01'; Priority='P0'; Module='Generation orchestration'; Scope='Request validation (variables, output, encryption)'; Acceptance='400/422 field errors per OpenAPI' }
            @{ Id='M2-T02'; Priority='P0'; Module='Generation orchestration'; Scope='Idempotency key handling + replay/conflict'; Acceptance='metadata.idempotencyStatus tested' }
            @{ Id='M2-T03'; Priority='P0'; Module='Rendering worker'; Scope='Sync DOCX render job execution'; Acceptance='Binary stream response with required headers' }
            @{ Id='M2-T04'; Priority='P0'; Module='Rendering worker'; Scope='PDF output + fidelity warnings in JSON path'; Acceptance='result.fidelityWarnings[] populated when warnings exist' }
            @{ Id='M2-T05'; Priority='P0'; Module='API adapters'; Scope='SYNC_DOWNLOAD_URL delivery + 15min expiry'; Acceptance='download.url + download.expiresAt in response' }
            @{ Id='M2-T06'; Priority='P1'; Module='Encryption'; Scope='Dynamic encryption parameter validation + execution'; Acceptance='ENCRYPTION_* errors; no password in logs' }
        )
    }
    'm3-task-sheet.md' = @{
        Title = 'M3 Task Sheet (Wave 3: Batch & Async Lifecycle)'
        Purpose = 'Batch generation and async task query/cancel with state model and partial-success semantics.'
        Wave = 'Wave 3'
        Priority = 'P0'
        Depends = @('M2 Done', 'Kafka async adapter (E05)')
        Exit = 'Batch and async paths pass contract and state-transition tests.'
        Tasks = @(
            @{ Id='M3-T01'; Priority='P0'; Module='Generation orchestration'; Scope='Batch item validation + itemId uniqueness'; Acceptance='400 ITEM_ID_DUPLICATED on duplicates' }
            @{ Id='M3-T02'; Priority='P0'; Module='Generation orchestration'; Scope='Sync batch all-or-nothing failure'; Acceptance='No files on partial validation failure' }
            @{ Id='M3-T03'; Priority='P0'; Module='Generation orchestration'; Scope='Async batch accept + PARTIAL_SUCCEEDED'; Acceptance='202 accept; 200 with per-item status' }
            @{ Id='M3-T04'; Priority='P0'; Module='Generation orchestration'; Scope='Task query + full replay on idempotency hit'; Acceptance='getAsyncTask returns complete state object' }
            @{ Id='M3-T05'; Priority='P0'; Module='Generation orchestration'; Scope='Task cancel rules + CANCELLED terminal state'; Acceptance='409 when cancellation not allowed' }
        )
    }
    'm4-task-sheet.md' = @{
        Title = 'M4 Task Sheet (Wave 4: Download Security)'
        Purpose = 'Secure document download with secondary authorization, expiry, and audit.'
        Wave = 'Wave 4'
        Priority = 'P0'
        Depends = @('M2 or M3 Done')
        Exit = 'Download endpoint enforces template-level re-auth and expiry; audit complete.'
        Tasks = @(
            @{ Id='M4-T01'; Priority='P0'; Module='API adapters'; Scope='GET download with documentId resolution to template'; Acceptance='403/404/410 cases tested' }
            @{ Id='M4-T02'; Priority='P0'; Module='Authorization'; Scope='Secondary auth: credential + AD Group + template'; Acceptance='Fail-closed; no version re-check at download' }
            @{ Id='M4-T03'; Priority='P0'; Module='Artifact storage'; Scope='Presigned/stream download from MinIO'; Acceptance='Multiple downloads within 15min window' }
            @{ Id='M4-T04'; Priority='P1'; Module='Audit'; Scope='Download audit summary'; Acceptance='No full URL in audit/log UI' }
        )
    }
    'm5-task-sheet.md' = @{
        Title = 'M5 Task Sheet (Wave 5: API Management)'
        Purpose = 'Template-level API policy management plane with policyVersion and impact preview.'
        Wave = 'Wave 5'
        Priority = 'P0'
        Depends = @('M1 Done', 'E03 alignment')
        Exit = 'Admins manage all policy domains through product flows with audit.'
        Tasks = @(
            @{ Id='M5-T01'; Priority='P0'; Module='API management'; Scope='Policy aggregate + per-domain save'; Acceptance='policyVersion increments; API_POLICY_UPDATED audit' }
            @{ Id='M5-T02'; Priority='P0'; Module='API management'; Scope='Credential lifecycle CRUD + one-time secret'; Acceptance='Fingerprint only after create/rotate' }
            @{ Id='M5-T03'; Priority='P0'; Module='API management'; Scope='AD Group authorization config + cache invalidation'; Acceptance='Immediate effect; 5min cache rule' }
            @{ Id='M5-T04'; Priority='P0'; Module='API management'; Scope='Default route target change + impact preview'; Acceptance='Audit + contract reflects target version' }
            @{ Id='M5-T05'; Priority='P1'; Module='API management'; Scope='Management UI for policy domains'; Acceptance='English i18n; hard block vs warning UX' }
        )
    }
    'm6-task-sheet.md' = @{
        Title = 'M6 Task Sheet (Wave 6: Lifecycle Governance)'
        Purpose = 'Template lifecycle state machine, publish gate, and lifecycle audit chain.'
        Wave = 'Wave 6'
        Priority = 'P0'
        Depends = @('E01 partial', 'M4 preview artifacts')
        Exit = 'Full lifecycle journey in UI/API with publish gate and audit evidence.'
        Tasks = @(
            @{ Id='M6-T01'; Priority='P0'; Module='Template release governance'; Scope='State transitions through publish'; Acceptance='Deterministic guards per role' }
            @{ Id='M6-T02'; Priority='P0'; Module='Template release governance'; Scope='Test/approval opinion forms + evidence confirmation'; Acceptance='Warning summary viewed flag in audit' }
            @{ Id='M6-T03'; Priority='P0'; Module='Template release governance'; Scope='Publish gate checklist aggregation'; Acceptance='Blockers prevent publish' }
            @{ Id='M6-T04'; Priority='P0'; Module='Template release governance'; Scope='Stop/restore/deprecate + impact preview'; Acceptance='Fail-closed; audit trail' }
            @{ Id='M6-T05'; Priority='P1'; Module='Collaboration'; Scope='In-app todos + timeout escalation visibility'; Acceptance='No auto-approve; admin escalation only' }
        )
    }
    'm7-task-sheet.md' = @{
        Title = 'M7 Task Sheet (Wave 7: Runtime E2E Integration)'
        Purpose = 'Close adapter-to-HTTP gap with full request flow integration tests.'
        Wave = 'Wave 7'
        Priority = 'P0'
        Depends = @('M2–M4 Done')
        Exit = 'All nine OpenAPI operations callable end-to-end over HTTP.'
        Tasks = @(
            @{ Id='M7-T01'; Priority='P0'; Module='API adapters'; Scope='Controller wiring for all runtime ops'; Acceptance='RestAssured/contract E2E green' }
            @{ Id='M7-T02'; Priority='P0'; Module='API adapters'; Scope='Negative matrix 401/403/404/409/410/422'; Acceptance='Stable error.code per scenario' }
            @{ Id='M7-T03'; Priority='P0'; Module='Generation orchestration'; Scope='Trace + audit metadata completeness'; Acceptance='traceId/auditId on all responses' }
        )
    }
    'm8-task-sheet.md' = @{
        Title = 'M8 Task Sheet (Wave 8: Security Remediation Gates)'
        Purpose = 'Dependency remediation inventory and enforceable static/security release gates.'
        Wave = 'Wave 8'
        Priority = 'P1'
        Depends = @('M7 Done')
        Exit = 'High/critical findings remediated or exception-tracked; gates executable.'
        Tasks = @(
            @{ Id='M8-T01'; Priority='P0'; Module='Platform'; Scope='Checkstyle + SpotBugs + PMD baseline'; Acceptance='mvn verify includes all three' }
            @{ Id='M8-T02'; Priority='P0'; Module='Platform'; Scope='JaCoCo thresholds enforced'; Acceptance='85%/90% gates in pom' }
            @{ Id='M8-T03'; Priority='P1'; Module='Platform'; Scope='Dependency advisory inventory + disposition'; Acceptance='No untracked high/critical' }
        )
    }
    'm9-task-sheet.md' = @{
        Title = 'M9 Task Sheet (Wave 9: Dependency Scan Recovery)'
        Purpose = 'Restore online/intranet dependency scan execution and frontend audit loop.'
        Wave = 'Wave 9'
        Priority = 'P1'
        Depends = @('M8 Done')
        Exit = 'SBOM + SCA evidence captured; frontend audit executable.'
        Tasks = @(
            @{ Id='M9-T01'; Priority='P0'; Module='Platform'; Scope='Backend SBOM generation (CycloneDX)'; Acceptance='Artifact archived per run' }
            @{ Id='M9-T02'; Priority='P0'; Module='Platform'; Scope='Frontend SBOM + intranet SCA'; Acceptance='See m9-t02-closure-plan.md' }
            @{ Id='M9-T03'; Priority='P1'; Module='Platform'; Scope='Renew/close security exceptions with metadata'; Acceptance='Owner + expiry on any residual' }
        )
    }
    'm10-task-sheet.md' = @{
        Title = 'M10 Task Sheet (Wave 10: Deferred Security Closure)'
        Purpose = 'Execute deferred security scans in approved network path; exit temporary exceptions.'
        Wave = 'Wave 10'
        Priority = 'P2'
        Depends = @('M9 Done')
        Exit = 'All deferred exceptions closed or renewed with approval.'
        Tasks = @(
            @{ Id='M10-T01'; Priority='P0'; Module='Platform'; Scope='Run approved OWASP/SCA path'; Acceptance='Machine-readable report archived' }
            @{ Id='M10-T02'; Priority='P0'; Module='Platform'; Scope='Disposition remaining findings'; Acceptance='Remediate or time-bound exception' }
        )
    }
    'm11-task-sheet.md' = @{
        Title = 'M11 Task Sheet (Wave 11: Intranet Security Baseline)'
        Purpose = 'Permanent intranet-executable security baseline without public internet dependency.'
        Wave = 'Wave 11'
        Priority = 'P2'
        Depends = @('M10 Done')
        Exit = 'Continuous intranet gate cadence documented and passing.'
        Tasks = @(
            @{ Id='M11-T01'; Priority='P0'; Module='Platform'; Scope='Lock intranet SBOM + SCA workflow'; Acceptance='Reproducible in corp network' }
            @{ Id='M11-T02'; Priority='P0'; Module='Platform'; Scope='Close M10 renewed exceptions'; Acceptance='Approval trace + timestamp' }
        )
    }
    'm12-task-sheet.md' = @{
        Title = 'M12 Task Sheet (Wave 12: Runtime Endpoint Adapters)'
        Purpose = 'Complete adapter-level orchestration before HTTP transport wiring.'
        Wave = 'Wave 12'
        Priority = 'P0'
        Depends = @('M7 partial')
        Exit = 'Adapter tests cover positive + negative paths for all runtime ops.'
        Tasks = @(
            @{ Id='M12-T01'; Priority='P0'; Module='API adapters'; Scope='Discovery adapter flows'; Acceptance='Adapter-level tests green' }
            @{ Id='M12-T02'; Priority='P0'; Module='API adapters'; Scope='Generate adapter flows (sync/async accept)'; Acceptance='Idempotency-safe adapter tests' }
            @{ Id='M12-T03'; Priority='P0'; Module='API adapters'; Scope='Task + download adapter flows'; Acceptance='Secondary auth at adapter boundary' }
        )
    }
    'm13-task-sheet.md' = @{
        Title = 'M13 Task Sheet (Wave 13: Runtime HTTP Transport)'
        Purpose = 'Wire adapters to HTTP controllers with OpenAPI-conformant transport mapping.'
        Wave = 'Wave 13'
        Priority = 'P0'
        Depends = @('M12 Done')
        Exit = 'HTTP integration tests pass for discovery, generate, task, download.'
        Tasks = @(
            @{ Id='M13-T01'; Priority='P0'; Module='API adapters'; Scope='HTTP controllers + header/path mapping'; Acceptance='Status codes match OpenAPI' }
            @{ Id='M13-T02'; Priority='P0'; Module='API adapters'; Scope='Sync stream header metadata'; Acceptance='Required headers on file responses' }
            @{ Id='M13-T03'; Priority='P0'; Module='API adapters'; Scope='Transport negative-path regression suite'; Acceptance='Full matrix automated' }
        )
    }
    'm14-task-sheet.md' = @{
        Title = 'M14 Task Sheet (Wave 14: Batch Transport & Async Acceptance)'
        Purpose = 'HTTP parity for batch endpoints and 202 async-accepted projection.'
        Wave = 'Wave 14'
        Priority = 'P0'
        Depends = @('M13 Done', 'M3 Done')
        Exit = 'All nine OpenAPI ops have transport-level parity including batch.'
        Tasks = @(
            @{ Id='M14-T01'; Priority='P0'; Module='API adapters'; Scope='Batch generate HTTP handlers'; Acceptance='Ordered items[] in response' }
            @{ Id='M14-T02'; Priority='P0'; Module='API adapters'; Scope='202 async-accepted projection model'; Acceptance='task.queryPath in accept response' }
            @{ Id='M14-T03'; Priority='P0'; Module='API adapters'; Scope='Batch transport integration tests'; Acceptance='Sync fail-all + async partial scenarios' }
        )
    }
}

foreach ($kv in $milestones.GetEnumerator()) {
    $m = $kv.Value
    New-TaskSheet -FileName $kv.Key -Title $m.Title -Purpose $m.Purpose -WaveOrEpic $m.Wave `
        -Priority $m.Priority -DependsOn $m.Depends -ExitCriteria $m.Exit -Tasks $m.Tasks
}

# --- Epics E01-E07 ---
$epics = @{
    'e01-task-sheet.md' = @{
        Title = 'E01 Task Sheet (Master & Template Authoring Core)'
        Purpose = 'Product core: master management, template wizard, structured editing, variables, rules, preview entry.'
        Wave = 'Epic E01 — maps to P2, P3, P4'
        Priority = 'P0'
        Depends = @('P1 login', 'P0 foundation')
        Exit = 'User creates master, builds template, runs test generation with preview.'
        Tasks = @(
            @{ Id='E01-T01'; Priority='P0'; Module='Master document'; Scope='DOCX upload + anchor catalog extraction'; Acceptance='Anchors stable anchorId; group isolated' }
            @{ Id='E01-T02'; Priority='P0'; Module='Master document'; Scope='Master review workflow'; Acceptance='Only APPROVED masters referenceable' }
            @{ Id='E01-T03'; Priority='P0'; Module='Template composition'; Scope='Template wizard steps 1–4'; Acceptance='English i18n; role-scoped' }
            @{ Id='E01-T04'; Priority='P0'; Module='Template composition'; Scope='Variable schema editor + validation'; Acceptance='Publish candidate schema locked' }
            @{ Id='E01-T05'; Priority='P0'; Module='Template composition'; Scope='Structured content nodes (v1 matrix)'; Acceptance='Unsupported nodes block publish' }
            @{ Id='E01-T06'; Priority='P0'; Module='Template composition'; Scope='Condition/loop configurator'; Acceptance='No external API in rules' }
            @{ Id='E01-T07'; Priority='P0'; Module='Rendering worker'; Scope='Test generation + preview record'; Acceptance='Authoritative preview artifacts stored' }
        )
    }
    'e02-task-sheet.md' = @{
        Title = 'E02 Task Sheet (Template Lifecycle Workflow)'
        Purpose = 'End-to-end lifecycle productization with audit evidence.'
        Wave = 'Epic E02 — maps to P5'
        Priority = 'P0'
        Depends = @('E01 partial')
        Exit = 'Lifecycle completable in UI/API with audit.'
        Tasks = @(
            @{ Id='E02-T01'; Priority='P0'; Module='Template release governance'; Scope='Submit test + test decision'; Acceptance='Role guards; state machine correct' }
            @{ Id='E02-T02'; Priority='P0'; Module='Template release governance'; Scope='Submit approval + approval decision'; Acceptance='Evidence summaries required' }
            @{ Id='E02-T03'; Priority='P0'; Module='Template release governance'; Scope='Publish + semver release version'; Acceptance='API contract generated at publish' }
            @{ Id='E02-T04'; Priority='P0'; Module='Template release governance'; Scope='Import/export governance'; Acceptance='Import restarts at draft in prod' }
            @{ Id='E02-T05'; Priority='P1'; Module='Collaboration'; Scope='Todos + timeout escalation'; Acceptance='No proxy approval' }
        )
    }
    'e03-task-sheet.md' = @{
        Title = 'E03 Task Sheet (API Management Completion)'
        Purpose = 'Full API management product flows for admins and caller contract view.'
        Wave = 'Epic E03 — maps to P6'
        Priority = 'P0'
        Depends = @('E02 partial')
        Exit = 'Policy lifecycle operable without backend-only workarounds.'
        Tasks = @(
            @{ Id='E03-T01'; Priority='P0'; Module='API management'; Scope='All five policy domains in UI'; Acceptance='Impact preview before save' }
            @{ Id='E03-T02'; Priority='P0'; Module='API management'; Scope='Credential admin flows'; Acceptance='Secret shown once; expiry reminders' }
            @{ Id='E03-T03'; Priority='P0'; Module='Contract publication'; Scope='Caller-facing contract page'; Acceptance='Non-sensitive; version diff computed in UI' }
        )
    }
    'e04-task-sheet.md' = @{
        Title = 'E04 Task Sheet (Audit & Governance Console)'
        Purpose = 'Audit search, filter, export with role scope and masking.'
        Wave = 'Epic E04 — maps to P8'
        Priority = 'P1'
        Depends = @('E02', 'E03')
        Exit = 'Audit roles use complete console within permission boundaries.'
        Tasks = @(
            @{ Id='E04-T01'; Priority='P0'; Module='Audit'; Scope='Audit query API + scope filters'; Acceptance='GROUP_ADMIN cannot see other groups' }
            @{ Id='E04-T02'; Priority='P0'; Module='Audit'; Scope='Masked export'; Acceptance='No sensitive plaintext in export' }
            @{ Id='E04-T03'; Priority='P0'; Module='Audit'; Scope='Audit console UI'; Acceptance='English i18n; time window filters' }
        )
    }
    'e05-task-sheet.md' = @{
        Title = 'E05 Task Sheet (Enterprise Integration Hardening)'
        Purpose = 'Replace transitional seams with production adapters for persistence, cache, messaging, storage, directory, secrets.'
        Wave = 'Epic E05 — maps to P0, P7, P9'
        Priority = 'P0'
        Depends = @('P0 compose')
        Exit = 'Core paths use real adapters; external env evidence tracked separately.'
        Tasks = @(
            @{ Id='E05-T01'; Priority='P0'; Module='Infrastructure'; Scope='PostgreSQL repositories (no in-memory primary store)'; Acceptance='Flyway migrations; integration tests' }
            @{ Id='E05-T02'; Priority='P0'; Module='Infrastructure'; Scope='Redis cache + idempotency'; Acceptance='Fail-closed when Redis unavailable' }
            @{ Id='E05-T03'; Priority='P0'; Module='Infrastructure'; Scope='MinIO artifact storage'; Acceptance='Upload/download round-trip' }
            @{ Id='E05-T04'; Priority='P0'; Module='Infrastructure'; Scope='Kafka async publisher + consumer'; Acceptance='Retry + DLT configured' }
            @{ Id='E05-T05'; Priority='P0'; Module='Authorization'; Scope='Enterprise AD Group resolver adapter'; Acceptance='503 fail-closed without cache' }
            @{ Id='E05-T06'; Priority='P1'; Module='Platform'; Scope='External validation evidence ledger'; Acceptance='See e05-external-validation-evidence.md' }
        )
    }
    'e06-task-sheet.md' = @{
        Title = 'E06 Task Sheet (Management UI Product Finish)'
        Purpose = 'Login-first OA management shell with dual-brand theming and grouped navigation.'
        Wave = 'Epic E06 — maps to P1, P5, P6, P8 UI'
        Priority = 'P1'
        Depends = @('P1 login')
        Exit = 'UI presents coherent product shell, not a workbench stub.'
        Tasks = @(
            @{ Id='E06-T01'; Priority='P0'; Module='Frontend shell'; Scope='Global header + side nav (4 OA groups)'; Acceptance='REDBC/GREENBC theme switch' }
            @{ Id='E06-T02'; Priority='P0'; Module='Frontend shell'; Scope='Role-aware landing pages'; Acceptance='Forbidden route unified UX' }
            @{ Id='E06-T03'; Priority='P0'; Module='Frontend shell'; Scope='Lifecycle governance surfaces'; Acceptance='End-to-end journey in shell' }
            @{ Id='E06-T04'; Priority='P0'; Module='Frontend shell'; Scope='API governance surfaces'; Acceptance='Wired to backend session auth' }
            @{ Id='E06-T05'; Priority='P1'; Module='Frontend shell'; Scope='Accessibility + responsive baseline'; Acceptance='Vitest/Playwright smoke' }
        )
    }
    'e07-task-sheet.md' = @{
        Title = 'E07 Task Sheet (Production Readiness)'
        Purpose = 'Release gate automation, performance evidence, observability readiness.'
        Wave = 'Epic E07 — maps to P9'
        Priority = 'P2'
        Depends = @('E01–E06 substantive progress')
        Exit = 'Release readiness evidenced via automated gate script output.'
        Tasks = @(
            @{ Id='E07-T01'; Priority='P0'; Module='Platform'; Scope='One-command production readiness gate script'; Acceptance='Artifacts under artifacts/' }
            @{ Id='E07-T02'; Priority='P0'; Module='Platform'; Scope='Structured logging + metrics export'; Acceptance='JSON logs; Micrometer endpoints' }
            @{ Id='E07-T03'; Priority='P1'; Module='Platform'; Scope='Load/smoke benchmarks documented'; Acceptance='Baseline numbers recorded' }
        )
    }
}

foreach ($kv in $epics.GetEnumerator()) {
    $e = $kv.Value
    New-TaskSheet -FileName $kv.Key -Title $e.Title -Purpose $e.Purpose -WaveOrEpic $e.Wave `
        -Priority $e.Priority -DependsOn $e.Depends -ExitCriteria $e.Exit -Tasks $e.Tasks
}

# --- Evidence / continuation files ---
$evidenceFiles = @{
    'e05-external-validation-evidence.md' = @"
# E05 External Validation Evidence

$banner

## Purpose

Track **deployment-time** validation for enterprise dependencies that cannot be
fully proven inside the repository alone.

## Dependency evidence matrix

| Dependency | Required evidence | Owner | Cadence | Status |
| --- | --- | --- | --- | --- |
| PostgreSQL (HA cluster) | Connectivity + migration smoke in target env | TBD | Per release candidate | Not Started |
| Redis cluster | Cache + lock + idempotency smoke | TBD | Per release candidate | Not Started |
| Kafka cluster | Topic ACLs + consumer lag check | TBD | Per release candidate | Not Started |
| MinIO tenancy | Bucket policy + SSE verification | TBD | Per release candidate | Not Started |
| AD / LDAP directory | Group resolution spot-check | TBD | Weekly | Not Started |
| Secrets provider | Secret mount + rotation drill | TBD | Per release candidate | Not Started |

## Pass/fail rule

E05 epic may be marked **Done** in-repo when adapter code and tests are complete.
**Release readiness** additionally requires all rows above marked **Pass** with
linked evidence artifacts.

## Execution log

See [e05-external-evidence-execution-log.md](./e05-external-evidence-execution-log.md).
"@
    'e05-external-evidence-execution-log.md' = @"
# E05 External Evidence Execution Log

$banner

## Usage

Record each validation cycle below. Do not mark **Pass** without attached evidence.

| Cycle ID | Date | Dependency | Environment | Result | Evidence link | Reviewer |
| --- | --- | --- | --- | --- | --- | --- |
| — | — | — | — | Not Started | — | — |

"@
    'e06-role-journey-release-evidence.md' = @"
# E06 Role-Journey Release Evidence

$banner

## Purpose

Evidence slots for E06 management UI role-journey productization. All slots empty
until re-earned.

| Evidence | Description | Status |
| --- | --- | --- |
| E06-EV-01 | Login → role landing → first critical task (3 roles) | Not Started |
| E06-EV-02 | Forbidden route UX + audit trace | Not Started |
| E06-EV-03 | Dual-brand theme + logo switch screenshots/tests | Not Started |
| E06-EV-04 | Frontend quality gate log | Not Started |

"@
    'e11-role-journey-ui-continuation-plan.md' = @"
# E11 Role-Journey UI Continuation Plan

$banner

## Scope

Continue login-first role journeys for `GLOBAL_ADMIN`, `GROUP_ADMIN`, `TEMPLATE_AUTHOR`
after P1 login foundation.

## Route visibility baseline

| routeKey | GLOBAL_ADMIN | GROUP_ADMIN | TEMPLATE_AUTHOR |
| --- | --- | --- | --- |
| route.global-governance-home | Allow | Deny | Deny |
| route.group-governance-home | Allow | Allow | Deny |
| route.template-authoring-home | Allow | Allow | Allow |
| route.api-policy-management | Allow | Scoped | Deny |
| route.audit-console | Allow | Scoped | Deny |

## Tasks

| ID | Scope | Status |
| --- | --- | --- |
| E11-T01 | Map routeKey to Vue Router paths | Not Started |
| E11-T02 | Post-login landing resolver | Not Started |
| E11-T03 | Navigation guard + forbidden page | Not Started |

## Pending questions

- Final URL mapping table (requires user confirmation)
- TEMPLATE_AUTHOR vs 母版设计人员 landing merge rule

"@
    'e12-frontend-role-journey-development-plan.md' = @"
# E12 Frontend Role-Operation Journey Development Plan

$banner

## Purpose

Extend E11 with **operation-level** journeys (not just landing pages): each role
can complete its first critical task inside the product shell.

## Milestones

| Milestone | Goal | Status |
| --- | --- | --- |
| E12-M1 | GLOBAL_ADMIN: global API policy action | Not Started |
| E12-M2 | GROUP_ADMIN: scoped AD Group / default route action | Not Started |
| E12-M3 | TEMPLATE_AUTHOR: create template + submit test | Not Started |

## Phase task sheets

- [Phase 1](./e12-phase1-task-sheet.md) — E12-T01..T05
- [Phase 2](./e12-phase2-task-sheet.md) — E12-T06..T10

"@
    'm9-t02-closure-plan.md' = @"
# M9-T02 Closure Plan (Frontend Dependency Security)

$banner

## Task

Close frontend dependency security baseline: SBOM generation + intranet SCA + remediation loop.

## Steps

| Step | Action | Status |
| --- | --- | --- |
| 1 | Generate frontend CycloneDX SBOM | Not Started |
| 2 | Submit to approved intranet SCA | Not Started |
| 3 | Remediate or exception-track high/critical | Not Started |
| 4 | Re-run pnpm lint/type-check/test/build | Not Started |

"@
}

foreach ($kv in $evidenceFiles.GetEnumerator()) {
    Set-Content -Path (Join-Path $base $kv.Key) -Value $kv.Value -Encoding UTF8
}

# E12 phase sheets
$e12phases = @{
    'e12-phase1-task-sheet.md' = @(
        @{ Id='E12-T01'; Module='Frontend'; Scope='GLOBAL_ADMIN governance home page'; Acceptance='Lands after login; i18n keys' }
        @{ Id='E12-T02'; Module='Frontend'; Scope='GROUP_ADMIN governance home page'; Acceptance='Scoped to authorized groups' }
        @{ Id='E12-T03'; Module='Frontend'; Scope='TEMPLATE_AUTHOR authoring home'; Acceptance='Template list/create entry' }
        @{ Id='E12-T04'; Module='Frontend'; Scope='Shared shell components extraction'; Acceptance='Theme tokens only; no hardcoded brand' }
        @{ Id='E12-T05'; Module='Frontend'; Scope='Playwright smoke: 3 role landings'; Acceptance='CI job defined' }
    )
    'e12-phase2-task-sheet.md' = @(
        @{ Id='E12-T06'; Module='Frontend'; Scope='GLOBAL_ADMIN first API policy task flow'; Acceptance='Completes save with confirmation' }
        @{ Id='E12-T07'; Module='Frontend'; Scope='GROUP_ADMIN scoped policy task flow'; Acceptance='Cannot access other groups' }
        @{ Id='E12-T08'; Module='Frontend'; Scope='TEMPLATE_AUTHOR create + submit test flow'; Acceptance='Reaches TESTING state in UI' }
        @{ Id='E12-T09'; Module='Frontend'; Scope='Forbidden route regression tests'; Acceptance='No data leak in DOM/network' }
        @{ Id='E12-T10'; Module='Frontend'; Scope='Role journey metrics hooks (optional)'; Acceptance='Pending user threshold confirmation' }
    )
}

foreach ($kv in $e12phases.GetEnumerator()) {
    $phaseNum = if ($kv.Key -match 'phase1') { '1' } else { '2' }
    New-TaskSheet -FileName $kv.Key -Title "E12 Phase $phaseNum Task Sheet" `
        -Purpose "E12 frontend role-operation journey — phase $phaseNum tasks." `
        -WaveOrEpic 'Epic E12' -Priority 'P1' -DependsOn @('E11 partial', 'P1 login') `
        -ExitCriteria "Phase $phaseNum role journeys executable in UI with tests." `
        -Tasks ($kv.Value | ForEach-Object { @{ Id=$_.Id; Priority='P0'; Module=$_.Module; Scope=$_.Scope; Acceptance=$_.Acceptance } })
}

Write-Host "Generated task sheets and evidence files under $base"
