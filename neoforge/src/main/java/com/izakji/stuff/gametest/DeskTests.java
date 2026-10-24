package com.izakji.stuff.gametest;

import com.izakji.stuff.content.desk.Drawer;
import com.izakji.stuff.content.desk.DrawerMenu;
import com.izakji.stuff.content.desk.StudioDeskBlock;
import com.izakji.stuff.content.desk.StudioDeskBlockEntity;
import com.izakji.stuff.registry.ModContent;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests for desk inventories, menus, drops, and facing hit mapping.
 *
 * @author Izakji
 */
@GameTestHolder("stuff")
@PrefixGameTestTemplate(false)
public final class DeskTests {
    private static final BlockPos LOCAL_POS = new BlockPos(3, 1, 3);

    private static StudioDeskBlockEntity place(GameTestHelper helper, Direction facing) {
        BlockPos pos = helper.absolutePos(LOCAL_POS);
        BlockState state =
                ModContent.OAK_STUDIO_DESK.get().defaultBlockState().setValue(StudioDeskBlock.FACING, facing);
        helper.getLevel().setBlock(pos, state, Block.UPDATE_ALL);
        ModContent.OAK_STUDIO_DESK.get().setPlacedBy(helper.getLevel(), pos, state, null, ItemStack.EMPTY);
        return (StudioDeskBlockEntity) helper.getLevel().getBlockEntity(pos);
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void inventoriesAndViewers(GameTestHelper helper) {
        StudioDeskBlockEntity desk = place(helper, Direction.SOUTH);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(desk.getBlockPos()));
        for (Drawer drawer : Drawer.values()) {
            DrawerMenu menu = new DrawerMenu(1, player.getInventory(), desk, drawer);
            helper.assertTrue(menu.slots.size() == 36 + drawer.slotCount(), "Incorrect drawer slot count");
            helper.assertTrue(menu.stillValid(player), "Placed desk menu is invalid");
            helper.assertTrue(desk.getBlockState().getValue(drawer.openProperty()), "Selected drawer not open");
            for (Drawer other : Drawer.values()) {
                if (other != drawer) {
                    helper.assertTrue(!desk.getBlockState().getValue(other.openProperty()), "Unselected drawer opened");
                }
            }
            player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 7));
            menu.quickMoveStack(player, drawer.slotCount() + 27);
            helper.assertTrue(desk.inventory(drawer).getItem(0).getCount() == 7, "Shift-click failed");
            menu.removed(player);
            helper.assertTrue(!desk.getBlockState().getValue(drawer.openProperty()), "Drawer did not close");
        }

        var saved = desk.saveWithoutMetadata(helper.getLevel().registryAccess());
        StudioDeskBlockEntity restored = new StudioDeskBlockEntity(desk.getBlockPos(), desk.getBlockState());
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        for (Drawer drawer : Drawer.values()) {
            helper.assertTrue(
                    restored.inventory(drawer).getItem(0).getCount() == 7, "Inventory not persisted: " + drawer);
        }

        DrawerMenu first = new DrawerMenu(2, player.getInventory(), desk, Drawer.SMALL);
        DrawerMenu second = new DrawerMenu(3, player.getInventory(), desk, Drawer.SMALL);
        first.removed(player);
        helper.assertTrue(
                desk.getBlockState().getValue(Drawer.SMALL.openProperty()), "Closed with another viewer present");
        second.removed(player);
        helper.assertTrue(!desk.getBlockState().getValue(Drawer.SMALL.openProperty()), "Last viewer failed to close");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void survivalBreakRight(GameTestHelper helper) {
        breakDesk(helper, true, false);
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void survivalBreakLeft(GameTestHelper helper) {
        breakDesk(helper, false, false);
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void creativeBreakRight(GameTestHelper helper) {
        breakDesk(helper, true, true);
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void creativeBreakLeft(GameTestHelper helper) {
        breakDesk(helper, false, true);
    }

    private static void breakDesk(GameTestHelper helper, boolean rightHalf, boolean creative) {
        StudioDeskBlockEntity desk = place(helper, Direction.SOUTH);
        BlockPos owner = desk.getBlockPos();
        BlockPos other = StudioDeskBlock.other(owner, desk.getBlockState());
        desk.inventory(Drawer.LARGE).setItem(0, new ItemStack(Items.DIAMOND, 7));
        BlockPos target = rightHalf ? other : owner;
        if (creative) {
            ModContent.OAK_STUDIO_DESK
                    .get()
                    .playerWillDestroy(
                            helper.getLevel(),
                            target,
                            helper.getLevel().getBlockState(target),
                            helper.makeMockPlayer(GameType.CREATIVE));
        }
        helper.getLevel().destroyBlock(target, !creative);
        helper.assertTrue(
                helper.getLevel().getBlockState(owner).isAir()
                        && helper.getLevel().getBlockState(other).isAir(),
                "Desk half left behind");

        int deskDrops = 0;
        int diamondDrops = 0;
        for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds())) {
            if (item.getItem().is(ModContent.OAK_STUDIO_DESK_ITEM.get())) {
                deskDrops += item.getItem().getCount();
            }
            if (item.getItem().is(Items.DIAMOND)) {
                diamondDrops += item.getItem().getCount();
            }
        }
        helper.assertTrue(deskDrops == (creative ? 0 : 1), "Wrong desk drop count: " + deskDrops);
        helper.assertTrue(diamondDrops == 7, "Lost or duplicated contents: " + diamondDrops);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void facingAndDrawerHits(GameTestHelper helper) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (Vec3 expected : List.of(new Vec3(.4, .8, .94), new Vec3(.9, .8, .94))) {
                Vec3 world = switch (facing) {
                    case NORTH -> new Vec3(1 - expected.x, expected.y, 1 - expected.z);
                    case WEST -> new Vec3(1 - expected.z, expected.y, expected.x);
                    case EAST -> new Vec3(expected.z, expected.y, 1 - expected.x);
                    default -> expected;
                };
                helper.assertTrue(
                        StudioDeskBlock.localHit(world, facing).distanceTo(expected) < .0001,
                        "Incorrect hit rotation " + facing);
            }
        }
        helper.assertTrue(
                Drawer.atFrontFace(7, 13) == Drawer.SMALL
                        && Drawer.atFrontFace(7, 8) == Drawer.LARGE
                        && Drawer.atFrontFace(23, 13) == Drawer.LONG
                        && Drawer.atFrontFace(23, 5) == null,
                "Drawer hit regions wrong");
        helper.succeed();
    }
}
