# 07-encrypted-pdf (ACTIVE)

Minimal encrypted-PDF golden sample for CE-K07 skeleton.

- **Maturity:** `ACTIVE` — runs in `mvn verify`.
- **Input:** master DOCX with `{{anchor:BODY}}`; template binds a single paragraph
  with the literal `Encrypted sample body`.
- **DOCX assertions:** `word/document.xml` contains `Encrypted sample body`.
- **PDF assertions (soffice required, else skipped per K07-C9):**
  1. Convert DOCX → PDF.
  2. Encrypt with `PdfEncryptionService` using the test-fixture
     `openPassword`/`ownerPassword` and `ALLOW_PRINT` permission.
  3. Assert loading without the password fails (encrypted observable evidence).
  4. Assert that after decrypting with the fixture `openPassword`, extracted
     text contains `Encrypted sample body`.

The fixture password is test-only; it is NOT used in production configuration
and is NOT emitted in failure/log assertions (K07-C7).
