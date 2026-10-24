package com.izakji.stuff.content.bookshelf;

import java.util.Arrays;

/**
 * Shared server/client layout: contiguous occupied slots are packed into a touching, centered run.
 *
 * @author Izakji
 */
public final class BookshelfLayout {
    public static final double BOOK_GAP = .02;

    private BookshelfLayout() {}

    /**
     * Returns one X center per shelf slot in model units.
     *
     * <p>An entry is {@link Float#NaN} when that slot is empty, so callers must check before using
     * the value.
     *
     * @param shelf the bookshelf block entity
     * @param shelfIndex the zero-based shelf index
     * @return a new array of slot centers
     */
    public static float[] packedSlotCenters(BookshelfBlockEntity shelf, int shelfIndex) {
        float[] centers = new float[BookshelfSlots.SLOTS_PER_SHELF];
        fillPackedSlotCenters(shelf, shelfIndex, centers);
        return centers;
    }

    /**
     * Fills a caller-owned buffer to avoid a per-shelf allocation in the block-entity renderer.
     *
     * @param shelf the bookshelf block entity
     * @param shelfIndex the zero-based shelf index
     * @param centers the buffer to fill; unused entries become {@link Float#NaN}
     * @throws IllegalArgumentException if {@code centers} is too short
     */
    public static void fillPackedSlotCenters(BookshelfBlockEntity shelf, int shelfIndex, float[] centers) {
        if (centers.length < BookshelfSlots.SLOTS_PER_SHELF) {
            throw new IllegalArgumentException("Center buffer must hold " + BookshelfSlots.SLOTS_PER_SHELF + " slots");
        }
        Arrays.fill(centers, 0, BookshelfSlots.SLOTS_PER_SHELF, Float.NaN);
        int slotIndex = 0;
        while (slotIndex < BookshelfSlots.SLOTS_PER_SHELF) {
            int firstBookIndex = BookshelfSlots.slotIndex(shelfIndex, slotIndex);
            if (shelf.bookAt(firstBookIndex).isEmpty()) {
                slotIndex++;
                continue;
            }

            int runStartSlot = slotIndex;
            float widthSum = 0;
            int bookCount = 0;
            while (slotIndex < BookshelfSlots.SLOTS_PER_SHELF) {
                int bookIndex = BookshelfSlots.slotIndex(shelfIndex, slotIndex);
                if (shelf.bookAt(bookIndex).isEmpty()) break;
                widthSum += BookshelfVariants.thickness(shelf.bookAt(bookIndex), bookIndex);
                bookCount++;
                slotIndex++;
            }
            int runEndSlot = slotIndex - 1;
            float runWidth = widthSum + (float) BOOK_GAP * (bookCount - 1);
            float spanStart = (float) BookshelfSlots.slotCenterX(runStartSlot) - (float) BookshelfSlots.SLOT_WIDTH / 2;
            float spanEnd = (float) BookshelfSlots.slotCenterX(runEndSlot) + (float) BookshelfSlots.SLOT_WIDTH / 2;
            float cursor = (spanStart + spanEnd) / 2 - runWidth / 2;
            for (int slot = runStartSlot; slot <= runEndSlot; slot++) {
                int bookIndex = BookshelfSlots.slotIndex(shelfIndex, slot);
                float thickness = BookshelfVariants.thickness(shelf.bookAt(bookIndex), bookIndex);
                centers[slot] = cursor + thickness / 2;
                cursor += thickness + (float) BOOK_GAP;
            }
        }
    }

    /**
     * Finds the occupied visual slot under a local X coordinate.
     *
     * @param shelf the bookshelf block entity
     * @param shelfIndex the zero-based shelf index
     * @param localX the X coordinate in model units
     * @return the slot index, or {@code -1} when the coordinate is empty shelf space
     */
    public static int visualSlotAt(BookshelfBlockEntity shelf, int shelfIndex, double localX) {
        float[] centers = packedSlotCenters(shelf, shelfIndex);
        for (int slot = 0; slot < BookshelfSlots.SLOTS_PER_SHELF; slot++) {
            if (Float.isNaN(centers[slot])) continue;
            int bookIndex = BookshelfSlots.slotIndex(shelfIndex, slot);
            float halfThickness = BookshelfVariants.thickness(shelf.bookAt(bookIndex), bookIndex) / 2;
            if (localX >= centers[slot] - halfThickness && localX <= centers[slot] + halfThickness) return slot;
        }
        return -1;
    }
}
