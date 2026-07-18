package com.bank.docgen.rendering.goldencorpus;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import org.verapdf.core.EncryptedPdfException;
import org.verapdf.core.ModelParsingException;
import org.verapdf.core.ValidationException;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.results.ValidationResult;

/**
 * Machine PDF/A-2b validation via veraPDF Greenfield (IBL-B3 / F12).
 *
 * <p>Unlike {@link PdfAidXmpAssertor}, this runs the full PDF/A-2b profile — not XMP-only.
 *
 * <p>Local opt-out (never for CI): {@code -Ddocgen.verapdf.skip=true} or
 * {@code DOCGEN_VERAPDF_SKIP=true}. Default {@code mvn verify} must not set either; when
 * {@code docgen.verapdf.required=true} (verify default) a skip request fails the gate.
 */
public final class VeraPdfPdfA2bAssertor {

    private static final Object INIT_LOCK = new Object();
    private static volatile boolean foundryInitialised;

    private VeraPdfPdfA2bAssertor() {
    }

    /**
     * @return true when local skip was requested
     */
    public static boolean isSkipRequested() {
        if (Boolean.parseBoolean(System.getProperty("docgen.verapdf.skip", "false"))) {
            return true;
        }
        String env = System.getenv("DOCGEN_VERAPDF_SKIP");
        return env != null && Boolean.parseBoolean(env.trim());
    }

    /**
     * Verify/CI default is required ({@code true}). Local inner-loop may set
     * {@code -Ddocgen.verapdf.required=false} together with skip.
     */
    public static boolean isGateRequired() {
        return Boolean.parseBoolean(System.getProperty("docgen.verapdf.required", "true"));
    }

    /**
     * Enforce skip policy for gate tests: required+skip → fail; optional+skip → caller may assume-out.
     *
     * @return true when the caller should proceed to validate
     */
    public static boolean shouldValidateOrFailIfRequired() {
        if (!isSkipRequested()) {
            return true;
        }
        if (isGateRequired()) {
            throw new GoldenCorpusException(
                    "veraPDF PDF/A-2b gate skip requested while docgen.verapdf.required=true — "
                            + "CI/verify must not set docgen.verapdf.skip / DOCGEN_VERAPDF_SKIP"
            );
        }
        return false;
    }

    public static void assertPdfA2b(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new GoldenCorpusException("veraPDF PDF/A-2b assertion failed: empty PDF bytes");
        }
        ensureFoundry();
        PDFAFlavour flavour = PDFAFlavour.fromString("2b");
        try (InputStream input = new ByteArrayInputStream(pdfBytes);
                PDFAParser parser = Foundries.defaultInstance().createParser(input, flavour)) {
            PDFAValidator validator = Foundries.defaultInstance().createValidator(flavour, false);
            ValidationResult result = validator.validate(parser);
            if (result == null || !result.isCompliant()) {
                throw new GoldenCorpusException(
                        "veraPDF PDF/A-2b assertion failed: document is not compliant"
                                + summariseFailures(result)
                );
            }
        } catch (GoldenCorpusException ex) {
            throw ex;
        } catch (IOException | ValidationException | ModelParsingException | EncryptedPdfException ex) {
            throw new GoldenCorpusException(
                    "veraPDF PDF/A-2b assertion failed: " + ex.getClass().getSimpleName()
                            + (ex.getMessage() == null ? "" : (": " + ex.getMessage())),
                    ex
            );
        }
    }

    private static void ensureFoundry() {
        if (foundryInitialised) {
            return;
        }
        synchronized (INIT_LOCK) {
            if (!foundryInitialised) {
                VeraGreenfieldFoundryProvider.initialise();
                foundryInitialised = true;
            }
        }
    }

    private static String summariseFailures(ValidationResult result) {
        if (result == null || result.getFailedChecks() == null || result.getFailedChecks().isEmpty()) {
            return "";
        }
        int failed = result.getFailedChecks().size();
        String sample = result.getFailedChecks().keySet().stream()
                .limit(3)
                .map(Object::toString)
                .reduce((a, b) -> a + "; " + b)
                .orElse("");
        return String.format(Locale.ROOT, " (%d failed check(s): %s)", failed, sample);
    }
}
