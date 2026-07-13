# OWASP dependency-check — aborted (fallback mode)

Started: `mvn -B -ntp -f backend/pom.xml org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7`

Observed: dependency-check **12.2.2** began NVD API update with **365,117** records and **no NVD API key** (warning: update can take a VERY long time). Process was stopped after ~8 minutes still at ~3% download to unblock the slice.

Per `docs/architecture/quality-gate-threshold-baseline.md` intranet-constrained policy, external dependency-check is optional/non-blocking. Blocking evidence for this slice:

- `versions:display-dependency-updates` / `display-plugin-updates`
- Known CVE research (Boot 3.3.13 / Tomcat / CVE-2025-22235; PDFBox examples-only)
- Regenerated CycloneDX SBOM for intranet SCA (M9-T02 remains separate)

Partial log: `owasp-partial.txt`
