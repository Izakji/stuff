package com.izakji.stuff.content.bookshelf;

import net.minecraft.util.Mth;

/**
 * Shared slot geometry for the authored model and the interaction hit mapping.
 *
 * @author Izakji
 */
public final class BookshelfSlots {
    public static final int SHELF_COUNT = 5;
    public static final int SLOTS_PER_SHELF = 8;
    public static final int TOTAL_SLOTS = SHELF_COUNT * SLOTS_PER_SHELF;
    public static final double INTERIOR_MIN_X = 1.5;
    public static final double INTERIOR_MAX_X = 14.5;
    public static final double SLOT_WIDTH = (INTERIOR_MAX_X - INTERIOR_MIN_X) / SLOTS_PER_SHELF;
    public static final double MODEL_UNITS_PER_BLOCK = 16;
    public static final double FIRST_SHELF_SURFACE_Y = 5.5;
    public static final double SHELF_SPACING = 5.0;

    private BookshelfSlots() {}

    public static int slotIndex(int shelfIndex, int slotInShelf) {
        if (shelfIndex < 0 || shelfIndex >= SHELF_COUNT || slotInShelf < 0 || slotInShelf >= SLOTS_PER_SHELF) {
            throw new IllegalArgumentException("Bookshelf slot out of range");
        }
        return shelfIndex * SLOTS_PER_SHELF + slotInShelf;
    }

    public static double shelfSurfaceY(int shelfIndex) {
        return FIRST_SHELF_SURFACE_Y + shelfIndex * SHELF_SPACING;
    }

    public static double slotCenterX(int slotIndex) {
        return INTERIOR_MIN_X + (slotIndex + .5) * SLOT_WIDTH;
    }

    public static int shelfIndexAt(double localY) {
        return Mth.clamp((int) Math.floor((localY - FIRST_SHELF_SURFACE_Y) / SHELF_SPACING), 0, SHELF_COUNT - 1);
    }

    public static int slotIndexAt(double localX) {
        return Mth.clamp((int) Math.floor((localX - INTERIOR_MIN_X) / SLOT_WIDTH), 0, SLOTS_PER_SHELF - 1);
    }
}
