# Blue-Green Release Runbook (P15-T08 / ADR-0030)

Production releases use **blue-green cutover** with **manual approval** before traffic switch
and **manual rollback** on regression. Non-production environments may cut over without the
production approval gate.

## Prerequisites

- Helm chart `deploy/helm/docgen` installed in the target namespace.
- `blueGreen.enabled: true` in environment values (enabled in `values-prod.yaml`).
- New container images pushed to the registry.
- Change ticket / release record for production (approval artifact).

## Color model

| Label | Meaning |
| --- | --- |
| `docgen.io/deployment-color: blue` | Blue Deployment replicas |
| `docgen.io/deployment-color: green` | Green Deployment replicas |
| Active `Service` selector | Routes Ingress traffic to `blueGreen.activeColor` |
| `*-preview` Services | Route to the **inactive** color for smoke validation |

Two Deployments per workload (`backend`, `frontend`) run in parallel. Only the active color
receives production traffic via the main Service.

## Deploy new version (no traffic switch)

1. Determine **inactive color**: if `activeColor` is `blue`, deploy to `green` (and vice versa).
2. Set the inactive color image tag and upgrade **without** changing `activeColor`:

```powershell
$inactive = if ($activeColor -eq "blue") { "green" } else { "blue" }
helm upgrade docgen ./deploy/helm/docgen `
  -f deploy/helm/docgen/values-prod.yaml `
  --namespace docgen `
  --set "blueGreen.colors.${inactive}.backendImageTag=<new-backend-tag>" `
  --set "blueGreen.colors.${inactive}.frontendImageTag=<new-frontend-tag>"
```

3. Wait until inactive color pods are **Ready** (`kubectl get pods -l docgen.io/deployment-color=$inactive`).
4. Smoke-test via preview Services (cluster-internal):

```powershell
kubectl run curl-smoke --rm -it --restart=Never --image=curlimages/curl -- `
  curl -sf "http://docgen-frontend-preview:8080/healthz"
kubectl run curl-smoke --rm -it --restart=Never --image=curlimages/curl -- `
  curl -sf "http://docgen-backend-preview:8080/readyz"
```

5. Record smoke results in the release ticket.

## Production cutover (manual approval required)

**Gate (P15-T08b):** Production cutover is **never automated**. The chart exposes
`blueGreen.requireManualApproval: true` in `values-prod.yaml` as the contract flag — CI/CD
pipelines and release tooling **must** enforce a human approval step before any `helm upgrade`
that changes `blueGreen.activeColor`. The chart itself does not perform cutover; only the
operator (or an approved pipeline step after sign-off) may flip `activeColor`.

| Control | Location | Purpose |
| --- | --- | --- |
| `blueGreen.requireManualApproval` | `values-prod.yaml` | Documents prod gate; pipelines should fail closed if approval artifact missing |
| `blueGreen.activeColor` | `values-prod.yaml` / `--set` | Traffic selector on main Services + HPA `scaleTargetRef` |
| Change ticket | Release process | Human approval artifact (approver ID, timestamp) |

Non-production profiles (`values-dev.yaml`, `values-staging.yaml`) leave `blueGreen.enabled: false`
and may upgrade image tags without the production approval gate.

1. Confirm approver + ticket ID in the release log when `requireManualApproval` is `true`.
2. Switch traffic by flipping `activeColor`:

```powershell
helm upgrade docgen ./deploy/helm/docgen `
  -f deploy/helm/docgen/values-prod.yaml `
  --namespace docgen `
  --set blueGreen.activeColor=<inactive-color-from-previous-step>
```

3. Verify active Service endpoints and Ingress health.
4. Monitor error rates / SLO dashboards for the observation window (minimum 15 minutes).

## Manual rollback (P15-T08c)

If regression is detected **after cutover**, rollback is a **controlled manual operation**
(ADR-0030). Do **not** delete Deployments, PersistentVolumeClaims, or database volumes —
Flyway migrations are forward-only; reverting traffic does not roll back schema.

1. **Do not delete** the previous color — it should still be running with the last-known-good image.
2. Revert `activeColor` to the previous stable color:

```powershell
helm upgrade docgen ./deploy/helm/docgen `
  -f deploy/helm/docgen/values-prod.yaml `
  --namespace docgen `
  --set blueGreen.activeColor=<previous-stable-color>
```

3. Verify traffic restored — confirm main Service selectors and endpoints:

```powershell
kubectl get svc -n docgen -l app.kubernetes.io/part-of=docgen -o wide
kubectl get endpoints docgen-backend docgen-frontend -n docgen
```

4. Probe verification (active color pods must pass before closing incident):

```powershell
# Via Ingress (external)
curl -sf "https://docgen.prod.example.com/healthz"
curl -sf "https://docgen.prod.example.com/api/healthz"

# In-cluster readiness (backend dependency check)
kubectl run curl-ready --rm -it --restart=Never --image=curlimages/curl -n docgen -- `
  curl -sf "http://docgen-backend:8080/readyz"
kubectl run curl-ready --rm -it --restart=Never --image=curlimages/curl -n docgen -- `
  curl -sf "http://docgen-frontend:8080/readyz"
```

5. Confirm HPA `scaleTargetRef` now targets the reverted active color Deployment
   (`kubectl describe hpa -n docgen`).
6. Open a post-incident note; leave the failed color running for forensics until explicitly scaled down.

## Dry-run validation (local)

```powershell
.\scripts\helm-validate.ps1
```

Prod profile asserts blue-green labels, preview Services, and `Assert-T08BlueGreen` checks; kubeconform validates rendered manifests.

## Related

- [ADR-0030](../docs/adr/operations/0030-operational-platform-baseline.md) — CD release + approval + rollback decisions
- [deploy/README.md](./README.md) — install/upgrade prerequisites
