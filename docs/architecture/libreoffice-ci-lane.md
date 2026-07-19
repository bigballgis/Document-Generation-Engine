# LibreOffice CI lane — optional local skip vs mandatory fail-closed

| Field | Value |
| --- | --- |
| **Slice** | IBL-D2 / F21 (`ibl-d2-lo-mandatory-lane`) |
| **Task Master** | **#124** |
| **Recorded** | 2026-07-19 |
| **Maven profile** | **`libreoffice-ci`** |
| **System property** | `docgen.libreoffice.mandatory` (`true` under the profile; default `false`) |
| **JUnit tag** | `@Tag("libreoffice")` |

## Why two modes

LibreOffice (`soffice`) conversion is required for real PDF halves (font smoke, parallel
conversion IT, selected golden `LIBREOFFICE` packages). Developer machines and some CI
agents may not have LibreOffice on `PATH`. Closing **F21** does **not** force every local
`mvn verify` to install LibreOffice — it means a **documented mandatory CI lane** cannot
stay green by silently skipping conversion.

| Lane | Command | When `soffice` absent | When `soffice` present |
| --- | --- | --- | --- |
| **Default verify** | `mvn -B -ntp -f backend/pom.xml verify` | LO-dependent checks **skip** (Assumptions / optional) — build may stay green | LO-dependent checks **run** and must pass |
| **Mandatory LO CI** | `mvn -B -ntp -f backend/pom.xml -Plibreoffice-ci,dev-fast test` | LO-dependent checks **FAIL** (fail-closed) | LO-dependent checks **run** and must pass |

Dedicated profile with static gates:

```powershell
mvn -B -ntp -f backend/pom.xml -Plibreoffice-ci verify
```

(`-Plibreoffice-ci` alone runs **only** `@Tag("libreoffice")` tests with
`docgen.libreoffice.mandatory=true`. Use as a **second CI job**, not a replacement for
default verify.)

Override without the profile (full suite, still fail-closed):

```powershell
mvn -B -ntp -f backend/pom.xml "-Ddocgen.libreoffice.mandatory=true" test
```

Command discovery: `LIBREOFFICE_COMMAND` env (default `soffice`).

## What each mode catches

| Risk | Default verify | `-Plibreoffice-ci` |
| --- | --- | --- |
| Unit / slice logic without conversion | Yes | Not the focus |
| Missing soffice hidden as green | Possible (optional skip — **documented**) | **Fails** the lane |
| Font smoke / parallel conversion / LO golden PDF halves | Run when soffice present; skip when absent | **Must** convert; fail if soffice missing |

## Implementation pointers

- Gate helper: `LibreOfficeTestSupport` (`requireSoffice` / `requireSofficeForCommand`).
- Unit proof (always in default suite): `LibreOfficeTestSupportTest` — mandatory + missing
  command → `AssertionError`; optional → `TestAbortedException`.
- CI gate test: `LibreOfficeMandatoryLaneGateTest` (`@Tag("libreoffice")`).
- Wired callers: `RenderingFontSmokeTest`, `LibreOfficeParallelConversionIntegrationTest`,
  `GoldenCorpusActiveRunner` (LIBREOFFICE PDF halves), `LongClauseOverflowGoldenCorpusTest`,
  `GoldenCorpusHarnessTest`.
- Maven: profile `libreoffice-ci` sets Surefire `groups=libreoffice` and
  `docgen.libreoffice.mandatory=true` (via Surefire `systemPropertyVariables`).

## Honesty residuals (out of this leaf)

- Do **not** invent LibreOffice-attributed golden PDF binaries when soffice is absent
  (IBL-C3 / F19 honesty).
- Word / pixel baselines remain **IBL-B7** (Blocked).
- IBL-D3 k6 / D4 LO chaos / D5 legalhold — separate leaves; D3 path: [k6-nfr-confirmation-path.md](./k6-nfr-confirmation-path.md).

## Traceability

- Behavior readiness: [ibl-d2-lo-mandatory-lane.md](../behavior/ibl-d2-lo-mandatory-lane.md)
- Program: [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) § IBL-D2 / F21
- Evidence: [../plan/evidence/ibl-d2-lo-mandatory-lane/](../plan/evidence/ibl-d2-lo-mandatory-lane/)
- Sibling H2 vs TC: [test-database-strategy.md](./test-database-strategy.md)
