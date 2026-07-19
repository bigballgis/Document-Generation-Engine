# IBL-D2 / #124 — LibreOffice mandatory CI lane evidence

| Field | Value |
| --- | --- |
| Status | **Implementation complete in feature worktree** (merge / Done / Wave D Done **not** claimed) |
| Timestamp (local) | 2026-07-19 |
| Worktree | `D:/working/DGE-ibl-d2-lo-mandatory-lane` |
| Branch | `feat/ibl-d2-lo-mandatory-lane` |
| Profile | **`libreoffice-ci`** |
| Property | `docgen.libreoffice.mandatory=true` (profile); default `false` |
| JUnit tag | `@Tag("libreoffice")` |
| Docs | [libreoffice-ci-lane.md](../../../architecture/libreoffice-ci-lane.md) |
| Host soffice | **Absent** on `PATH` (`LIBREOFFICE_COMMAND` unset) |
| frontend_ui_in_scope | false |
| Go-live / #3b / #5a / Wave D Done | **not** claimed |

## Gate commands

### 1. Default verify (must stay green; optional LO skip)

```powershell
mvn -B -ntp -f backend/pom.xml verify
```

| Metric | Result |
| --- | --- |
| Result | **BUILD SUCCESS** |
| Tests | **2138** run / **0** fail / **14** skipped |
| LO gate under default | `LibreOfficeMandatoryLaneGateTest` **skipped** (optional) |
| Log | [default-verify.log](./default-verify.log) |

### 2. Mandatory LO lane without soffice (expected FAIL — CI gate proof)

```powershell
mvn -B -ntp -f backend/pom.xml "-Plibreoffice-ci,dev-fast" test "-Dtest=LibreOfficeMandatoryLaneGateTest"
```

| Metric | Result |
| --- | --- |
| Result | **BUILD FAILURE** (intentional) |
| Tests | **1** run / **1** fail / **0** skipped |
| Failure | `AssertionFailedError` … `mandatory lane` … `command='soffice'` |
| Log | [lo-mandatory-fail-without-soffice.log](./lo-mandatory-fail-without-soffice.log) |

### 3. Unit proof of skip vs fail-closed (always in default suite)

```powershell
mvn -B -ntp -f backend/pom.xml -Pdev-fast test "-Dtest=LibreOfficeTestSupportTest,LibreOfficeMandatoryLaneGateTest"
```

| Metric | Result |
| --- | --- |
| `LibreOfficeTestSupportTest` | **4** run / **0** fail (mandatory + missing command → fail; optional → abort) |
| `LibreOfficeMandatoryLaneGateTest` | **1** skipped under default (`mandatory=false`) |

### 4. Green LO lane with soffice

**Not demonstrated on this host** (no `soffice`). CI agents with LibreOffice installed should run:

```powershell
mvn -B -ntp -f backend/pom.xml "-Plibreoffice-ci,dev-fast" test
```

Expected: BUILD SUCCESS when `soffice --version` works (or `LIBREOFFICE_COMMAND` points at a working binary).

## Notes for architecture review

- Pattern mirrors IBL-D1 `-Ptestcontainers`: opt-in profile flips skip → fail-closed.
- Silent early-`return` in font smoke / parallel IT replaced with `LibreOfficeTestSupport.requireSoffice`.
- Residuals: company CI job wiring for `-Plibreoffice-ci`; LO green proof when soffice present; LO PDF golden upgrades remain honesty-bound (no invented binaries); D3–D5 / B7 / Wave D Done out of leaf.
