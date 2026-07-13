# deps-security-refresh — Backend Maven dependency security audit (Task #49)

| Field | Value |
| --- | --- |
| **Slice** | `deps-security-refresh` |
| **Task Master** | **#49** |
| **Date (local)** | 2026-07-13 |
| **Worktree** | `D:/working/DGE-deps-security-refresh` · `feat/deps-security-refresh` |
| **Audit mode** | **Fallback** — versions plugin + known CVE research + SBOM regen (OWASP NVD download aborted) |

## Audit mode rationale

`org.owasp:dependency-check-maven:check` was started but remained blocked on an unauthenticated NVD API refresh (`365,117` records; no API key). Per [quality-gate-threshold-baseline.md](../../architecture/quality-gate-threshold-baseline.md) intranet-constrained policy, external dependency-check is **optional / non-blocking**. Fallback evidence:

1. `mvn versions:display-dependency-updates` → `docs/operations/deps-security-refresh-versions-deps.txt`
2. `mvn versions:display-plugin-updates` → `docs/operations/deps-security-refresh-versions-plugins.txt`
3. Public CVE / release notes research (Spring Boot 3.3.13, Tomcat/CVE-2025-24813 path via Boot BOM, CVE-2025-22235 ≤3.3.10, PDFBox examples-only CVE-2026-23907/33929)
4. Backend CycloneDX SBOM via `.\scripts\generate-sbom.ps1 -BackendOnly` (artifact under `artifacts/sbom/`, gitignored)

## Severity summary (changed / direct dependency scope)

| Severity | Count | Notes |
| --- | --- | --- |
| **Critical** | **0 open** after remediation | Tomcat RCE class (CVE-2025-24813) mitigated by Boot **3.3.13** → Tomcat **10.1.42** (requires specific PUT/config; still remediated via BOM bump) |
| **High** | **0 open** after remediation | CVE-2025-22235 (Spring Boot `EndpointRequest`, affected ≤3.3.10) remediated by **3.3.13** |
| **Medium** | **1 residual note** | PDFBox CVE-2026-23907 / CVE-2026-33929 affect **examples** module only; core `pdfbox` jar not affected — still bumped **3.0.3 → 3.0.8** for hygiene |
| **Info / deferred** | See deferred table | Major-line upgrades blocked by ADR baselines |

### Top findings (pre-remediation)

1. **Spring Boot 3.3.5** — behind final OSS 3.3.x patch **3.3.13**; includes security-relevant BOM upgrades (Tomcat, Spring Security). **Remediated.**
2. **PDFBox 3.0.3** — examples-path traversal CVEs (medium, examples-only). **Remediated (hygiene).**
3. **BouncyCastle 1.78.1 → 1.85**, **commons-io / POI / minio 8.5.x / jjwt 0.12.x / resilience4j 2.x** — patch/minor within baseline. **Remediated.**
4. **Spring Boot 3.4+/3.5/4.x, ShedLock 7.x, springdoc 2.7+/3.x, minio 9.x, logstash-logback 9.x, jjwt 0.13.x, mapstruct 1.7 beta, Checkstyle 12+/13.x** — **Deferred (ADR / baseline).**

## Upgrades applied

| Coordinate | Before | After | Rationale |
| --- | --- | --- | --- |
| `spring-boot-starter-parent` | 3.3.5 | **3.3.13** | Latest OSS 3.3.x; security BOM |
| `jjwt.*` | 0.12.6 | **0.12.7** | Patch within 0.12.x |
| `minio` | 8.5.14 | **8.5.17** | Patch within 8.5.x (not 9.x) |
| `poi-ooxml` | 5.3.0 | **5.5.1** | Latest 5.x |
| `commons-io` | 2.18.0 | **2.22.0** | Latest 2.x |
| `logstash-logback-encoder` | 8.0 | **8.1** | Patch within 8.x (not 9.x) |
| `pdfbox` | 3.0.3 | **3.0.8** | Hygiene / examples CVE line |
| `bcprov-jdk18on` | 1.78.1 | **1.85** | Crypto provider patches |
| `resilience4j-*` | 2.2.0 | **2.4.0** | Minor within 2.x |
| `swagger-parser` (test) | 2.1.22 | **2.1.45** | Patch within 2.1.x |
| `archunit-junit5` (test) | 1.3.0 | **1.4.2** | Minor within 1.x |
| `checkstyle` | 10.20.1 | **10.23.0** | Stay on 10.x |
| `jacoco-maven-plugin` | 0.8.12 | **0.8.15** | Plugin patch |
| `maven-surefire-plugin` | 3.2.5 | **3.5.3** | Stable 3.5.x (avoid M1) |
| `cyclonedx-maven-plugin` | 2.9.1 | **2.9.2** | SBOM plugin patch |

**Not bumped (gate regression / noise):** `pmd-plugin` remains **3.26.0** (3.28.0 → PMD 7.17 added 38 non-CVE rule hits); `spotbugs-plugin` remains **4.8.6.4** (4.10.x deferred with tooling slice).

**Classpath note:** `poi-ooxml` / `pdfbox` / BouncyCastle (`bcprov-jdk18on`) remain on the single-module backend classpath; module isolation is enforced via ArchUnit (not separate classloaders).

### Unchanged (already latest within allowed line)

| Coordinate | Version | Note |
| --- | --- | --- |
| `springdoc` | 2.6.0 | Boot 3.3 matrix pin |
| `shedlock` | 6.10.0 | Latest 6.x |
| `querydsl` | 5.1.0 | Latest 5.1.x |
| `mapstruct` | 1.6.3 | Latest 1.6.x (1.7 is Beta) |
| `bucket4j-core` | 8.10.1 | Latest 8.x on Central |

## Deferred (ADR / baseline blocked)

| Upgrade | Why deferred | Follow-up |
| --- | --- | --- |
| Spring Boot **3.4+ / 3.5 / 4.x** | Session constraint + ADR-0028 baseline; Boot 3.3 OSS EOL noted as residual | New ADR / user confirmation to leave 3.3.x |
| ShedLock **7.x** | Targets Boot 3.4+ / Spring 6.2 | With Boot major bump |
| springdoc **2.7+ / 2.8+ / 3.x** | Matrix: 2.6.x ↔ Boot 3.3; 2.7+ ↔ 3.4+; 3.x ↔ Boot 4 | With Boot bump |
| MinIO Java SDK **8.6 / 9.x** | Major client line | Compatibility spike + ADR |
| logstash-logback **9.x** | Major encoder line | Compatibility spike |
| jjwt **0.13.x** | Minor API line beyond 0.12 pin | Review API deltas |
| mapstruct **1.7.0.Beta2** | Pre-release | Wait for GA |
| Checkstyle **12+/13.x** | Ruleset migration risk | Dedicated tooling task |
| PMD plugin **3.28+** / SpotBugs **4.10+** | New rule noise fails verify without product CVE benefit | Dedicated quality-tooling slice |

## Docker / compose image notes

| Image / pin | Status | Action |
| --- | --- | --- |
| `eclipse-temurin:21-jre-jammy` | Java 21 JRE — keep | No change |
| `nginx:1.27-alpine` | Frontend runtime | Keep within approved alpine line; bump only with company image approval |
| `postgres:16-alpine` / `redis:7-alpine` | Floating minor tags | Acceptable for LAB; pin digests for claimed prod |
| `minio/minio:RELEASE.2024-12-18T13-15-44Z` | Dated release pin | Residual — refresh only with company-approved MinIO image |
| `linuxserver/libreoffice:latest` | Floating `latest` | Residual risk — prefer digest/pin for prod (ADR-0030 / ops) |
| `KAFKA_IMAGE` | Operator-supplied | Unchanged (#10 CONDITIONAL) |

No Docker tag changes applied in this slice (approved-image policy; avoid inventing coordinates).

## Gate evidence

| Gate | Result | Artifact |
| --- | --- | --- |
| `mvn -B -ntp -f backend/pom.xml verify` | **GREEN** (`VERIFY_EXIT=0`; Tests run: **1355**, Failures: 0, Errors: 0, Skipped: 7) | `gate-verify.txt` / `gate-verify-exit.txt` |
| Backend SBOM | Regenerated after bumps | `artifacts/sbom/backend-cyclonedx.json` (gitignored); hash in `sbom-sha256.txt` |

## Residual risks

1. **Spring Boot 3.3.x OSS EOL** (final OSS patch 3.3.13, 2025-06) — no further community CVE fixes without commercial NES or major-line upgrade.
2. OWASP dependency-check not completed this session — rely on intranet SCA (M9-T02) for org-blocking Critical/High after SBOM upload.
3. Floating Docker tags (`libreoffice:latest`, alpine minors) and dated MinIO release pin remain ops residual.
4. This slice does **not** close M9-T02 org intranet SCA gate.

## no-commit

Implementer returns evidence only; **no commit / no push / no post-task-doc-sync** (orchestrator owns MAIN stages 12–13 after merge).
