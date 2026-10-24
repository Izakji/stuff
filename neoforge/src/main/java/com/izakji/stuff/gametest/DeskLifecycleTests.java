package com.izakji.stuff.gametest;

import com.izakji.stuff.content.desk.Drawer;
import com.izakji.stuff.content.desk.StudioDeskBlock;
import com.izakji.stuff.content.desk.StudioDeskBlockEntity;
import com.izakji.stuff.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests for desk viewer recovery and stale open-state cleanup.
 *
 * @author Izakji
 */
@GameTestHolder("stuff")
@PrefixGameTestTemplate(false)
public final class DeskLifecycleTests {
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void staleOpenStateRecoversWithoutTicker(GameTestHelper helper) {
        var level = helper.getLevel();
        var block = ModContent.OAK_STUDIO_DESK.get();
        BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 3));
        var state = block.defaultBlockState().setValue(Drawer.LONG.openProperty(), true);
        level.setBlock(pos, state, 3);
        block.setPlacedBy(level, pos, state, null, ItemStack.EMPTY);
        var desk = (StudioDeskBlockEntity) level.getBlockEntity(pos);
        // Simulate the load callback for a desk saved while a viewer had a menu open.
        desk.recoverOnLoad();
        helper.assertTrue(
                block.getTicker(level, state, ModContent.DESK_ENTITY.get()) == null,
                "Storage-only furniture must not have a permanent ticker");
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(
                    !level.getBlockState(pos).getValue(Drawer.LONG.openProperty()),
                    "Saved open drawer did not recover");
            BlockPos partner = StudioDeskBlock.other(pos, state);
            helper.assertTrue(
                    !level.getBlockState(partner).getValue(Drawer.LONG.openProperty()),
                    "Partner kept a stale open state");
            helper.assertTrue(
                    !level.getBlockTicks().hasScheduledTick(pos, block),
                    "Idle desk is still scheduling recurring work");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void abandonedViewerRecoversWithoutCloseCallback(GameTestHelper helper) {
        var level = helper.getLevel();
        var block = ModContent.OAK_STUDIO_DESK.get();
        BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 3));
        var state = block.defaultBlockState();
        level.setBlock(pos, state, 3);
        block.setPlacedBy(level, pos, state, null, ItemStack.EMPTY);
        var desk = (StudioDeskBlockEntity) level.getBlockEntity(pos);
        helper.runAfterDelay(3, () -> {
            // Simulate a viewer whose menu disappeared without calling removed.
            desk.startViewing(Drawer.SMALL);
            helper.assertTrue(desk.getBlockState().getValue(Drawer.SMALL.openProperty()), "Drawer never opened");
        });
        helper.runAfterDelay(35, () -> {
            helper.assertTrue(
                    !desk.getBlockState().getValue(Drawer.SMALL.openProperty()),
                    "Abandoned viewer left the drawer open");
            helper.assertTrue(
                    !level.getBlockTicks().hasScheduledTick(pos, block), "Abandoned viewer kept scheduling work");
            helper.succeed();
        });
    }
}
