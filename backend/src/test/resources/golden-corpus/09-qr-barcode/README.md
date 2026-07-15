# 09-qr-barcode (ACTIVE)

CE-K06b golden sample: `qrBarcodeRef` → ZXing PNG embedded in DOCX.

- **Maturity:** `ACTIVE` — DOCX half executed by golden-corpus harness in `mvn verify`.
- **Behavior:** variables payload for `PAYMENT-QR` encoded as QR and embedded via `addPicture`.
- **Assertions:** `expected/docx-assertions.json` requires `drawing` / `blip` (structural; no pixel compare).
- **PDF:** deferred (`expected/pdf-assertions.json`); no soffice required for this package.
