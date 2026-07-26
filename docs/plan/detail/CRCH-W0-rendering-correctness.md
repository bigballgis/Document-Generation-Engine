# CRCH-W0 — Rendering & conversion correctness

**Program:** [CRCH](../core-render-compute-hardening-program.md)
**Wave:** W0 (stop the bleeding)
**Status:** **Not Started**
**Delivered together with:** [CRCH-W1](CRCH-W1-preview-consolidation.md) — one merged slice, one worktree
**Slice id:** `render-p0-preview-dedupe` · worktree `../DGE-render-p0-preview-dedupe` · branch `feat/render-p0-preview-dedupe`
**Audit baseline commit:** `df9a5b7d` — all line numbers below are from this commit; **locate code by the quoted snippet, not by line number**

---

## Before you write any code

1. Read §0 of [the program document](../core-render-compute-hardening-program.md). It contains the
   worktree rule, the TDD rule, the gate commands, and the "if the test passes, stop" rule.
2. Create the worktree from MAIN:

```powershell
git fetch origin
git worktree add "..\DGE-render-p0-preview-dedupe" -b feat/render-p0-preview-dedupe origin/main
```

3. Work in that directory. Do not edit anything in the MAIN checkout.

### Host constraint you must account for

The development host is **Windows**. Two consequences:

- `DockerExecPdfConversionServiceTest` is annotated `@DisabledOnOs(WINDOWS)` and **will not run
  locally**. W0-5 below is therefore specified as an OS-independent argv-level unit test.
- Real LibreOffice tests (`-Plibreoffice-ci`) will **skip** unless `soffice` is on PATH. If they
  skip, say so honestly in the evidence record. Real-LibreOffice verification happens in the
  Linux Docker acceptance stack via `.\scripts\docker-deploy-queue.ps1`. Never write "LibreOffice
  tests passed" when they were skipped.

### Task order

Execute in this order. W0-5 and W0-6 both modify `DockerExecPdfConversionService`, so W0-5 must
land before W0-6.

| Order | Task | Severity | Primary file |
| --- | --- | --- | --- |
| 1 | [W0-1](#w0-1) Body anchor index mismatch | **P0** | `DocxStructuredAnchorSupport.java` |
| 2 | [W0-2](#w0-2) Image aspect-ratio distortion | **P0** | `StructuredContentDocxInlineSupport.java` |
| 3 | [W0-3](#w0-3) Seal ignores declared size | **P0** | `StructuredContentDocxInlineSupport.java` |
| 4 | [W0-4](#w0-4) Section page-number plan is invented | **P0** | `DocxPdfPageNumberStampPlanResolver.java` |
| 5 | [W0-5](#w0-5) docker-exec cross-contamination | **P0** | `DockerExecPdfConversionService.java` |
| 6 | [W0-6](#w0-6) Orphan soffice + pipe deadlock | **P0** | 3 conversion services |

---

<a id="w0-1"></a>
## W0-1 — Body anchor index is computed against one list and consumed against another

**Severity:** P0 — crashes generation, or silently replaces the wrong paragraph
**Files:** `backend/src/main/java/com/bank/docgen/rendering/DocxStructuredAnchorSupport.java`
**Test file:** `backend/src/test/java/com/bank/docgen/rendering/DocxAssemblerTest.java`

### Current behaviour

`replaceInDocumentBody` collects anchor positions by iterating `document.getBodyElements()`,
which returns paragraphs **and tables**, interleaved in document order:

```java
List<AnchorReplacement> replacements = collectStructuredAnchorReplacements(
        document.getBodyElements(),
        bindingJsonByAnchor,
        anchorPattern
);
for (int replacementIndex = replacements.size() - 1; replacementIndex >= 0; replacementIndex--) {
    AnchorReplacement replacement = replacements.get(replacementIndex);
    writer.replaceAnchorParagraph(
            document,
            replacement.paragraphIndex(),
            ...
    );
}
```

`StructuredContentDocxWriter.replaceAnchorParagraph` then resolves that index against a
**paragraph-only** list:

```java
replaceStructuredAnchorInParagraph(
        document,
        document,
        document.getParagraphs().get(paragraphIndex),
        ...
);
```

### Why this is a defect

`getBodyElements()` and `getParagraphs()` are different collections. Every body-level table that
appears before an anchor paragraph shifts the two indices apart by one.

Concrete case — a master whose body is `[table, anchorParagraph]`:
`getBodyElements()` index of the anchor is `1`; `getParagraphs()` contains a single element at
index `0`; `get(1)` throws `IndexOutOfBoundsException`. With more paragraphs present the call may
instead succeed and rewrite an unrelated paragraph, silently producing a wrong document.

Bank letter masters routinely place a table in the body. The sibling path
(`replaceInParagraphs`, used for table cells, headers and footers) is **already correct** because
it indexes the same local list it was handed.

### Why no existing test caught it

Every master builder in `DocxAssemblerTest` creates a lone anchor paragraph with no preceding
body table.

### Step 1 — Write the failing test

Add to `DocxAssemblerTest`. Follow the file's existing master-builder style
(`document.createParagraph().createRun().setText("{{anchor:" + anchorId + "}}")`).

```java
@Test
void assemblesBodyAnchorWhenMasterBodyContainsTableBeforeAnchor() throws Exception {
    byte[] master = masterWithBodyTableBeforeAnchor("BODY");

    byte[] result = assembler.assembleStructuredFromBytes(
            master,
            Map.of("BODY", "{\"nodes\":[{\"type\":\"paragraph\",\"children\":"
                    + "[{\"type\":\"text\",\"text\":\"REPLACED BODY\"}]}]}"),
            Map.of(),
            Map.of(),
            null
    );

    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(result))) {
        String bodyText = document.getParagraphs().stream()
                .map(XWPFParagraph::getText)
                .collect(Collectors.joining("\n"));
        assertThat(bodyText).contains("REPLACED BODY");
        assertThat(bodyText).doesNotContain("{{anchor:BODY}}");
        // The pre-existing table must survive untouched.
        assertThat(document.getTables()).hasSize(1);
        assertThat(document.getTables().get(0).getRow(0).getCell(0).getText())
                .isEqualTo("HEADER TABLE");
    }
}

private static byte[] masterWithBodyTableBeforeAnchor(String anchorId) throws IOException {
    try (XWPFDocument document = new XWPFDocument();
            ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        XWPFTable table = document.createTable(1, 1);
        table.getRow(0).getCell(0).setText("HEADER TABLE");
        document.createParagraph().createRun().setText("{{anchor:" + anchorId + "}}");
        document.write(out);
        return out.toByteArray();
    }
}
```

**Expected red symptom:** `IndexOutOfBoundsException` propagating out of
`assembleStructuredFromBytes`. If instead the test passes immediately, STOP and report — see
§0.4 of the program document.

Match the surrounding test class for the exact assembler construction and the structured-content
JSON shape; copy an existing passing test in the same file as your template rather than inventing
a JSON schema.

### Step 2 — Implement

Replace the entire body of `replaceInDocumentBody` with a delegation to the already-correct
helper, so the index is produced and consumed against the same list:

```java
static void replaceInDocumentBody(
        XWPFDocument document,
        Map<String, String> bindingJsonByAnchor,
        Map<String, Object> variables,
        Map<String, String> pinnedModuleStructures,
        StructuredContentDocxWriter writer,
        Pattern anchorPattern
) {
    replaceInParagraphs(
            document,
            document,
            document.getParagraphs(),
            bindingJsonByAnchor,
            variables,
            pinnedModuleStructures,
            writer,
            anchorPattern
    );
}
```

This is behaviour-preserving for the previously-working case: `replaceAnchorParagraph(document, i, …)`
already delegated to `replaceStructuredAnchorInParagraph(document, document, document.getParagraphs().get(i), …)`,
which is exactly what `replaceInParagraphs` does. The reverse iteration that guards against index
shift when new paragraphs are inserted is preserved inside `replaceInParagraphs`.

### Do NOT

- **Do not delete `StructuredContentDocxWriter.replaceAnchorParagraph`.** It becomes unused by
  this path but is still called by `renderPlainTextProjection` in the same class. Deleting it
  breaks the plain-text projection used elsewhere.
- Do not change `collectStructuredAnchorReplacements`, `replaceInParagraphs`, or the
  `AnchorReplacement` record.
- Do not change `replaceInTablesHeadersAndFooters`.

### Acceptance

- New test red before, green after.
- `mvn -B -ntp -f backend/pom.xml verify` green, with no pre-existing rendering test regressions.

---

<a id="w0-2"></a>
## W0-2 — Every image is stretched into a square, destroying aspect ratio

**Severity:** P0 — visibly wrong documents (logos and signatures distorted)
**File:** `backend/src/main/java/com/bank/docgen/rendering/StructuredContentDocxInlineSupport.java`
**Test file:** `backend/src/test/java/com/bank/docgen/rendering/StructuredContentDocxWriterTest.java`

### Current behaviour

```java
run.addPicture(
        new java.io.ByteArrayInputStream(image.bytes()),
        XWPFDocument.PICTURE_TYPE_PNG,
        image.fileName(),
        Units.toEMU(48),
        Units.toEMU(48)
);
```

Width and height are both hardcoded to 48 points. A 200×100 logo renders as a 48×48 square —
horizontally squashed by half.

### Design constraint — do NOT change the size envelope

The fix is **fit inside the existing 48×48 point box while preserving aspect ratio**, not
"render at natural size". Enlarging images would change the layout of every existing document and
is out of scope. After this change:

| Source | Before | After |
| --- | --- | --- |
| 200×100 px | 48 × 48 pt | 48 × 24 pt |
| 100×200 px | 48 × 48 pt | 24 × 48 pt |
| 100×100 px | 48 × 48 pt | 48 × 48 pt (unchanged) |

### Step 1 — Write the failing test

In `StructuredContentDocxWriterTest`, render a structured content tree containing an `imageRef`
whose resolved bytes are a **200×100** PNG, then read back the emitted drawing extent.

Generate the PNG in the test rather than adding a fixture file:

```java
private static byte[] pngBytes(int width, int height) throws IOException {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(image, "png", out);
    return out.toByteArray();
}
```

Assert on the emitted extent:

```java
XWPFPicture picture = run.getEmbeddedPictures().get(0);
long cx = picture.getCTPicture().getSpPr().getXfrm().getExt().getCx();
long cy = picture.getCTPicture().getSpPr().getXfrm().getExt().getCy();
assertThat(cx).isEqualTo(Units.toEMU(48));
assertThat(cy).isEqualTo(Units.toEMU(24));   // 48 * (100/200)
```

Follow the existing tests in that file for how the writer and a stub
`StructuredContentImageResolver` are constructed. The resolver must be stubbed to return your
generated PNG bytes — see how existing image tests wire it.

**Expected red symptom:** `cy` is `Units.toEMU(48)` instead of `Units.toEMU(24)`.

### Step 2 — Implement

`StructuredContentImageResolver.ResolvedImage` is `record ResolvedImage(byte[] bytes, String fileName)`
— it carries **no dimensions**. You must read them from the bytes.

In `writeReferenceNode`, for the **image** branch only (the seal branch is W0-3):

1. Read intrinsic pixel dimensions with `javax.imageio.ImageIO.read(new ByteArrayInputStream(image.bytes()))`.
   `ImageIO` is part of the JDK — no new dependency.
2. If the result is `null`, or width/height is `<= 0`, or reading throws: keep the current
   48 × 48 behaviour and continue. An unreadable image must never fail the render.
3. Otherwise scale to fit the 48 pt box, preserving ratio:

```java
private static final int IMAGE_BOX_PT = 48;

// widthPx / heightPx are the intrinsic pixel dimensions
double scale = Math.min(
        (double) IMAGE_BOX_PT / widthPx,
        (double) IMAGE_BOX_PT / heightPx
);
int widthPt  = Math.max(1, (int) Math.round(widthPx  * scale));
int heightPt = Math.max(1, (int) Math.round(heightPx * scale));
```

4. Pass `Units.toEMU(widthPt)` and `Units.toEMU(heightPt)` to `addPicture`.

`Math.max(1, …)` prevents a degenerate zero dimension for extreme aspect ratios.

### Note on `applyScaling`

The authoring contract has an `applyScaling` boolean on image references that raises an
`IMAGE_SCALING_ADJUSTED` warning, and nothing in the rendering package reads it. **Leave it alone
in W0-2.** Deciding what author-controlled scaling should mean is a product question, not a bug
fix. Fixing the distortion is the P0; honouring `applyScaling` belongs to W3.

### Do NOT

- Do not change the 48 pt box size, and do not make it configurable in this task.
- Do not touch the seal branch (`"sealRef".equals(...)`) — that is W0-3.
- Do not add an image-processing dependency. `javax.imageio` is sufficient.
- Do not let an unreadable image throw. Degrade to 48 × 48.

---

<a id="w0-3"></a>
## W0-3 — Seals silently ignore the author's declared size

**Severity:** P0 — the rendered seal does not match the size the author declared and the system validated
**File:** `backend/src/main/java/com/bank/docgen/rendering/StructuredContentDocxInlineSupport.java`
**Test file:** `backend/src/test/java/com/bank/docgen/rendering/StructuredContentDocxWriterTest.java`

### Current behaviour

Seals go through the same hardcoded `Units.toEMU(48)` call as images.

Meanwhile the authoring layer defines a real geometry contract. In
`AuthorizedSealAreaCatalog.parseSealBox`, a seal's `placement.sealBox` accepts `widthPt` and
`heightPt`, falling back to `SealGeometryRules.DEFAULT_SEAL_WIDTH_PT` / `DEFAULT_SEAL_HEIGHT_PT`,
both of which are `48.0d`.

### Why this is a defect

Because the default happens to equal the hardcoded constant, the bug is invisible for
default-sized seals — which is why it survived. But an author who declares
`"sealBox": { "pageIndex": 0, "xPt": 400, "yPt": 650, "widthPt": 120, "heightPt": 120 }`
gets a 48 pt seal. The declared value is validated (it must fit inside a declared authorized
area) and then discarded at render time.

Note also that seals **forbid** `applyScaling` — `ReferenceNodeService.validateSealRef` raises a
`SEAL_SCALING_NOT_ALLOWED` **blocker** with the message "seals must render at authorized size".
The system therefore already promises that the seal renders at its authorized size. It does not.

### Step 1 — Write the failing test

Render a `sealRef` node carrying:

```json
{
  "type": "sealRef",
  "referenceKey": "OFFICIAL_SEAL",
  "placement": {
    "authorizedAreaId": "AREA_1",
    "sealBox": { "pageIndex": 0, "xPt": 100, "yPt": 100, "widthPt": 120, "heightPt": 90 }
  }
}
```

Assert the emitted extent is `Units.toEMU(120)` × `Units.toEMU(90)`.

Add a second test proving the default is unchanged: a `sealRef` with no `placement` still
renders at `Units.toEMU(48)` × `Units.toEMU(48)`.

**Expected red symptom:** the first test reports 48 × 48.

### Step 2 — Implement

In the seal branch of `writeReferenceNode`, read the declared box off the node:

```java
double widthPt  = node.path("placement").path("sealBox").path("widthPt").asDouble(DEFAULT_SEAL_PT);
double heightPt = node.path("placement").path("sealBox").path("heightPt").asDouble(DEFAULT_SEAL_PT);
```

Guard the values: if either is not finite or `<= 0`, fall back to `DEFAULT_SEAL_PT`. Then emit
`Units.toEMU((int) Math.round(widthPt))` and `Units.toEMU((int) Math.round(heightPt))`.

Use the declared box **exactly**. Do not apply the aspect-ratio fitting from W0-2 — for seals the
declared rectangle is the compliance contract, not a bounding hint.

### Where to put the `48.0` constant — read this carefully

`SealGeometryRules.DEFAULT_SEAL_WIDTH_PT` lives in `com.bank.docgen.authoring.structured`.
The rendering module may be forbidden from importing the authoring module by an ArchUnit rule
(the repository has an ArchUnit suite).

Procedure:

1. First try importing `SealGeometryRules` and run the ArchUnit tests.
2. If ArchUnit fails, do **not** weaken the ArchUnit rule. Instead declare a rendering-local
   constant:

```java
/** Mirrors SealGeometryRules.DEFAULT_SEAL_WIDTH_PT / DEFAULT_SEAL_HEIGHT_PT (48pt);
    duplicated because rendering must not depend on the authoring module. */
private static final double DEFAULT_SEAL_PT = 48.0d;
```

3. Whichever route you take, state it in the task report.

### Do NOT

- Do not implement absolute seal positioning (`pageIndex` / `xPt` / `yPt`). That is **OD-1**, an
  open decision in the program document, and is explicitly out of scope. This task changes size
  only; the seal stays inline.
- Do not remove or weaken any seal validation in the authoring module.

---

<a id="w0-4"></a>
## W0-4 — Section page numbers are invented, not measured

**Severity:** P0 — wrong page numbers on multi-section bank letters
**File:** `backend/src/main/java/com/bank/docgen/rendering/DocxPdfPageNumberStampPlanResolver.java`
**Test file:** `backend/src/test/java/com/bank/docgen/rendering/DocxPdfPageNumberStampPlanResolverTest.java` (**new file — none exists today**)

### Current behaviour

```java
private static List<Integer> extractSectionStartPages(String documentXml) {
    ...
    int sectionBreakCount = countOccurrences(documentXml, "<w:sectPr");
    if (sectionBreakCount <= 1) {
        return List.of(1);
    }
    List<Integer> sectionStarts = new ArrayList<>();
    sectionStarts.add(1);
    for (int sectionIndex = 1; sectionIndex < sectionBreakCount; sectionIndex++) {
        sectionStarts.add(sectionIndex + 1);
    }
    return List.copyOf(sectionStarts);
}
```

It counts `<w:sectPr>` occurrences in `word/document.xml` and then asserts that section *n*
starts on page *n+1* — i.e. it assumes **every section is exactly one page long**.

### Why this is a defect

Section start pages cannot be derived from `document.xml` at all; pagination is only known after
the layout engine has run. The consumer of this plan,
`PdfPageNumberStampPlan.sectionPageNumber` / `sectionPageCount`, does real arithmetic on these
numbers, so a three-page first section produces "Section Page 3 of 1"-class nonsense stamped onto
the PDF.

There is currently **no test of this resolver at all**. Existing stamping tests hand-build a
`PdfPageNumberStampPlan` and never exercise the resolver.

### Step 1 — Write the failing tests

Create `DocxPdfPageNumberStampPlanResolverTest` with these cases:

| Case | Input | Expected |
| --- | --- | --- |
| 1 | `docxBytes` null or empty | `globalOnly()` |
| 2 | Footer XML without `SECTIONPAGES` | `globalOnly()` (already true — regression guard) |
| 3 | Footer contains `SECTIONPAGES`, body has a single `<w:sectPr>` | `globalOnly()` |
| 4 | Footer contains `SECTIONPAGES`, body has **three** `<w:sectPr>` | `globalOnly()` — **this is the new behaviour and the red test** |

Build the DOCX bytes with POI (`XWPFDocument`, a footer via
`document.createFooter(HeaderFooterType.DEFAULT)`), or assemble a minimal zip containing
`word/document.xml` and `word/footer1.xml` directly — the resolver only reads those two entries
by name, so a hand-built zip is legitimate and simpler. Look at
`DocxPdfConversionPreprocessorTest` for the house pattern.

**Expected red symptom for case 4:** the resolver returns
`sectionAndGlobal([1, 2, 3])` with `dualPageNumbersEnabled == true`, instead of `globalOnly()`.

### Step 2 — Implement (fail closed)

Real section pagination is not available at this point in the pipeline. Until it is derived from
the converted PDF (a W5 item), the resolver must **degrade honestly instead of guessing**:

1. Delete `extractSectionStartPages` and `countOccurrences` if they become unused.
2. In `resolve`, when `dualPageFooter` is true but true section boundaries cannot be determined,
   return `PdfPageNumberStampPlan.globalOnly()`.
3. Log at WARN, once per conversion, that dual section page numbering was requested but could not
   be resolved.
4. Add a fidelity warning constant to
   `backend/src/main/java/com/bank/docgen/sharedkernel/document/fidelity/FidelityWarningCode.java`:

```java
/** CRCH-W0-4: footer requests SECTIONPAGES but true section pagination could not be resolved;
    stamping degraded to document-global page numbers. */
PDF_SECTION_PAGE_NUMBERS_UNRESOLVED,
```

5. Surface that warning through the **existing** stamp-result warning channel. Follow exactly how
   `PDF_PAGE_NUMBER_STAMP_SKIPPED_FOR_PDFA` flows through `PdfPageStampResult` and
   `PdfConversionPostProcessor`, and mirror it.

**If step 5 does not fit cleanly into the existing plumbing, stop and report.** Do not invent a
new warning transport. Steps 1–4 alone already remove the wrong output, which is the P0.

### Do NOT

- Do not attempt to compute real section boundaries from `document.xml`. It is not derivable
  there. Guessing is the bug.
- Do not disable stamping altogether — global page numbering must keep working.
- Do not change the `docgen.rendering.pdf-page-number-stamping-enabled` default. That is **OD-2**,
  an open decision.

---

<a id="w0-5"></a>
## W0-5 — docker-exec mode can hand one customer another customer's PDF

**Severity:** P0 — cross-request data leakage
**File:** `backend/src/main/java/com/bank/docgen/rendering/DockerExecPdfConversionService.java`
**Test file:** `backend/src/test/java/com/bank/docgen/rendering/DockerExecPdfConversionServiceTest.java`

### Current behaviour

The container-side paths are global constants while the conversion pool runs up to 4 conversions
concurrently:

```java
String containerInput = "/tmp/docgen-input.docx";
...
runCommand(..., "--convert-to", ..., "--outdir", "/tmp", containerInput);
Path outputPdf = hostDir.resolve("input.pdf");
runCommand(renderingProperties.getDockerCliCommand(), "cp", container + ":/tmp/input.pdf", outputPdf.toString());
```

### Why this is a defect

Two concurrent conversions both write `/tmp/docgen-input.docx` and both read `/tmp/input.pdf`
inside the same container. Request A can copy out the PDF produced from request B's DOCX.

The irony worth noting: three lines above, the code already computes a **unique** per-invocation
LibreOffice profile path and documents why (`CD-PIT-11`, concurrent profile collisions):

```java
Path profileDirName = hostDir.getFileName();
...
containerProfile = "/tmp/docgen-lo-profile-" + profileDirName;
```

The uniqueness discipline was applied to the profile and not to the payload.

This affects `docker-exec` mode only. Production currently runs `cli` mode
(`LIBREOFFICE_CONVERSION_MODE: cli`), so this is a latent landmine rather than an active
incident — a single configuration change arms it.

### Step 1 — Write the failing test

`DockerExecPdfConversionServiceTest` is `@DisabledOnOs(WINDOWS)` and cannot run on this host.
So make the red test **OS-independent** by testing argv construction, not execution.

Extract the container path derivation into a package-private static method and unit-test it:

```java
static String containerInputPath(String uniqueToken) { ... }
static String containerOutputPath(String uniqueToken) { ... }
```

Test:

```java
@Test
void containerPathsAreUniquePerInvocation() {
    String a = DockerExecPdfConversionService.containerInputPath("docgen-docker-pdf-111");
    String b = DockerExecPdfConversionService.containerInputPath("docgen-docker-pdf-222");
    assertThat(a).isNotEqualTo(b);
    assertThat(DockerExecPdfConversionService.containerOutputPath("docgen-docker-pdf-111"))
            .isNotEqualTo(DockerExecPdfConversionService.containerOutputPath("docgen-docker-pdf-222"));
}
```

Put this test in a new `DockerExecPdfConversionPathsTest` **without** `@DisabledOnOs`, so it runs
on Windows. Leave the existing disabled test class alone.

**Expected red symptom:** the methods do not exist (compilation failure), or return a constant.

### Step 2 — Implement

1. Derive one `uniqueToken` per invocation from the existing unique host temp dir name — the same
   value already used for `containerProfile`. Reuse it; do not generate a second random token.
2. Build all three container paths from it:
   - input: `/tmp/docgen-input-<token>.docx`
   - outdir: `/tmp/docgen-out-<token>` (pass this to `--outdir`)
   - output: `/tmp/docgen-out-<token>/docgen-input-<token>.pdf`

   LibreOffice names the output after the input file's base name, so the output filename must be
   derived from the input filename, not hardcoded to `input.pdf`. Verify this in the argv test.
3. Extend the `finally` block to remove the container-side input file and output directory, next
   to the existing `bestEffortContainerProfileCleanup`. Today only the profile is cleaned, so
   input and output files accumulate inside the container indefinitely.

### Do NOT

- Do not remove or weaken the existing per-invocation profile isolation.
- Do not change the pool size or the concurrency model.
- Do not enable `docker-exec` mode anywhere. It stays opt-in.
- Do not remove `@DisabledOnOs(WINDOWS)` from the existing test class.

---

<a id="w0-6"></a>
## W0-6 — Timed-out LibreOffice processes are never killed, and their output pipe is never drained

**Severity:** P0 — resource exhaustion, plus a self-inflicted guaranteed timeout
**Files (three call sites):**
- `backend/src/main/java/com/bank/docgen/rendering/LibreOfficePdfConversionService.java`
- `backend/src/main/java/com/bank/docgen/rendering/DockerExecPdfConversionService.java`
- `LibreOfficeDocxNormalizationService.java` (locate it — same package family; it is a copy-paste of the same shape)

### Current behaviour

```java
processBuilder.redirectErrorStream(true);
Process process = processBuilder.start();
boolean finished = process.waitFor(
        renderingProperties.getConversionTimeoutSeconds(),
        TimeUnit.SECONDS
);
if (!finished || process.exitValue() != 0) {
    throw new RenderingOperationException("api.error.generation.pdfConversionFailed");
}
```

### Why this is two defects, not one

**Defect A — the pipe is never drained.** `redirectErrorStream(true)` merges stderr into stdout,
but nothing ever reads that stream. When LibreOffice writes more than the OS pipe buffer
(typically 64 KB) it blocks forever on the write, and the conversion is then *guaranteed* to hit
the 120 s timeout. The code creates the hang it then reports.

**Defect B — the process is never killed.** On timeout the method throws without calling
`destroy()` or `destroyForcibly()`. The orphaned `soffice` keeps its CPU, memory and temp files.
Resilience4j is configured to retry twice, so one hung conversion can leave three orphans behind.
`future.cancel(true)` in `PdfConversionOffloadSupport` does not help — cancelling a Java task does
not kill a native child process.

### Step 1 — Write the failing test

Follow the existing fake-executable pattern used by `LibreOfficePdfConversionServiceTest` (find how
it fabricates a stand-in for `soffice`; note the Windows constraint — if the existing pattern is
shell-script based it may be Linux-only, in which case add your test alongside it using the same
guard and record honestly that it skips locally).

Write a test that points the service at a fake executable which sleeps well beyond the configured
timeout, then asserts:

1. the expected `RenderingOperationException` is thrown, **and**
2. the child process is no longer alive afterwards.

For (2), have the test capture the spawned `Process` — the cleanest way is to assert on the
extracted helper directly (see Step 2) rather than through the whole service:

```java
@Test
void terminatesProcessOnTimeout() throws Exception {
    ProcessBuilder builder = /* fake executable that sleeps 60s */;
    Process[] captured = new Process[1];

    assertThatThrownBy(() ->
            ExternalProcessRunner.runToCompletion(builder, 1, "api.error.generation.pdfConversionFailed", captured))
        .isInstanceOf(RenderingOperationException.class);

    assertThat(captured[0].isAlive()).isFalse();
}
```

Simplify the capture mechanism to match house style — the essential assertion is
**`isAlive() == false` after the timeout**.

**Expected red symptom:** the process is still alive.

The existing chaos test `LibreOfficePdfConversionPoolChaosIntegrationTest` already asserts that
the exception is thrown; it does **not** assert process death. That is the gap.

### Step 2 — Implement a shared helper and use it in all three places

Create `backend/src/main/java/com/bank/docgen/rendering/ExternalProcessRunner.java`:

```java
package com.bank.docgen.rendering;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * CRCH-W0-6: single place where external processes (LibreOffice CLI, docker CLI) are run.
 * Guarantees the output pipe cannot fill and that a timed-out process is terminated.
 */
final class ExternalProcessRunner {

    private static final long TERMINATION_GRACE_SECONDS = 5L;

    private ExternalProcessRunner() {
    }

    static void runToCompletion(ProcessBuilder builder, long timeoutSeconds, String failureMessageKey)
            throws IOException, InterruptedException {
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        Process process = builder.start();
        boolean finished;
        try {
            finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            terminate(process);
            throw ex;
        }
        if (!finished) {
            terminate(process);
            throw new RenderingOperationException(failureMessageKey);
        }
        if (process.exitValue() != 0) {
            throw new RenderingOperationException(failureMessageKey);
        }
    }

    private static void terminate(Process process) {
        process.destroy();
        try {
            if (!process.waitFor(TERMINATION_GRACE_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException ex) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }
}
```

Why `Redirect.DISCARD` rather than a drain thread: combined with `redirectErrorStream(true)` it
routes merged output to the null device, so no pipe exists and no buffer can fill. It is the
smallest change that provably removes the deadlock, and none of the three call sites currently
reads the output anyway.

Then replace the inline process handling at all three call sites with a call to
`ExternalProcessRunner.runToCompletion(...)`.

### Do NOT

- Do not change the timeout value, the retry configuration, or the circuit-breaker settings.
- Do not add a stdout-capture-to-file feature. It would be genuinely useful for diagnostics but it
  is a separate improvement; keep this task to the deadlock and the orphan.
- Do not leave any of the three call sites on the old inline pattern. A partial fix guarantees the
  next person assumes it is handled everywhere.

---

## W0 exit criteria

| # | Criterion |
| --- | --- |
| 1 | All six tasks have a test that was observed **red first**, then green |
| 2 | `mvn -B -ntp -f backend/pom.xml verify` green |
| 3 | `mvn -B -ntp -f backend/pom.xml verify -Plibreoffice-ci` run, with skips recorded honestly if `soffice` is absent |
| 4 | No change to any public API contract, error code, or configuration default (except the new `FidelityWarningCode` constant from W0-4) |
| 5 | OD-1 (seal absolute positioning) and OD-2 (stamping default) remain **unresolved and reported**, not silently decided |
| 6 | A short evidence note per task: what was red, what made it green |
