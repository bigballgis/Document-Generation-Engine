package com.bank.docgen.master.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "master_revision_line_anchor")
@IdClass(MasterRevisionLineAnchorEntity.MasterRevisionLineAnchorId.class)
public class MasterRevisionLineAnchorEntity {

    @Id
    @Column(name = "revision_line_id")
    private UUID revisionLineId;

    @Id
    @Column(name = "anchor_id", length = 128)
    private String anchorId;

    @Column(name = "display_label", length = 256)
    private String displayLabel;

    @Column(name = "document_sequence", nullable = false)
    private int documentSequence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revision_line_id", insertable = false, updatable = false)
    private MasterRevisionLineEntity revisionLine;

    protected MasterRevisionLineAnchorEntity() {
    }

    public MasterRevisionLineAnchorEntity(
            UUID revisionLineId,
            String anchorId,
            String displayLabel,
            int documentSequence
    ) {
        this.revisionLineId = revisionLineId;
        this.anchorId = anchorId;
        this.displayLabel = displayLabel;
        this.documentSequence = documentSequence;
    }

    public UUID getRevisionLineId() {
        return revisionLineId;
    }

    public String getAnchorId() {
        return anchorId;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public int getDocumentSequence() {
        return documentSequence;
    }

    public void setDocumentSequence(int documentSequence) {
        this.documentSequence = documentSequence;
    }

    public void setRevisionLine(MasterRevisionLineEntity revisionLine) {
        this.revisionLine = revisionLine;
    }

    public static class MasterRevisionLineAnchorId implements Serializable {
        private UUID revisionLineId;
        private String anchorId;

        public MasterRevisionLineAnchorId() {
        }

        public MasterRevisionLineAnchorId(UUID revisionLineId, String anchorId) {
            this.revisionLineId = revisionLineId;
            this.anchorId = anchorId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof MasterRevisionLineAnchorId that)) {
                return false;
            }
            return revisionLineId.equals(that.revisionLineId) && anchorId.equals(that.anchorId);
        }

        @Override
        public int hashCode() {
            return revisionLineId.hashCode() * 31 + anchorId.hashCode();
        }
    }
}
