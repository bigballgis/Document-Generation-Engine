# 03-nested-clauses (ACTIVE)

Minimal nested-clauses golden sample for CE-K07 skeleton.

- **Maturity:** `ACTIVE` — runs in `mvn verify`.
- **Input:** master DOCX with `{{anchor:BODY}}`; template binds a structured payload
  with one plain paragraph plus a `conditionBlock` (expression `${showNotice} == true`)
  whose children are a nested paragraph.
- **Variables:** `{"showNotice": true}` so the conditional notice renders.
- **DOCX assertions:** `word/document.xml` contains `Nested clause body` and
  `Conditional notice applies`.
- **PDF assertions:** extracted text contains the same substrings (skipped when
  `soffice` is unavailable per K07-C9).

Enrichment responsibility: this is the minimal ACTIVE sample shipped by CE-K07.
Later CE-K02/K03 fidelity slices may add deeper nesting fixtures here rather
than creating a parallel golden root.
