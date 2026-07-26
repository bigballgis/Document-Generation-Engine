package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

class ExternalProcessRunnerTest {

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void terminatesProcessOnTimeout_crchW06() {
        ProcessBuilder builder = new ProcessBuilder("sleep", "60");
        Process[] captured = new Process[1];

        long started = System.currentTimeMillis();
        assertThatThrownBy(() -> ExternalProcessRunner.runToCompletion(
                        builder, 1L, "api.error.generation.pdfConversionFailed", captured))
                .isInstanceOf(RenderingOperationException.class);
        assertThat(System.currentTimeMillis() - started).isLessThan(15_000L);
        assertThat(captured[0]).isNotNull();
        assertThat(captured[0].isAlive()).isFalse();
    }
}
