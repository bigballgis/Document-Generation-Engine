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

@Entity
@Table(name = "master_anchor")
@IdClass(MasterAnchorEntity.MasterAnchorId.class)
public class MasterAnchorEntity {

    @Id
    @Column(name = "master_id")
    private java.util.UUID masterId;

    @Id
    @Column(name = "anchor_id", length = 128)
    private String anchorId;

    @Column(name = "display_label", length = 256)
    private String displayLabel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_id", insertable = false, updatable = false)
    private MasterDocumentEntity master;

    protected MasterAnchorEntity() {
    }

    public MasterAnchorEntity(java.util.UUID masterId, String anchorId, String displayLabel) {
        this.masterId = masterId;
        this.anchorId = anchorId;
        this.displayLabel = displayLabel;
    }

    public String getAnchorId() {
        return anchorId;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public void setMaster(MasterDocumentEntity master) {
        this.master = master;
    }

    public static class MasterAnchorId implements Serializable {
        private java.util.UUID masterId;
        private String anchorId;

        public MasterAnchorId() {
        }

        public MasterAnchorId(java.util.UUID masterId, String anchorId) {
            this.masterId = masterId;
            this.anchorId = anchorId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof MasterAnchorId that)) {
                return false;
            }
            return masterId.equals(that.masterId) && anchorId.equals(that.anchorId);
        }

        @Override
        public int hashCode() {
            return masterId.hashCode() * 31 + anchorId.hashCode();
        }
    }
}
