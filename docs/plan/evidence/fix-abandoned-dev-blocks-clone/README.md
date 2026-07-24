# Evidence — fix-abandoned-dev-blocks-clone (#165)

**Slice:** `fix-abandoned-dev-blocks-clone`  
**Merge:** `c1bb6c77` · feature `cfefbb55`  
**Gates:** `mvn verify` **GREEN 2406**; E2E/UIUX **N/A** (API/resolver-only; FE untouched)

## Docker / acceptance

| File | What it proves |
| --- | --- |
| [stage5-10-demo-covenant-waiver-version-lines.json](./stage5-10-demo-covenant-waiver-version-lines.json) | `DEMO-COVENANT-WAIVER`: PUBLISHED `1.0.0` `cloneable=true`; abandoned sibling `lifecycleStatus=STOPPED` + `cloneUnblocked=true` |
| [stage5-10-version-lines-raw.json](./stage5-10-version-lines-raw.json) | Raw version-lines API capture from live stack |

Do **not** flip checklist **#3b/#5a**; do **not** mark **#53** / **#106** Done; do **not** activate **#119**.
