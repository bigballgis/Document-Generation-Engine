# 01-dual-font-master (ACTIVE — CE-K02)

Dual-font master fidelity sample for master style authority.

- **Maturity:** `ACTIVE`
- **Master fonts:** docDefaults eastAsia=`宋体`; `ClauseBody` eastAsia=`仿宋`
- **DOCX assertions:** styleId `ClauseBody` applied; styles contain 宋体/仿宋; `word/document.xml` must not contain hard-coded `Calibri`
- **PDF:** deferred (LibreOffice optional per K07)

Rebuild master package:

```bash
python backend/src/test/resources/golden-corpus/01-dual-font-master/_build_master.py
```
