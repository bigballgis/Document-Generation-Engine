package com.bank.docgen.rendering;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;

/**
 * IBL-D2 / F21 — shared LibreOffice availability gate for tests.
 *
 * <p>Default local verify may {@link Assumptions#assumeTrue(boolean, String) skip} when
 * {@code soffice} is absent. The {@code libreoffice-ci} Maven profile sets
 * {@value #MANDATORY_PROPERTY}{@code true} so the same checks {@link Assertions#fail(String)
 * fail} instead of skipping.
 */
public final class LibreOfficeTestSupport {

    public static final String MANDATORY_PROPERTY = "docgen.libreoffice.mandatory";
    public static final String TAG = "libreoffice";
    /** Deterministically unavailable command for unit proofs (not a real binary). */
    public static final String MISSING_COMMAND_FOR_TESTS = "__docgen_soffice_missing__";

    private LibreOfficeTestSupport() {
    }

    public static boolean isMandatory() {
        String prop = System.getProperty(MANDATORY_PROPERTY);
        if (prop != null && !prop.isBlank()) {
            return Boolean.parseBoolean(prop.trim());
        }
        return "true".equalsIgnoreCase(System.getenv("DOCGEN_LIBREOFFICE_MANDATORY"));
    }

    public static String sofficeCommand() {
        return System.getenv().getOrDefault("LIBREOFFICE_COMMAND", "soffice");
    }

    public static boolean isSofficeAvailable() {
        return isSofficeAvailable(sofficeCommand());
    }

    public static boolean isSofficeAvailable(String command) {
        if (command == null || command.isBlank()) {
            return false;
        }
        try {
            Process process = new ProcessBuilder(command, "--version").redirectErrorStream(true).start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * LO-dependent entry: skip when optional + absent; fail when mandatory + absent.
     */
    public static void requireSoffice(String context) {
        requireSofficeForCommand(sofficeCommand(), context);
    }

    /**
     * Same as {@link #requireSoffice(String)} but with an explicit command (for unit proofs).
     */
    public static void requireSofficeForCommand(String command, String context) {
        if (isSofficeAvailable(command)) {
            return;
        }
        String detail = (context == null || context.isBlank())
                ? "LibreOffice soffice unavailable"
                : context;
        String commandLabel = command == null || command.isBlank() ? "<blank>" : command;
        if (isMandatory()) {
            Assertions.fail(
                    "LibreOffice mandatory lane (-Plibreoffice-ci / "
                            + MANDATORY_PROPERTY
                            + "=true): "
                            + detail
                            + " (command='"
                            + commandLabel
                            + "'). Install soffice or unset mandatory for optional local skip."
            );
        }
        Assumptions.assumeTrue(
                false,
                detail + " (optional local skip; use -Plibreoffice-ci to fail-closed)"
        );
    }
}
