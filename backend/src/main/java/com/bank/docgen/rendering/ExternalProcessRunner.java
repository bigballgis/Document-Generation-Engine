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
        runToCompletion(builder, timeoutSeconds, failureMessageKey, null);
    }

    /**
     * @param captured optional single-element array to observe the started process (tests).
     */
    static void runToCompletion(
            ProcessBuilder builder,
            long timeoutSeconds,
            String failureMessageKey,
            Process[] captured
    ) throws IOException, InterruptedException {
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        Process process = builder.start();
        if (captured != null && captured.length > 0) {
            captured[0] = process;
        }
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
