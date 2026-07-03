package com.bank.docgen.rendering.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.rendering.domain.BatchTestRunStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BatchTestRunEntityTest {

    @Test
    void startNew_createsRunningEntity() {
        UUID id = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();

        BatchTestRunEntity run = BatchTestRunEntity.startNew(id, templateId, versionId, "author", 5);

        assertThat(run.getId()).isEqualTo(id);
        assertThat(run.getTemplateId()).isEqualTo(templateId);
        assertThat(run.getTemplateVersionId()).isEqualTo(versionId);
        assertThat(run.getCreatedBy()).isEqualTo("author");
        assertThat(run.getTotalSamples()).isEqualTo(5);
        assertThat(run.getStatus()).isEqualTo(BatchTestRunStatus.RUNNING);
        assertThat(run.isHidden()).isFalse();
        assertThat(run.getInvalidatedAt()).isNull();
        assertThat(run.getCompletedAt()).isNull();
    }

    @Test
    void completeRun_updatesAllFields() {
        BatchTestRunEntity run = BatchTestRunEntity.startNew(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "author", 3
        );

        run.completeRun(2, 1, 0, 0, "[{}]",
                BigDecimal.valueOf(90.0), BigDecimal.valueOf(85.0), BigDecimal.valueOf(100.0),
                false, false);

        assertThat(run.getStatus()).isEqualTo(BatchTestRunStatus.COMPLETED);
        assertThat(run.getSucceededCount()).isEqualTo(2);
        assertThat(run.getFailedCount()).isEqualTo(1);
        assertThat(run.getAllSamplesSucceeded()).isFalse();
        assertThat(run.getGatePassed()).isFalse();
        assertThat(run.getAnchorCoveragePct()).isEqualByComparingTo("90.0");
        assertThat(run.getCompletedAt()).isNotNull();
    }

    @Test
    void failRun_setsFailedStatus() {
        BatchTestRunEntity run = BatchTestRunEntity.startNew(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "author", 3
        );

        run.failRun();

        assertThat(run.getStatus()).isEqualTo(BatchTestRunStatus.FAILED);
        assertThat(run.getCompletedAt()).isNotNull();
    }

    @Test
    void invalidate_setsInvalidatedAt() {
        BatchTestRunEntity run = BatchTestRunEntity.startNew(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "author", 3
        );
        assertThat(run.getInvalidatedAt()).isNull();

        run.invalidate();

        assertThat(run.getInvalidatedAt()).isNotNull();
    }

    @Test
    void hide_setsHiddenTrue() {
        BatchTestRunEntity run = BatchTestRunEntity.startNew(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "author", 3
        );
        assertThat(run.isHidden()).isFalse();

        run.hide();

        assertThat(run.isHidden()).isTrue();
    }

    @Test
    void legacyConstructor_setsCompletedStatus() {
        BatchTestRunEntity run = new BatchTestRunEntity(
                UUID.randomUUID(), UUID.randomUUID(), "author", 3, 3, 0, 0, 0, "[]"
        );

        assertThat(run.getStatus()).isEqualTo(BatchTestRunStatus.COMPLETED);
        assertThat(run.isHidden()).isFalse();
        assertThat(run.getCompletedAt()).isNotNull();
    }
}
