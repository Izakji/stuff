package com.izakji.stuff.gametest;

import com.izakji.stuff.content.bookshelf.BookshelfBlock;
import com.izakji.stuff.content.bookshelf.BookshelfBlockEntity;
import com.izakji.stuff.content.bookshelf.BookshelfLayout;
import com.izakji.stuff.content.bookshelf.BookshelfSlots;
import com.izakji.stuff.content.bookshelf.BookshelfVariants;
import com.izakji.stuff.content.worktable.WorktableCategory;
import com.izakji.stuff.content.worktable.WorktableRecipe;
import com.izakji.stuff.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests for bookshelf storage, interaction, drops, and recipes.
 *
 * @author Izakji
 */
@GameTestHolder("stuff")
@PrefixGameTestTemplate(false)
public final class BookshelfTests {
    private static final BlockPos LOCAL = new BlockPos(3, 1, 3);

    private static BookshelfBlockEntity place(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(LOCAL);
        helper.getLevel().setBlock(pos, ModContent.OAK_STUDIO_BOOKSHELF.get().defaultBlockState(), 3);
        ModContent.OAK_STUDIO_BOOKSHELF
                .get()
                .setPlacedBy(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), null, ItemStack.EMPTY);
        return (BookshelfBlockEntity) helper.getLevel().getBlockEntity(pos);
    }

    private static ItemStack insert(BookshelfBlockEntity shelf, int shelfIndex, int slotIndex, ItemStack stack) {
        shelf.insertBook(BookshelfSlots.slotIndex(shelfIndex, slotIndex), stack, null);
        return stack;
    }

    @GameTest(template = "empty")
    public static void slotMappingMatchesShelfGeometry(GameTestHelper helper) {
        helper.assertTrue(
                BookshelfSlots.SHELF_COUNT == 5
                        && BookshelfSlots.SLOTS_PER_SHELF == 8
                        && BookshelfSlots.TOTAL_SLOTS == 40,
                "Wrong slot constants");
        for (int shelf = 0; shelf < 5; shelf++) {
            double surface = BookshelfSlots.shelfSurfaceY(shelf);
            helper.assertTrue(BookshelfSlots.shelfIndexAt(surface + .25) == shelf, "Wrong shelf mapping: " + shelf);
            helper.assertTrue(
                    BookshelfSlots.shelfIndexAt(surface - .01) == Math.max(0, shelf - 1),
                    "Wrong lower shelf boundary: " + shelf);
        }
        for (int slot = 0; slot < 8; slot++) {
            helper.assertTrue(
                    BookshelfSlots.slotIndexAt(BookshelfSlots.slotCenterX(slot)) == slot,
                    "Wrong slot mapping: " + slot);
        }
        helper.assertTrue(
                BookshelfSlots.shelfIndexAt(31) == 4 && BookshelfSlots.shelfIndexAt(0) == 0, "Shelf clamp failed");
        helper.assertTrue(
                BookshelfSlots.slotIndexAt(0) == 0 && BookshelfSlots.slotIndexAt(16) == 7, "Slot clamp failed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bookVariantsAreStable(GameTestHelper helper) {
        helper.assertTrue(BookshelfVariants.legacyVariant(new ItemStack(Items.BOOK)) == 0, "Plain book variant");
        helper.assertTrue(
                BookshelfVariants.legacyVariant(new ItemStack(Items.ENCHANTED_BOOK)) == 7, "Enchanted book variant");
        int first = BookshelfVariants.legacyVariant(new ItemStack(Items.WRITTEN_BOOK));
        int second = BookshelfVariants.legacyVariant(new ItemStack(Items.WRITTEN_BOOK));
        helper.assertTrue(first == second && first >= 1 && first <= 6, "Written book variant is unstable");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bookShapesAndSlotVariantsStayBounded(GameTestHelper helper) {
        float minThickness = 10, maxThickness = 0, minHeight = 10, maxHeight = 0;
        for (int slot = 0; slot < BookshelfSlots.TOTAL_SLOTS; slot++) {
            BookshelfVariants.BookShape shape = BookshelfVariants.shapeForSlot(new ItemStack(Items.BOOK), slot);
            helper.assertTrue(
                    shape.thickness() >= 1.10F && shape.thickness() <= 1.58F,
                    "Book thickness out of range: " + shape.thickness());
            helper.assertTrue(
                    shape.height() >= 2.40F && shape.height() <= 3.90F, "Book height out of range: " + shape.height());
            helper.assertTrue(shape.thickness() < BookshelfSlots.SLOT_WIDTH, "Book fills its slot with no gap");
            helper.assertTrue(
                    shape.equals(BookshelfVariants.shapeForSlot(new ItemStack(Items.BOOK), slot)),
                    "Book shape is not stable");
            minThickness = Math.min(minThickness, shape.thickness());
            maxThickness = Math.max(maxThickness, shape.thickness());
            minHeight = Math.min(minHeight, shape.height());
            maxHeight = Math.max(maxHeight, shape.height());
            int variant = BookshelfVariants.variantForSlot(new ItemStack(Items.BOOK), slot);
            helper.assertTrue(
                    variant >= 0 && variant < BookshelfVariants.VARIANT_COUNT, "Book variant out of range: " + variant);
            helper.assertTrue(
                    BookshelfVariants.variantForSlot(new ItemStack(Items.ENCHANTED_BOOK), slot) == 7,
                    "Enchanted book variant changed");
        }
        helper.assertTrue(maxThickness - minThickness > .35F, "Book thickness varies too little");
        helper.assertTrue(maxHeight - minHeight > 1.2F, "Book height varies too little");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packedBooksTouchWithoutLeaning(GameTestHelper helper) {
        BookshelfBlockEntity shelf = place(helper);
        for (int slot = 0; slot < 4; slot++) {
            insert(shelf, 0, slot, new ItemStack(Items.BOOK));
        }
        float[] centers = BookshelfLayout.packedSlotCenters(shelf, 0);
        for (int slot = 1; slot < 4; slot++) {
            int index = BookshelfSlots.slotIndex(0, slot);
            int previous = BookshelfSlots.slotIndex(0, slot - 1);
            float half =
                    BookshelfVariants.shapeForSlot(shelf.bookAt(index), index).thickness() / 2;
            float previousHalf = BookshelfVariants.shapeForSlot(shelf.bookAt(previous), previous)
                            .thickness()
                    / 2;
            float gap = centers[slot] - centers[slot - 1] - half - previousHalf;
            helper.assertTrue(Math.abs(gap - BookshelfLayout.BOOK_GAP) < .03F, "Packed books do not touch: gap=" + gap);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void insertRemoveAndPersistence(GameTestHelper helper) {
        BookshelfBlockEntity shelf = place(helper);
        int index = BookshelfSlots.slotIndex(2, 3);
        ItemStack book = new ItemStack(Items.BOOK, 2);
        helper.assertTrue(shelf.insertBook(index, book, null), "Valid book rejected");
        helper.assertTrue(
                shelf.bookAt(index).is(Items.BOOK) && shelf.bookAt(index).getCount() == 1, "Wrong stored stack");
        helper.assertTrue(book.getCount() == 1, "Insert did not consume exactly one");
        helper.assertTrue(!shelf.insertBook(index, new ItemStack(Items.BOOK), null), "Occupied slot accepted");
        helper.assertTrue(
                !shelf.insertBook(BookshelfSlots.slotIndex(0, 0), new ItemStack(Items.DIAMOND), null),
                "Non-book accepted");
        ItemStack removed = shelf.removeBook(index, null);
        helper.assertTrue(
                removed.is(Items.BOOK)
                        && removed.getCount() == 1
                        && shelf.bookAt(index).isEmpty(),
                "Removal failed");
        helper.assertTrue(shelf.insertBook(index, new ItemStack(Items.BOOK), null), "Reinsert failed");
        var saved = shelf.saveWithoutMetadata(helper.getLevel().registryAccess());
        var restored = new BookshelfBlockEntity(shelf.getBlockPos(), shelf.getBlockState());
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(restored.bookAt(index).is(Items.BOOK), "Persistence lost book");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lowerHalfOwnsSingleEntity(GameTestHelper helper) {
        BookshelfBlockEntity shelf = place(helper);
        helper.assertTrue(
                helper.getLevel().getBlockEntity(shelf.getBlockPos().above()) == null, "Upper half has a block entity");
        helper.assertTrue(
                ModContent.OAK_STUDIO_BOOKSHELF
                                .get()
                                .getTicker(helper.getLevel(), shelf.getBlockState(), ModContent.BOOKSHELF_ENTITY.get())
                        == null,
                "Bookshelf installed a ticker");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void breakingUpperDropsBooksOnce(GameTestHelper helper) {
        BookshelfBlockEntity shelf = place(helper);
        insert(shelf, 1, 1, new ItemStack(Items.BOOK));
        insert(shelf, 3, 6, new ItemStack(Items.WRITABLE_BOOK));
        BlockPos lower = shelf.getBlockPos();
        helper.getLevel().destroyBlock(lower.above(), true);
        helper.assertTrue(
                helper.getLevel().getBlockState(lower).isAir()
                        && helper.getLevel().getBlockState(lower.above()).isAir(),
                "Breaking upper left a half behind");
        assertDrops(helper, lower, 1, 2);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void breakingLowerDropsBooksOnce(GameTestHelper helper) {
        BookshelfBlockEntity shelf = place(helper);
        insert(shelf, 0, 0, new ItemStack(Items.BOOK));
        insert(shelf, 4, 7, new ItemStack(Items.WRITTEN_BOOK));
        BlockPos lower = shelf.getBlockPos();
        helper.getLevel().destroyBlock(lower, true);
        helper.assertTrue(
                helper.getLevel().getBlockState(lower).isAir()
                        && helper.getLevel().getBlockState(lower.above()).isAir(),
                "Breaking lower left a half behind");
        assertDrops(helper, lower, 1, 2);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void creativeBreakDropsBooksOnly(GameTestHelper helper) {
        BookshelfBlockEntity shelf = place(helper);
        insert(shelf, 2, 2, new ItemStack(Items.BOOK));
        BlockPos lower = shelf.getBlockPos();
        var creative = helper.makeMockPlayer(GameType.CREATIVE);
        ModContent.OAK_STUDIO_BOOKSHELF
                .get()
                .playerWillDestroy(
                        helper.getLevel(), lower.above(), helper.getLevel().getBlockState(lower.above()), creative);
        helper.getLevel().destroyBlock(lower.above(), false);
        helper.assertTrue(helper.getLevel().getBlockState(lower).isAir(), "Creative break left lower half");
        int furniture = 0, books = 0;
        for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(lower).inflate(3))) {
            if (item.getItem().is(ModContent.OAK_STUDIO_BOOKSHELF_ITEM.get()))
                furniture += item.getItem().getCount();
            if (item.getItem().is(ItemTags.BOOKSHELF_BOOKS))
                books += item.getItem().getCount();
        }
        helper.assertTrue(
                furniture == 0 && books == 1,
                "Creative break should drop books but not the furniture: furniture=" + furniture + " books=" + books);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void rightClickInsertsAndRemovesAtClickedSlot(GameTestHelper helper) {
        BookshelfBlockEntity shelf = place(helper);
        BlockPos pos = shelf.getBlockPos();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(pos.getX() + .5, pos.getY() + .5, pos.getZ() + 1.5);
        ItemStack held = new ItemStack(Items.BOOK, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        var state = helper.getLevel().getBlockState(pos);
        var hit = new BlockHitResult(
                new Vec3(
                        pos.getX() + BookshelfSlots.slotCenterX(5) / 16.0,
                        pos.getY() + 11.5 / 16.0,
                        pos.getZ() + 15.5 / 16.0),
                Direction.SOUTH,
                pos,
                false);
        helper.assertTrue(
                state.useItemOn(held, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit)
                        .consumesAction(),
                "Right-click book insertion failed");
        int index = BookshelfSlots.slotIndex(1, 5);
        helper.assertTrue(shelf.bookAt(index).is(Items.BOOK) && held.getCount() == 1, "Wrong inserted slot or count");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(
                state.useWithoutItem(helper.getLevel(), player, hit).consumesAction(), "Right-click removal failed");
        helper.assertTrue(
                shelf.bookAt(index).isEmpty() && player.getInventory().countItem(Items.BOOK) == 1,
                "Removal lost or duplicated the book");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void localHitFollowsFacing(GameTestHelper helper) {
        Vec3 point = new Vec3(.25, .5, .75);
        helper.assertTrue(BookshelfBlock.localHit(point, Direction.SOUTH).distanceTo(point) < .0001, "SOUTH local hit");
        helper.assertTrue(
                BookshelfBlock.localHit(point, Direction.NORTH).distanceTo(new Vec3(.75, .5, .25)) < .0001,
                "NORTH local hit");
        helper.assertTrue(
                BookshelfBlock.localHit(point, Direction.WEST).distanceTo(new Vec3(.75, .5, .75)) < .0001,
                "WEST local hit");
        helper.assertTrue(
                BookshelfBlock.localHit(point, Direction.EAST).distanceTo(new Vec3(.25, .5, .25)) < .0001,
                "EAST local hit");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void placementRequiresUpperSpace(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(LOCAL);
        helper.getLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(pos.above(), Blocks.STONE.defaultBlockState(), 3);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var stack = new ItemStack(ModContent.OAK_STUDIO_BOOKSHELF_ITEM.get());
        var use = new UseOnContext(
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                stack,
                new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false));
        helper.assertTrue(
                ModContent.OAK_STUDIO_BOOKSHELF.get().getStateForPlacement(new BlockPlaceContext(use)) == null,
                "Occupied upper cell accepted");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void worktableRecipeAndCatalogue(GameTestHelper helper) {
        var holder = helper.getLevel()
                .getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath("stuff", "oak_studio_bookshelf"))
                .orElseThrow();
        helper.assertTrue(holder.value() instanceof WorktableRecipe, "Bookshelf was not added to the Worktable");
        WorktableRecipe recipe = (WorktableRecipe) holder.value();
        helper.assertTrue(recipe.category() == WorktableCategory.STORAGE, "Wrong Worktable category");
        helper.assertTrue(recipe.result().is(ModContent.OAK_STUDIO_BOOKSHELF_ITEM.get()), "Wrong Worktable result");
        helper.assertTrue(recipe.materials().size() == 2, "Wrong material count");
        helper.assertTrue(
                recipe.materials().get(0).count() == 8
                        && recipe.materials().get(0).ingredient().test(new ItemStack(Items.OAK_PLANKS)),
                "Wrong planks");
        helper.assertTrue(
                recipe.materials().get(1).count() == 3
                        && recipe.materials().get(1).ingredient().test(new ItemStack(Items.BOOK)),
                "Wrong books");
        helper.succeed();
    }

    private static void assertDrops(GameTestHelper helper, BlockPos pos, int tables, int books) {
        int foundTables = 0, foundBooks = 0;
        for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3))) {
            ItemStack stack = item.getItem();
            if (stack.is(ModContent.OAK_STUDIO_BOOKSHELF_ITEM.get())) foundTables += stack.getCount();
            if (stack.is(ItemTags.BOOKSHELF_BOOKS)) foundBooks += stack.getCount();
        }
        helper.assertTrue(
                foundTables == tables && foundBooks == books,
                "Wrong drops: tables=" + foundTables + " books=" + foundBooks);
    }
}
