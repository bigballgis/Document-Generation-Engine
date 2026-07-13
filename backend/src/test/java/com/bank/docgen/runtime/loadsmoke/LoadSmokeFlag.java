package com.bank.docgen.runtime.loadsmoke;

/**
 * LR-D6 gate: Docker load-smoke harness runs only when this system property is {@code true}.
 * Normal {@code mvn verify} must leave the property unset so the harness is never executed.
 */
public final class LoadSmokeFlag {

    /** Maven / JVM system property that enables the Docker load-smoke IT. */
    public static final String PROPERTY = "docgen.loadSmoke";

    private LoadSmokeFlag() {
    }

    public static boolean isEnabled() {
        return Boolean.parseBoolean(System.getProperty(PROPERTY, "false"));
    }
}
