package com.izakji.stuff.content.desk;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/**
 * The three desk drawer types and their inventory, state, and front-face geometry.
 *
 * @author Izakji
 */
public enum Drawer {
    SMALL(4, 2, "small", new FrontFace(3.25, 10.75, 11.65, 14.35), 1),
    LONG(6, 3, "long", new FrontFace(12.25, 28.75, 11.9, 14.35), 2),
    LARGE(9, 3, "large", new FrontFace(3.25, 10.75, 5.35, 10.85), 4);

    private final int slotCount;
    private final int columnCount;
    private final String serializedName;
    private final FrontFace frontFace;
    private final int stateMask;
    private final BooleanProperty openProperty;

    Drawer(int slotCount, int columnCount, String serializedName, FrontFace frontFace, int stateMask) {
        this.slotCount = slotCount;
        this.columnCount = columnCount;
        this.serializedName = serializedName;
        this.frontFace = frontFace;
        this.stateMask = stateMask;
        this.openProperty = BooleanProperty.create(serializedName + "_open");
    }

    public int slotCount() {
        return slotCount;
    }

    public int columnCount() {
        return columnCount;
    }

    public String serializedName() {
        return serializedName;
    }

    public FrontFace frontFace() {
        return frontFace;
    }

    public BooleanProperty openProperty() {
        return openProperty;
    }

    public int stateMask() {
        return stateMask;
    }

    public Component titleComponent() {
        return Component.translatable("container.stuff.desk." + serializedName);
    }

    /**
     * Finds the drawer whose front face contains the given model-space point.
     *
     * @param x the X coordinate in the full desk's unrotated 32 by 16 model space
     * @param y the Y coordinate in the full desk's unrotated 32 by 16 model space
     * @return the matching drawer, or {@code null} when the point is outside every front face
     */
    public static @Nullable Drawer atFrontFace(double x, double y) {
        for (Drawer drawer : values()) {
            if (drawer.frontFace.contains(x, y)) return drawer;
        }
        return null;
    }

    /**
     * Axis-aligned drawer front bounds in full-desk model space.
     *
     * @param minX the minimum X coordinate
     * @param maxX the maximum X coordinate
     * @param minY the minimum Y coordinate
     * @param maxY the maximum Y coordinate
     */
    public record FrontFace(double minX, double maxX, double minY, double maxY) {
        public boolean contains(double x, double y) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY;
        }
    }
}
