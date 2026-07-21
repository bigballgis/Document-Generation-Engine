package com.bank.docgen.rendering;

import java.util.concurrent.Callable;

/**
 * Thread-scoped template owning group for image/seal resolve (ALGI-C5).
 * Production assembly sets this from the template {@code groupCode}.
 */
public final class AssetResolveGroupContext {

    private static final ThreadLocal<String> GROUP_CODE = new ThreadLocal<>();

    private AssetResolveGroupContext() {
    }

    public static String currentGroupCode() {
        return GROUP_CODE.get();
    }

    public static <T> T callWithGroup(String groupCode, Callable<T> action) {
        String previous = GROUP_CODE.get();
        GROUP_CODE.set(blankToNull(groupCode));
        try {
            return action.call();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Asset resolve group context failed", ex);
        } finally {
            if (previous == null) {
                GROUP_CODE.remove();
            } else {
                GROUP_CODE.set(previous);
            }
        }
    }

    public static void runWithGroup(String groupCode, Runnable action) {
        callWithGroup(groupCode, () -> {
            action.run();
            return null;
        });
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
