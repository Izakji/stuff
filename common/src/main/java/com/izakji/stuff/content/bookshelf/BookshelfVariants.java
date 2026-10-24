package com.izakji.stuff.content.bookshelf;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Stable book-color and size selection. The primitive thickness accessor avoids record allocation in layout code.
 *
 * @author Izakji
 */
public final class BookshelfVariants {
    public static final int VARIANT_COUNT = 16;
    private static final int SLOT_COLOR_SALT = 31;
    private static final int SHAPE_SALT = 0x9E3779B9;
    private static final int SIZE_STEPS = 1000;
    private static final float MIN_THICKNESS = 1.10F;
    private static final float THICKNESS_RANGE = 0.48F;
    private static final float MIN_HEIGHT = 2.40F;
    private static final float HEIGHT_RANGE = 1.50F;

    private BookshelfVariants() {}

    /**
     * Selects a legacy item-only color variant.
     *
     * @param stack the book stack
     * @return the zero-based variant index
     */
    public static int legacyVariant(ItemStack stack) {
        if (stack.is(Items.BOOK)) return 0;
        if (stack.is(Items.ENCHANTED_BOOK)) return 7;
        int hash = itemHash(stack);
        int colored = Math.floorMod(hash, 6) + 1;
        return stack.is(Items.WRITTEN_BOOK) || stack.is(Items.WRITABLE_BOOK)
                ? colored
                : Math.floorMod(hash, VARIANT_COUNT);
    }

    /**
     * Selects a slot-aware color variant so a plain shelf does not repeat one cover color.
     *
     * @param stack the book stack
     * @param slot the global shelf slot index
     * @return the zero-based variant index
     */
    public static int variantForSlot(ItemStack stack, int slot) {
        if (stack.is(Items.ENCHANTED_BOOK)) return 7;
        return Math.floorMod(itemHash(stack) + slot * SLOT_COLOR_SALT, VARIANT_COUNT);
    }

    /**
     * Returns the book thickness in model units.
     *
     * @param stack the book stack
     * @param slot the global shelf slot index
     * @return the thickness in model units
     */
    public static float thickness(ItemStack stack, int slot) {
        return thickness(shapeHash(stack, slot));
    }

    /**
     * Returns the book thickness and height in model units.
     *
     * @param stack the book stack
     * @param slot the global shelf slot index
     * @return the book dimensions
     */
    public static BookShape shapeForSlot(ItemStack stack, int slot) {
        int mixed = shapeHash(stack, slot);
        return new BookShape(thickness(mixed), height(mixed));
    }

    private static int itemHash(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).hashCode();
    }

    private static int shapeHash(ItemStack stack, int slot) {
        int mixed = itemHash(stack) ^ (slot * SHAPE_SALT);
        return mixed ^ (mixed >>> 16);
    }

    private static float thickness(int mixed) {
        return MIN_THICKNESS + Math.floorMod(mixed, SIZE_STEPS) / (SIZE_STEPS - 1F) * THICKNESS_RANGE;
    }

    private static float height(int mixed) {
        return MIN_HEIGHT + Math.floorMod(mixed >>> 10, SIZE_STEPS) / (SIZE_STEPS - 1F) * HEIGHT_RANGE;
    }

    /**
     * Rendered dimensions of one book.
     *
     * @param thickness the thickness in model units
     * @param height the height in model units
     */
    public record BookShape(float thickness, float height) {}
}
