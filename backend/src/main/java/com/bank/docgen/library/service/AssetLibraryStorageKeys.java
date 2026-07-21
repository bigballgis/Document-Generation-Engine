package com.bank.docgen.library.service;

import java.util.ArrayList;
import java.util.List;

/** Physical MinIO key helpers for group-namespaced asset library objects (ALGI-C4). */
public final class AssetLibraryStorageKeys {

    private AssetLibraryStorageKeys() {
    }

    public static String namespacedKey(String groupCode, String assetKey) {
        return groupCode + "/" + assetKey;
    }

    public static List<String> namespacedResolvableKeys(String groupCode, String assetKey) {
        String base = namespacedKey(groupCode, assetKey);
        List<String> keys = new ArrayList<>();
        keys.add(base);
        if (!assetKey.contains(".")) {
            keys.add(base + ".png");
            keys.add(base + ".jpg");
            keys.add(base + ".jpeg");
        }
        return keys;
    }

    public static List<String> bareResolvableKeys(String assetKey) {
        List<String> keys = new ArrayList<>();
        keys.add(assetKey);
        if (!assetKey.contains(".")) {
            keys.add(assetKey + ".png");
            keys.add(assetKey + ".jpg");
            keys.add(assetKey + ".jpeg");
        }
        return keys;
    }
}
