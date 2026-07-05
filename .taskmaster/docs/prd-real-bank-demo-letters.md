# Real-bank-grade demo letters — drive full lifecycle through API management

## Goal
Turn the demo letter catalogue from placeholder-grade into real foreign-bank correspondence quality,
and prove each one end-to-end through: master upload → template authoring → lifecycle publish →
API policy + credential → runtime generation → real DOCX artifact.

## Context (2026-07-05)
Backend redeployed, healthy on :8080. Existing templates:
- DEMO-FULL-FLOW-LETTER (RETAIL, PUBLISHED) — minimal content, runtime call returns 403 AD Group denied
- CORP-FOL-OFFER (CORP, TESTING) — rich LMA-style content, not yet published
- CDP-MVP-GOLDEN (RETAIL, TESTING)
Simpler demos (credit-limit, mortgage, trade-lc, collection, annual-review, wealth, retail-account)
have shallow variables and short placeholder clauses — need real bank-letter content.

## Scope
1. Fix AD Group authorization mismatch so runtime generation works for published templates.
2. Publish CORP-FOL-OFFER through test→approval→publish + full API policy + credential; generate real FOL DOCX.
3. Rewrite credit-limit / mortgage / trade-lc / collection / annual-review / wealth demos to real
   foreign-bank-letter grade: parties, defined terms, covenants, schedules, signature blocks, governing law.
4. Re-import every demo package, publish each, generate a real DOCX per template, verify content + size.
5. Capture evidence (artifacts, audit records) for fundraising demo.

## Out of scope
- Code changes to runtime/apimgmt modules unless required to unblock a real failure.
- Frontend UI changes.
- Push to remote git.

## Acceptance
- Each demo template reaches PUBLISHED with a configured API policy + at least one credential.
- A runtime generate call per template returns 200 with a non-empty DOCX.
- Generated DOCX files saved under .tmp/generated_*.docx with bank-grade body content.
- task-master tasks track each demo; all marked done at completion.
