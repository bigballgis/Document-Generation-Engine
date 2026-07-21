# Kubernetes Configuration & Secrets (P15-T03 / ADR-0030)

Non-sensitive runtime configuration is supplied via **ConfigMap**; credentials and keys via
**Secret** references or **ExternalSecret** sync — never baked into images or committed in
plaintext. Externally managed data services (PostgreSQL, Redis, Kafka, MinIO) are wired through
ConfigMap endpoints plus Secret credentials; the chart does **not** deploy in-cluster StatefulSets
for those dependencies.

## T03 task mapping

| Task | Deliverable | Evidence |
| --- | --- | --- |
| **P15-T03a** | `templates/configmap.yaml` + per-env `externalServices` / `config` in `values-*.yaml` | Rendered `*-config` ConfigMap; no credential keys in `data` |
| **P15-T03b** | `secrets.create=false`, `existingSecretName`, optional `ExternalSecret` | `docgen.secretName` helper fail-closed; `helm-validate.ps1` negative test |
| **P15-T03c** | External service hosts in ConfigMap; credentials in Secret refs | No `StatefulSet` in rendered manifests; backend `envFrom` uses both refs |

## ConfigMap (non-sensitive)

Rendered resource: `{release}-config` (`templates/configmap.yaml`).

| Key | Source | Notes |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `config.springProfilesActive` | Spring profile |
| `APP_ENVIRONMENT` | `config.appEnvironment` | Logical env label |
| `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB` | `externalServices.postgres.*` | JDBC host/port/database only |
| `REDIS_HOST`, `REDIS_PORT` | `externalServices.redis.*` | Cache endpoint |
| `KAFKA_BOOTSTRAP_SERVERS` | `externalServices.kafka.bootstrapServers` | Broker list |
| `MINIO_ENDPOINT` | `externalServices.minio.endpoint` | Object storage URL (TLS) |
| `STORAGE_PROVIDER`, `STORAGE_BUCKET` | `config.*` | Storage selection |
| `DOCGEN_SEED_DEMO_CATALOG` | `config.seedDemoCatalog` | Dev-only demo seed flag |
| `DOCGEN_SEED_DEMO_ASSET_LIBRARY` | `config.seedDemoAssetLibrary` | Wave 8 managed Asset Library seed (IMG-1/SEAL-1); default false |
| `JAVA_TOOL_OPTIONS` | `config.javaToolOptions` | JVM tmp dir for read-only root FS |
| `BACKEND_PORT` | `backend.port` | Container listen port |

**Never** put `POSTGRES_USER`, `POSTGRES_PASSWORD`, `MINIO_ROOT_*`, or `JWT_SECRET` in ConfigMap.

## Secret (credentials)

Default posture: **`secrets.create: false`** — operator provisions a cluster Secret before install.

Expected keys (consumed by Spring Boot via `envFrom`):

- `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD`
- `JWT_SECRET` — **required**, operator-generated, **≥32 bytes**. **Never** place in ConfigMap.
  Do **not** use known insecure placeholders:
  `local-dev-only-change-me-please-32bytes-min`, `prod-change-me-32-bytes-minimum-secret`
  (acceptance/prod paths refuse these fail-closed). Compose must not bake `${JWT_SECRET:-…}`
  defaults either — see [BDD-OPS-JWT-SECRET-001](../docs/behavior/ops-jwt-secret-no-default.md)
  and [runbook § Required environment variables](../docs/operations/runbook.md#required-environment-variables-production).
  Checklist [#9](../docs/operations/launch-readiness-checklist.md) stays **NO-GO** until
  implement evidence; clearing #9 alone is **not** go-live.

Per-environment `existingSecretName` (name only — no plaintext in repo):

| Environment | `existingSecretName` |
| --- | --- |
| dev | `docgen-app-secrets-dev` |
| staging | `docgen-app-secrets-staging` |
| prod | `docgen-app-secrets-prod` |

### ExternalSecret (optional)

Set `secrets.externalSecret.enabled: true` to sync from a cluster SecretStore (Vault, cloud SM, etc.).
Remote property paths are declared in `values.yaml` under `secrets.externalSecret.data` — still no
plaintext in the chart. Target Secret name matches `{release}-secrets`.

### Dev-only chart-managed Secret

For local clusters only, `secrets.create: true` with `--set secrets.data.*=...` at install time
renders `templates/secret.yaml`. **Never commit** `--set` values or plaintext in values files.

## Fail-closed behavior

1. **Render time:** When `secrets.create` is false and `externalSecret.enabled` is false,
   `docgen.secretName` calls Helm `required` on `secrets.existingSecretName`. Empty or missing
   name aborts `helm template` / `helm upgrade` with a clear error (no silent default).
2. **Pod startup:** Backend Deployments reference `secretRef` for the named Secret. If the Secret
   does not exist, the kubelet fails pod creation (`CreateContainerConfigError` / secret not found).
   The app does not start with blank credentials.
3. **Missing Secret keys:** Spring Boot fails fast on startup when required env vars are absent
   (e.g. database connection failure during context initialization).
4. **Known insecure `JWT_SECRET` (acceptance / prod path):** Application secret guard refuses
   known insecure JWT defaults (and empty/blank) fail-closed — error semantics indicate
   default/insecure secrets **without** logging the secret value
   ([BDD-OPS-JWT-SECRET-001](../docs/behavior/ops-jwt-secret-no-default.md) S2b). Helm contract
   already requires the key; operators must still supply a **non-placeholder** value.

Validation (render-only, no cluster):

```powershell
.\scripts\helm-validate.ps1 -SkipKubeconform
```

The script asserts T03 invariants and runs a negative test:

```powershell
helm template docgen-fail-closed deploy/helm/docgen --set secrets.existingSecretName=
# Expected: error — secrets.existingSecretName is required when secrets.create is false
```

## Per-environment external endpoints

Override `externalServices` in `values-dev.yaml`, `values-staging.yaml`, `values-prod.yaml`.
Example hosts use `*.example.internal` placeholders — replace with real managed-service DNS before
install.

## Related

- [Helm chart README](./helm/docgen/README.md)
- [Deployment guide](./README.md)
- [Production runbook — JWT_SECRET](../docs/operations/runbook.md#required-environment-variables-production)
- [BDD-OPS-JWT-SECRET-001](../docs/behavior/ops-jwt-secret-no-default.md) — explicit JWT provision; no compose default
- [Launch readiness checklist #9](../docs/operations/launch-readiness-checklist.md) — **NO-GO** until implement evidence
- [ADR-0030](../docs/adr/operations/0030-operational-platform-baseline.md) — env + Secret Manager baseline
- [P15 plan](../docs/plan/detail/P15-kubernetes-deployment-container-hardening.md) — P15-T03 acceptance
