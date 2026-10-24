package com.izakji.stuff.registry;

import java.util.Objects;

/**
 * Installed once by the loader registry; common content reads through this holder.
 *
 * @author Izakji
 */
public final class StuffContentHolder {
    private static StuffContent content;

    private StuffContentHolder() {}

    public static void set(StuffContent value) {
        if (content != null) {
            throw new IllegalStateException("Stuff content registry already installed");
        }
        content = Objects.requireNonNull(value, "content");
    }

    public static StuffContent get() {
        if (content == null) {
            throw new IllegalStateException("Stuff content registry is not installed");
        }
        return content;
    }
}
