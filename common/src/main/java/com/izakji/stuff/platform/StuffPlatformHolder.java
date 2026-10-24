package com.izakji.stuff.platform;

import java.util.Objects;

/**
 * Installed by the loader module before common content is used.
 *
 * @author Izakji
 */
public final class StuffPlatformHolder {
    private static StuffPlatform platform;

    private StuffPlatformHolder() {}

    public static void set(StuffPlatform value) {
        if (platform != null) {
            throw new IllegalStateException("Stuff platform already installed");
        }
        platform = Objects.requireNonNull(value, "platform");
    }

    public static StuffPlatform get() {
        if (platform == null) {
            throw new IllegalStateException("Stuff platform is not installed");
        }
        return platform;
    }
}
