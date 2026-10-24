package com.izakji.stuff.gametest;

import com.izakji.stuff.content.worktable.WorktableMenu;
import com.izakji.stuff.content.worktable.WorktableRecipe;
import com.izakji.stuff.registry.ModContent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests for worktable crafting, catalogue selection, and lifecycle.
 *
 * @author Izakji
 */
@GameTestHolder("stuff")
@PrefixGameTestTemplate(false)
public final class WorktableTests {
    private static final BlockPos LOCAL = new BlockPos(3, 1, 3);

    private static Player player(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(LOCAL);
        helper.getLevel().setBlock(pos, ModContent.WORKTABLE.get().defaultBlockState(), 3);
        ModContent.WORKTABLE
                .get()
                .setPlacedBy(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), null, ItemStack.EMPTY);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
        return player;
    }

    private static WorktableMenu menu(GameTestHelper helper, Player player) {
        return new WorktableMenu(1, player.getInventory(), helper.absolutePos(LOCAL));
    }

    private static int recipe(WorktableMenu menu, String path) {
        for (int i = 0; i < menu.catalogue().size(); i++)
            if (menu.catalogue().get(i).id().getPath().equals(path)) return i + 1;
        throw new AssertionError("Recipe missing: " + path);
    }

    @GameTest(template = "empty")
    public static void exactConsumptionAndNoDoubleCraft(GameTestHelper h) {
        Player player = player(h);
        WorktableMenu menu = menu(h, player);
        menu.clickMenuButton(player, recipe(menu, "oak_wall_clock"));
        menu.getSlot(0).set(new ItemStack(Items.OAK_LOG, 5));
        menu.getSlot(1).set(new ItemStack(Items.IRON_INGOT, 9));
        h.assertTrue(menu.clickMenuButton(player, WorktableMenu.CRAFT_BUTTON), "Craft rejected valid inputs");
        h.assertTrue(
                menu.getSlot(WorktableMenu.OUTPUT).getItem().is(ModContent.OAK_WALL_CLOCK_ITEM.get()), "Wrong output");
        h.assertTrue(player.getInventory().countItem(Items.OAK_LOG) == 4, "Log leftovers lost/duplicated");
        h.assertTrue(player.getInventory().countItem(Items.IRON_INGOT) == 7, "Iron leftovers lost/duplicated");
        h.assertTrue(!menu.clickMenuButton(player, WorktableMenu.CRAFT_BUTTON), "Repeated craft duplicated output");
        menu.quickMoveStack(player, WorktableMenu.OUTPUT);
        h.assertTrue(
                player.getInventory().countItem(ModContent.OAK_WALL_CLOCK_ITEM.get()) == 1,
                "Output shift-click failed");
        menu.removed(player);
        h.assertTrue(
                player.getInventory().countItem(ModContent.OAK_WALL_CLOCK_ITEM.get()) == 1,
                "Closing duplicated output");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void rejectWrongMissingAndRemoteInputs(GameTestHelper h) {
        Player player = player(h);
        WorktableMenu menu = menu(h, player);
        menu.clickMenuButton(player, recipe(menu, "oak_studio_desk"));
        menu.getSlot(0).set(new ItemStack(Items.BIRCH_PLANKS, 7));
        h.assertTrue(!menu.clickMenuButton(player, 0), "Wrong wood accepted");
        menu.getSlot(0).set(new ItemStack(Items.OAK_PLANKS, 6));
        h.assertTrue(!menu.clickMenuButton(player, 0), "Insufficient count accepted");
        h.assertTrue(menu.getSlot(0).getItem().getCount() == 6, "Failed craft consumed input");
        h.assertTrue(
                !menu.clickMenuButton(player, Integer.MAX_VALUE) && !menu.clickMenuButton(player, -1),
                "Invalid button accepted");
        menu.getSlot(0).set(new ItemStack(Items.OAK_PLANKS, 7));
        player.setPos(player.getX() + 20, player.getY(), player.getZ());
        h.assertTrue(!menu.clickMenuButton(player, 0), "Remote crafting allowed");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void switchingClosingAndIndependentSessions(GameTestHelper h) {
        Player first = player(h), second = player(h);
        WorktableMenu a = menu(h, first), b = menu(h, second);
        a.clickMenuButton(first, recipe(a, "oak_studio_chair"));
        a.getSlot(0).set(new ItemStack(Items.OAK_PLANKS, 12));
        b.clickMenuButton(second, recipe(b, "oak_studio_chair"));
        h.assertTrue(!b.canCraft(), "Players shared their ingredient inventory");
        a.clickMenuButton(first, recipe(a, "oak_wall_clock"));
        h.assertTrue(first.getInventory().countItem(Items.OAK_PLANKS) == 12, "Selection switch lost inputs");
        a.getSlot(0).set(new ItemStack(Items.OAK_LOG));
        a.getSlot(1).set(new ItemStack(Items.IRON_INGOT, 2));
        a.clickMenuButton(first, 0);
        h.getLevel().setBlock(h.absolutePos(LOCAL), Blocks.AIR.defaultBlockState(), 3);
        h.assertTrue(!a.stillValid(first), "Broken table left menu valid");
        a.removed(first);
        h.assertTrue(
                first.getInventory().countItem(ModContent.OAK_WALL_CLOCK_ITEM.get()) == 1, "Closing lost paid output");
        b.removed(second);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fullInventoryReturnsOverflowAsDrops(GameTestHelper h) {
        Player player = player(h);
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
        WorktableMenu menu = menu(h, player);
        menu.clickMenuButton(player, recipe(menu, "oak_wall_clock"));
        menu.getSlot(0).set(new ItemStack(Items.OAK_LOG, 3));
        menu.getSlot(1).set(new ItemStack(Items.IRON_INGOT, 5));
        h.assertTrue(menu.clickMenuButton(player, 0), "Full player inventory blocked a valid craft");
        menu.removed(player);
        var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(LOCAL)).inflate(4));
        int logs = 0, iron = 0, clocks = 0;
        for (var drop : drops) {
            ItemStack stack = drop.getItem();
            if (stack.is(Items.OAK_LOG)) logs += stack.getCount();
            if (stack.is(Items.IRON_INGOT)) iron += stack.getCount();
            if (stack.is(ModContent.OAK_WALL_CLOCK_ITEM.get())) clocks += stack.getCount();
        }
        h.assertTrue(logs == 2 && iron == 3 && clocks == 1, "Overflow lost or duplicated items");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void upperHalfBreaksWholeTableOnce(GameTestHelper h) {
        player(h);
        BlockPos pos = h.absolutePos(LOCAL);
        h.assertTrue(
                h.getLevel().getBlockState(pos.above()).is(ModContent.WORKTABLE.get()), "Upper space was not reserved");
        h.getLevel().destroyBlock(pos.above(), true);
        h.assertTrue(
                h.getLevel().getBlockState(pos).isAir()
                        && h.getLevel().getBlockState(pos.above()).isAir(),
                "Table half left behind");
        int count = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream()
                .filter(item -> item.getItem().is(ModContent.WORKTABLE_ITEM.get()))
                .mapToInt(item -> item.getItem().getCount())
                .sum();
        h.assertTrue(count == 1, "Upper break must drop exactly one worktable");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void lowerHalfBreaksWholeTableOnce(GameTestHelper h) {
        player(h);
        BlockPos pos = h.absolutePos(LOCAL);
        h.getLevel().destroyBlock(pos, true);
        h.assertTrue(
                h.getLevel().getBlockState(pos).isAir()
                        && h.getLevel().getBlockState(pos.above()).isAir(),
                "Lower half break left the upper half behind");
        int count = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream()
                .filter(item -> item.getItem().is(ModContent.WORKTABLE_ITEM.get()))
                .mapToInt(item -> item.getItem().getCount())
                .sum();
        h.assertTrue(count == 1, "Lower break must drop exactly one worktable");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeLowerBreakDoesNotDropTable(GameTestHelper h) {
        player(h);
        BlockPos pos = h.absolutePos(LOCAL);
        h.getLevel().destroyBlock(pos, false);
        h.assertTrue(
                h.getLevel().getBlockState(pos).isAir()
                        && h.getLevel().getBlockState(pos.above()).isAir(),
                "Creative lower break left the upper half behind");
        h.assertTrue(
                h.getLevel()
                        .getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2))
                        .isEmpty(),
                "Creative lower break dropped an item");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeUpperBreakDoesNotDropTable(GameTestHelper h) {
        player(h);
        BlockPos pos = h.absolutePos(LOCAL);
        var creative = h.makeMockPlayer(GameType.CREATIVE);
        ModContent.WORKTABLE
                .get()
                .playerWillDestroy(h.getLevel(), pos.above(), h.getLevel().getBlockState(pos.above()), creative);
        h.getLevel().destroyBlock(pos.above(), false);
        h.assertTrue(h.getLevel().getBlockState(pos).isAir(), "Creative break left lower half");
        h.assertTrue(
                h.getLevel()
                        .getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2))
                        .isEmpty(),
                "Creative break dropped an item");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void occupiedUpperSpaceRejectsPlacement(GameTestHelper h) {
        BlockPos pos = h.absolutePos(LOCAL);
        h.getLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        h.getLevel().setBlock(pos.above(), Blocks.STONE.defaultBlockState(), 3);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var stack = new ItemStack(ModContent.WORKTABLE_ITEM.get());
        var use = new UseOnContext(
                h.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                stack,
                new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false));
        var context = new BlockPlaceContext(use);
        h.assertTrue(
                ModContent.WORKTABLE.get().getStateForPlacement(context) == null, "Shelf may overlap a solid block");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void bootstrapRecipeAndCatalogue(GameTestHelper h) {
        var manager = h.getLevel().getRecipeManager();
        var holder = manager.byKey(ResourceLocation.fromNamespaceAndPath("stuff", "worktable"))
                .orElseThrow();
        h.assertTrue(holder.value() instanceof ShapedRecipe, "Worktable bootstrap recipe missing");
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++)
            grid.add(
                    i < 3
                            ? new ItemStack(Items.OAK_LOG)
                            : i == 4 || i == 7 ? ItemStack.EMPTY : new ItemStack(Items.IRON_INGOT));
        var recipe = (ShapedRecipe) holder.value();
        h.assertTrue(recipe.matches(CraftingInput.of(3, 3, grid), h.getLevel()), "Bootstrap shape incorrect");
        grid.set(0, new ItemStack(Items.OAK_PLANKS));
        h.assertTrue(
                !recipe.matches(CraftingInput.of(3, 3, grid), h.getLevel()),
                "Bootstrap accepts planks instead of logs");
        for (String id : List.of(
                "oak_studio_desk", "oak_studio_chair", "oak_wall_clock", "oak_record_player", "oak_studio_bookshelf"))
            h.assertTrue(
                    manager.byKey(ResourceLocation.fromNamespaceAndPath("stuff", id))
                                    .orElseThrow()
                                    .value()
                            instanceof WorktableRecipe,
                    "Furniture has not migrated: " + id);
        h.succeed();
    }
}
