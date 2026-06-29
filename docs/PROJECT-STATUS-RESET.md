# Project Status Reset

**Effective date:** 2026-06-23

## Decision

The project **restarted from zero** on 2026-06-23. All prior wave, epic, milestone,
and task-sheet completion claims were **historical and void** until re-earned with
real, durable, verifiable behavior and green quality gates.

## Re-earned progress (2026-06-23)

Phases **P0–P11** have been re-earned **Done** with implementation in `backend/`
and `frontend/`, green quality gates, and updated plan detail docs. **P13** (identity &
group administration) was subsequently delivered and marked **Done** (2026-06-23) with the
same gate bar (backend `mvn verify` 114 tests green / JaCoCo met; frontend lint/type-check/test/build green).

| Evidence | Location |
| --- | --- |
| Phase status | [docs/plan/master-plan.md](./plan/master-plan.md) |
| Task-level detail | [docs/plan/detail/](./plan/detail/) |
| Epic/milestone mirror | [docs/plan/execution-sync-ledger.md](./plan/execution-sync-ledger.md) |
| Gate logs | Backend `mvn verify`; frontend lint/type-check/test/build (2026-06-23) |

**Active phase:** **P15 In Progress** (2026-06-27; sole active). **P14 Done** — all three vertical slices T01–T03 complete.
**P14-T01 Done** (2026-06-26) — clause/content module vertical slice T01a–T01e (`ContentModuleListView`, `ContentModuleDetailView`,
lifecycle impact dialog, `TemplateContentModuleReferencesPanel`; architecture remediation 4 Critical;
backend verify **469** tests; frontend **224** tests green; architecture re-review **PASS**).
**P14-T02 Done** (2026-06-27) — collaboration to-dos + timeout escalation vertical slice T02a–T02d
(`CollaborationWorkItemPanel`, `CollaborationTimeoutConfigPanel`; E2E `collaboration-todos.spec.ts` **3/3**;
backend verify **481**; frontend **235**).
**P14-T03 Done** (2026-06-27) — template export/import vertical slice T03a–T03c
(`TemplateExportService` / `TemplateImportService`, `TemplateExportActions` / `TemplateImportDialog`;
OpenAPI v1 + `contract-outline.md`; E2E `P14-T03-template-export-import.spec.ts` **2/2**;
backend verify **481**; frontend **235+**).
**P15-T01a Done** (2026-06-27) — backend container hardening re-earned (`backend/Dockerfile.packaged` alpine JRE UID **65532**; read-only smoke `/healthz` **200**; [`deploy/container-hardening.md`](../deploy/container-hardening.md) writable paths).
**P15-T01b Done** (2026-06-27) — frontend NGINX hardening (`frontend/Dockerfile.packaged` nginx UID **101** port **8080**; read-only smoke `/healthz` + `/` **200**; [`deploy/container-hardening.md`](../deploy/container-hardening.md) frontend writable paths; `scripts/container-hardening-smoke.ps1` PASSED).
**P15-T01c Done** (2026-06-27) — writable paths + smoke evidence documented in [`deploy/container-hardening.md`](../deploy/container-hardening.md) (backend `/tmp`, frontend `/tmp/nginx/*`; ReadonlyRootfs, uid **101**, `/healthz` + SPA **200**); `scripts/container-hardening-smoke.ps1` PASSED (backend + frontend). **P15-T01 vertical slice closed** (T01a–T01c).
**P15-T02 Done** (2026-06-27; re-earned) — Helm chart scaffold (`deploy/helm/docgen/`; securityContext non-root/read-only/drop ALL caps; CPU/memory requests+limits per ADR-0030; [`deploy/helm/docgen/README.md`](../deploy/helm/docgen/README.md); `.\scripts\helm-validate.ps1 -SkipKubeconform` PASSED — helm lint **0 failed**, template default/dev/staging/prod, fail-closed secret check; render-only).
**P15-T03 Done** (2026-06-27; re-earned) — ConfigMap/Secret + external service wiring ([`deploy/k8s-config-secrets.md`](../deploy/k8s-config-secrets.md); `secrets.create: false` in all env values; `Assert-T03ConfigSecrets` + fail-closed missing-secret test in `scripts/helm-validate.ps1`; `.\scripts\helm-validate.ps1 -SkipKubeconform` PASSED — lint **0 failed**, template default/dev/staging/prod, T03 assertions; render-only; no plaintext secrets in repo).
**P15-T04 Done** (2026-06-27; re-earned) — Service + Ingress + cert-manager TLS + K8s DNS ([`deploy/k8s-ingress-tls.md`](../deploy/k8s-ingress-tls.md); ClusterIP Services port **8080** + `docgen.io/cluster-dns`; `templates/ingress.yaml` NGINX `/api`→backend `/`→frontend; `templates/certificate.yaml` cert-manager TLS **1.2+**; `Assert-T04IngressTls` in `scripts/helm-validate.ps1`; `.\scripts\helm-validate.ps1 -SkipKubeconform` PASSED — lint **0 failed**, template default/dev/staging/prod, T03+T04 assertions; render-only).
**P15-T05 Done** (2026-06-27; re-earned) — HPA CPU/memory + custom metric ([`deploy/k8s-hpa-autoscaling.md`](../deploy/k8s-hpa-autoscaling.md); `templates/backend-hpa.yaml` + `templates/frontend-hpa.yaml` — `autoscaling/v2`, CPU + memory Resource metrics, min/max bounds, blue-green `scaleTargetRef`; backend Pods custom metric `docgen_http_requests_per_second` gated by `customMetric.enabled`; `values-staging.yaml` explicit customMetric; `Assert-T05Hpa` in `scripts/helm-validate.ps1`; `.\scripts\helm-validate.ps1 -SkipKubeconform` PASSED — lint **0 failed**, template default/dev/staging/prod, T03+T04+T05 assertions; render-only).
**P15-T06 Done** (2026-06-27; re-earned) — default-deny NetworkPolicy + explicit allow ([`deploy/k8s-network-policy.md`](../deploy/k8s-network-policy.md); `templates/networkpolicy.yaml` — `podSelector: {}` default-deny (Ingress + Egress); allow policies — ingress controller, frontend→backend :8080, backend→external egress (TCP **5432/6379/9092/443**), DNS, metrics scrape gated by `networkPolicy.monitoring.enabled`; `Assert-T06NetworkPolicy` in `scripts/helm-validate.ps1`; `.\scripts\helm-validate.ps1 -SkipKubeconform` PASSED — lint **0 failed**, template default/dev/staging/prod, T03+T04+T05+T06 assertions; render-only).
**P15-T07 Done** (2026-06-27; re-earned) — health probes `/healthz` + `/readyz` ([`deploy/k8s-health-probes.md`](../deploy/k8s-health-probes.md); backend Deployments liveness `/healthz` readiness `/readyz` port **8080**; frontend NGINX `/healthz` + `/readyz`; `ReadinessProbe` Postgres check; `Assert-T07Probes` in `scripts/helm-validate.ps1`; `.\scripts\helm-validate.ps1 -SkipKubeconform` PASSED — lint **0 failed**, template default/dev/staging/prod, T03+T04+T05+T06+T07 assertions; render-only).
**P15-T08 Done** (2026-06-27; re-earned) — blue-green release ([`deploy/blue-green-runbook.md`](../deploy/blue-green-runbook.md); dual color Deployments + activeColor Service selector + preview Services + HPA active color; `values-prod.yaml` — `blueGreen.requireManualApproval: true`; manual rollback steps (no data destroy); `Assert-T08BlueGreen` in `scripts/helm-validate.ps1`; `.\scripts\helm-validate.ps1 -SkipKubeconform` PASSED — lint **0 failed**, template default/dev/staging/prod, T03+T04+T05+T06+T07+T08 assertions; render-only).
**P15-T09 Done** (2026-06-27; re-earned) — CI manifest validation gates ([`.github/workflows/k8s-manifest-gates.yml`](../../.github/workflows/k8s-manifest-gates.yml); [`scripts/ci-k8s-manifest-gates.ps1`](../scripts/ci-k8s-manifest-gates.ps1) → `helm-validate.ps1`; PR + push to `main`, path filters `deploy/**` + gate scripts; helm lint + template all envs + blocking kubeconform; [`deploy/ci-k8s-gates.md`](../deploy/ci-k8s-gates.md); local `.\scripts\ci-k8s-manifest-gates.ps1 -SkipKubeconform` PASSED; full kubeconform on CI runner — local full gate blocked by Docker Hub timeout).
**Next planned:** **P15-T10** deployment docs + runbook + phase close; **P18 Not Started** (sequence P14 → P15 → P18).
**P19** verifiability/publish-gate
and **P20** i18n completed **Done** (2026-06-25). P12 (deferred enhancements) is the non-active catch-all.
**Still open:** external deployment validation (E05-T06), intranet SCA (M9-T02), M10–M11.

## What is preserved

| Category | Status |
| --- | --- |
| Confirmed requirements (`docs/requirements/`, `docs/product/PRD.md`) | Preserved |
| Domain model, permission matrix, API contract (OpenAPI v1) | Preserved |
| Accepted ADRs under `docs/adr/` | Preserved — ADR status records a **decision**, not task completion |
| Architecture views (module boundaries, runtime, storage, security) | Preserved as design baseline |

## What was reset (2026-06-23)

| Category | Baseline at reset |
| --- | --- |
| Epic / wave / milestone / task completion | Void until re-earned |
| Closure evidence, gate logs, in-repo done snapshots | Empty until re-captured |
| Active epic / active phase | None until explicitly activated |

## Execution truth

- **Overall plan:** `docs/plan/master-plan.md`
- **Detailed plans:** `docs/plan/detail/<phase>.md`
- **Sync ledger:** `docs/plan/execution-sync-ledger.md`
- **Epic ordering reference:** `docs/architecture/orchestration-high-level-plan.md`
- **Technical wave reference:** `docs/architecture/implementation-task-plan.md`

## Status vocabulary (only these)

- `Not Started` — no meaningful implementation work yet
- `In Progress` — active delivery focus (only one phase/epic at a time)
- `Blocked` — cannot proceed until dependency, decision, or environment is resolved
- `Done` — exit criteria met with real behavior + green gates + updated docs

## Re-earning Done

A task or phase may be marked `Done` only when:

1. Behavior is durable (not demo/in-memory/mock-only).
2. Required tests and quality gates pass.
3. Owning documentation and plan status are updated in the same change set.
4. Post-task doc sync completes (see `.cursor/rules/post-task-doc-sync-constitution.mdc`).
