# Slim R6-render — WriteSession / DocxAssembler / ConditionEvaluator peel evidence

Branch: `feat/slim-r6-render` (base `main` @ `dac7907`)
Date: 2026-07-13

## Approach

- Dead private methods: **none** (anchor replace, inline/table/style writers, and expression parser/AST all had in-file call sites)
- Behavior-preserving package-private collaborators in the same packages (`rendering` / `expression`)
- Public constructors / method signatures unchanged (`DocxAssembler` Spring bean API; `ConditionExpressionEvaluator` `INSTANCE` + `validateSyntax` / `extractVariableReferences` / `evaluate`)
- Package-private `StructuredContentDocxWriteSession` constructor and `writeBlockNodes` orchestration surface unchanged; helpers constructed internally
- Demo `.docx` fixtures under `deploy/` / `frontend/e2e/fixtures/` may appear dirty locally — **not** part of this commit

## LOC before / after (touched god-classes)

Non-blank lines (`Measure-Object -Line`):

| File | Before | After | Delta |
|------|-------:|------:|------:|
| `StructuredContentDocxWriteSession.java` | 526 | 305 | -221 |
| `DocxAssembler.java` | 447 | 243 | -204 |
| `ConditionExpressionEvaluator.java` | 446 | 179 | -267 |

Targets (~305 / ~243 / ~179) — **met**.

## Extracted collaborators (new)

| Collaborator | Role | LOC |
|--------------|------|----:|
| `StructuredContentDocxInlineSupport` | Inline text/image/emphasis runs for write session | 116 |
| `StructuredContentDocxStyleSupport` | StyleRef resolve, paragraph/run styling, emphasis | 62 |
| `StructuredContentDocxTableSupport` | Table populate / loop-row / scoped variables | 104 |
| `DocxPlainAnchorParagraphSupport` | Plain `{{anchor:...}}` paragraph replace + expand | 155 |
| `DocxStructuredAnchorSupport` | Structured-content anchor collect + writer replace | 100 |
| `ConditionExpressionAst` | Sealed Expr AST + RHS literals | 61 |
| `ConditionExpressionParser` | Tokenizer/parser + `ParseException` | 216 |

## Residuals

- Facades still own orchestration entry points (`writeBlockNodes`, assemble/buildAnchor*, evaluate/validate)
- Further peels optional only if soft budgets drift again; no additional god-class targets in this wave

## Verify

- Full: `mvn -B -ntp -f backend/pom.xml verify` — **BUILD SUCCESS** (Tests run: 1347, Failures: 0, Errors: 0, Skipped: 7; PMD/SpotBugs clean)
