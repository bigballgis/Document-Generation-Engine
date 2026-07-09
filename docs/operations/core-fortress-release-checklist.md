# CORE-FORTRESS Release Evidence Checklist

Use with `scripts/core-fortress-evidence-bundle.ps1` or `scripts/release-gate.ps1 -EvidenceBundle`.

## Automated gates

- [ ] Backend `mvn verify` green (`backend-verify.log`)
- [ ] Frontend lint / type-check / test / build green (`frontend-gates.log`, when run)

## Runtime snapshots (when Docker stack at `localhost:8080`)

- [ ] `/healthz` returns `{"status":"UP"}` (`healthz.txt`)
- [ ] `/readyz` returns HTTP 200 with `checks.postgres.status=UP` (`readyz.json`)
- [ ] `/actuator/prometheus` contains `docgen_generation_duration` or `docgen.generation` series (`prometheus-sample.txt`)

## SLO series present

- [ ] `docgen.generation.duration` (or `_seconds` Prometheus export)
- [ ] `docgen.pdf.conversion.duration`
- [ ] `docgen.pdf.conversion.outcome`

## Operational readiness

- [ ] DR runbook § Disaster Recovery reviewed (`docs/operations/runbook.md`)
- [ ] Prometheus alert rules reviewed (`deploy/observability/prometheus-alerts.yaml` — `draft: true` until LR-D5 confirmed)
- [ ] Optional: Docker smoke (`curl` health + one sync generation)

## Sign-off

| Field | Value |
| --- | --- |
| Git SHA | _(from `summary.json`)_ |
| Timestamp (UTC) | _(from `summary.json`)_ |
| Gate version | `core-fortress-evidence-v1` |
| Engineer | |
| Status | PASS / FAIL |
