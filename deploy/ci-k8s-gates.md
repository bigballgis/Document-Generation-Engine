# CI K8s Manifest Gates (P15-T09 / ADR-0030)

Blocking CI validation for the `deploy/helm/docgen` chart. Runs on **pull requests** and **pushes to `main`** when deployment artifacts or gate scripts change. Any failure **blocks merge** per ADR-0030 (block merge on any failed quality gate).

## Workflow

| Item | Value |
| --- | --- |
| Workflow file | [`.github/workflows/k8s-manifest-gates.yml`](../.github/workflows/k8s-manifest-gates.yml) |
| CI entry script | [`scripts/ci-k8s-manifest-gates.ps1`](../scripts/ci-k8s-manifest-gates.ps1) |
| Validation engine | [`scripts/helm-validate.ps1`](../scripts/helm-validate.ps1) |

## Path filters (when CI runs)

The job triggers only when a change touches:

- `deploy/**` — Helm chart, values, deployment docs under deploy
- `scripts/helm-validate.ps1` — lint/template/kubeconform logic
- `scripts/ci-k8s-manifest-gates.ps1` — CI entry point

Changes outside these paths do **not** run this workflow (other quality gates may still apply elsewhere).

## What the gate checks (blocking)

1. **P15-T09a — `helm lint`** on `deploy/helm/docgen`
2. **P15-T09a — `helm template`** for profiles: `default`, `dev`, `staging`, `prod`
3. **Custom assertions** — ConfigMap/Secret wiring, Ingress/TLS, HPA, NetworkPolicy, health probes, blue-green (prod), fail-closed secret reference
4. **P15-T09b — `kubeconform`** on each rendered manifest (Kubernetes 1.29.0 schema validation; skips `Certificate`, `ExternalSecret`)

Exit code **non-zero** fails the job and blocks merge on protected branches that require this check.

## Tooling on CI runners

GitHub Actions `ubuntu-latest` runners provide **Docker**. The validation scripts use Docker fallbacks when native `helm` or `kubeconform` binaries are absent:

| Tool | Native | Docker fallback |
| --- | --- | --- |
| Helm 3.14+ | `helm` on PATH | `alpine/helm:3.14.4` |
| kubeconform | `kubeconform` on PATH | `yannh/kubeconform:v0.6.7` |

No Kubernetes cluster is required.

## Local reproduction

From repository root (PowerShell 5.1+):

```powershell
# Full blocking gate (same as CI — includes kubeconform)
.\scripts\ci-k8s-manifest-gates.ps1
```

Equivalent direct invocation:

```powershell
.\scripts\helm-validate.ps1
```

### Offline escape hatch (`-SkipKubeconform`)

When Docker/registry access is unavailable and you only need lint + template + custom assertions:

```powershell
.\scripts\ci-k8s-manifest-gates.ps1 -SkipKubeconform
# or
.\scripts\helm-validate.ps1 -SkipKubeconform
```

**CI does not use this flag.** It is for local/offline development only. Do not merge chart changes without a green full gate (locally or in CI).

## Branch protection (repository admin)

To enforce blocking behavior:

1. Require status check **"Helm lint/template + kubeconform"** (job name from the workflow).
2. Require branches to be up to date before merging.
3. Apply to `main` (and release branches if used).

Until branch protection is configured, the workflow still runs and reports failure on PRs, but merge blocking depends on repository settings.

## Related

- [deploy/README.md](./README.md) — install/upgrade overview
- [ADR-0030](../docs/adr/operations/0030-operational-platform-baseline.md) — CI trigger + block-on-failure policy
- [P15 plan](../docs/plan/detail/P15-kubernetes-deployment-container-hardening.md) — P15-T09 acceptance
