package com.bank.docgen.library.persistence;

import java.io.Serializable;
import java.util.Objects;

/** Composite natural identity for group-scoped library assets (ALGI-C2). */
public class LibraryAssetId implements Serializable {

    private String groupCode;
    private String assetKey;

    public LibraryAssetId() {
    }

    public LibraryAssetId(String groupCode, String assetKey) {
        this.groupCode = groupCode;
        this.assetKey = assetKey;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public String getAssetKey() {
        return assetKey;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LibraryAssetId that)) {
            return false;
        }
        return Objects.equals(groupCode, that.groupCode) && Objects.equals(assetKey, that.assetKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupCode, assetKey);
    }
}
