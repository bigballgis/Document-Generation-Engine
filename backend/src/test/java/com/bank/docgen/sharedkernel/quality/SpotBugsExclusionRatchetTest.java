package com.bank.docgen.sharedkernel.quality;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Ratchet guard for {@code backend/config/spotbugs/exclude.xml}. Increasing exclusion
 * breadth requires an explicit baseline update in {@code docs/plan/spotbugs-exclusion-ratchet.md}.
 */
class SpotBugsExclusionRatchetTest {

    private static final Path EXCLUDE_XML = Path.of("config/spotbugs/exclude.xml");
    private static final Path RATCHET_PLAN = Path.of("..", "docs/plan/spotbugs-exclusion-ratchet.md");

    /** Baseline after SOR-A05 slice 0 (REC_CATCH_EXCEPTION removed). */
    private static final int BASELINE_MATCH_COUNT = 3;

    private static final List<String> BANNED_PATTERNS = List.of(
            "REC_CATCH_EXCEPTION"
    );

    private static final Pattern MATCH_BLOCK = Pattern.compile("<Match>", Pattern.CASE_INSENSITIVE);
    private static final Pattern BUG_PATTERN = Pattern.compile(
            "<Bug\\s+pattern=\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE
    );

    @Test
    void excludeFilterMatchCountMustNotIncreaseWithoutPlanUpdate() throws IOException {
        String xml = Files.readString(EXCLUDE_XML);
        int matchCount = countMatches(MATCH_BLOCK, xml);
        assertEquals(
                BASELINE_MATCH_COUNT,
                matchCount,
                "SpotBugs exclude.xml <Match> count changed. Update BASELINE_MATCH_COUNT in this test "
                        + "and docs/plan/spotbugs-exclusion-ratchet.md with justification."
        );
    }

    @Test
    void bannedPatternsMustNotReappearInExcludeFilter() throws IOException {
        String xml = Files.readString(EXCLUDE_XML);
        for (String banned : BANNED_PATTERNS) {
            assertFalse(
                    xml.contains(banned),
                    "Banned SpotBugs pattern reintroduced in exclude.xml: " + banned
                            + ". Fix code or document a new ratchet slice before re-adding."
            );
        }
    }

    @Test
    void ratchetPlanDocumentsCurrentBaseline() throws IOException {
        assertTrue(Files.isRegularFile(RATCHET_PLAN), "Missing ratchet plan: " + RATCHET_PLAN);
        String plan = Files.readString(RATCHET_PLAN);
        assertTrue(
                plan.contains("BASELINE_MATCH_COUNT=" + BASELINE_MATCH_COUNT),
                "Ratchet plan must record BASELINE_MATCH_COUNT=" + BASELINE_MATCH_COUNT
        );
        for (String banned : BANNED_PATTERNS) {
            assertTrue(
                    plan.contains(banned),
                    "Ratchet plan must document banned pattern: " + banned
            );
        }
    }

    @Test
    void excludeFilterDocumentsDeferredPatterns() throws IOException {
        String xml = Files.readString(EXCLUDE_XML);
        assertTrue(
                BUG_PATTERN.matcher(xml).results()
                        .anyMatch(match -> match.group(1).contains("EI_EXPOSE_REP")),
                "Expected deferred EI_EXPOSE_REP exclusion to remain documented in exclude.xml"
        );
    }

    private static int countMatches(Pattern pattern, String text) {
        return (int) pattern.matcher(text).results().count();
    }
}
