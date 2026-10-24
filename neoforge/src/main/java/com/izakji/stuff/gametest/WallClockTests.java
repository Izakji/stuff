package com.izakji.stuff.gametest;

import com.izakji.stuff.content.clock.ClockTime;
import com.izakji.stuff.content.clock.WallClockBlock;
import com.izakji.stuff.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests for wall-clock placement and time-angle behavior.
 *
 * @author Izakji
 */
@GameTestHolder("stuff")
@PrefixGameTestTemplate(false)
public final class WallClockTests {
    private static final BlockPos LOCAL = new BlockPos(3, 2, 3);

    private static UseOnContext context(GameTestHelper helper, BlockPos support, Direction face) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var stack = new ItemStack(ModContent.OAK_WALL_CLOCK_ITEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return new UseOnContext(
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                stack,
                new BlockHitResult(
                        Vec3.atCenterOf(support)
                                .add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5)),
                        face,
                        support,
                        false));
    }

    @GameTest(template = "empty")
    public static void placementOnAllFourWalls(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(LOCAL);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            var support = pos.relative(facing.getOpposite());
            level.setBlock(support, Blocks.STONE.defaultBlockState(), 3);
            var context = context(helper, support, facing);
            helper.assertTrue(
                    ModContent.OAK_WALL_CLOCK_ITEM.get().useOn(context).consumesAction(),
                    "Wall placement failed: " + facing);
            var state = level.getBlockState(pos);
            helper.assertTrue(
                    state.is(ModContent.OAK_WALL_CLOCK.get()) && state.getValue(WallClockBlock.FACING) == facing,
                    "Clock faces the wrong direction: " + facing);
            helper.assertTrue(
                    ModContent.OAK_WALL_CLOCK.get().getTicker(level, state, ModContent.WALL_CLOCK_ENTITY.get()) == null,
                    "Clock installed a server ticker");
            level.removeBlock(pos, false);
            level.removeBlock(support, false);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rejectsFloorCeilingAndObstruction(GameTestHelper helper) {
        var level = helper.getLevel();
        var support = helper.absolutePos(LOCAL);
        level.setBlock(support, Blocks.STONE.defaultBlockState(), 3);
        for (Direction face : new Direction[] {Direction.UP, Direction.DOWN}) {
            helper.assertTrue(
                    !ModContent.OAK_WALL_CLOCK_ITEM
                            .get()
                            .useOn(context(helper, support, face))
                            .consumesAction(),
                    "Clock attached to floor or ceiling");
        }
        level.setBlock(support.south(), Blocks.STONE.defaultBlockState(), 3);
        helper.assertTrue(
                !ModContent.OAK_WALL_CLOCK_ITEM
                        .get()
                        .useOn(context(helper, support, Direction.SOUTH))
                        .consumesAction(),
                "Clock replaced an obstructing block");
        helper.assertTrue(level.getBlockState(support.south()).is(Blocks.STONE), "Obstruction was altered");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void supportRemovalDropsOnce(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(LOCAL);
        level.setBlock(pos.north(), Blocks.STONE.defaultBlockState(), 3);
        ModContent.OAK_WALL_CLOCK_ITEM.get().useOn(context(helper, pos.north(), Direction.SOUTH));
        helper.assertTrue(level.getBlockEntity(pos) != null, "Clock was not placed");
        level.removeBlock(pos.north(), false);
        helper.assertTrue(
                level.getBlockState(pos).isAir() && level.getBlockEntity(pos) == null, "Unsupported clock remained");
        int drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1)).stream()
                .filter(entity -> entity.getItem().is(ModContent.OAK_WALL_CLOCK_ITEM.get()))
                .mapToInt(entity -> entity.getItem().getCount())
                .sum();
        helper.assertTrue(drops == 1, "Support removal must drop exactly one clock");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void minecraftTimeMapping(GameTestHelper helper) {
        helper.assertTrue(
                ClockTime.hourAngle(0, 0) == 180 && ClockTime.minuteAngle(0, 0) == 0, "Sunrise must read 06:00");
        helper.assertTrue(ClockTime.hourAngle(6000, 0) == 0, "Noon must read 12:00");
        helper.assertTrue(ClockTime.hourAngle(12000, 0) == 180, "Sunset must read 18:00");
        helper.assertTrue(ClockTime.hourAngle(18000, 0) == 0, "Midnight must read 00:00");
        helper.assertTrue(
                ClockTime.hourAngle(23750, 0) == 172.5F && ClockTime.minuteAngle(23750, 0) == 270,
                "05:45 mapping is wrong");
        helper.assertTrue(ClockTime.hourAngle(24000, 0) == ClockTime.hourAngle(0, 0), "Day rollover changed the pose");
        helper.assertTrue(ClockTime.minuteAngle(0, .5F) > 0, "Partial ticks must interpolate the hands");
        helper.succeed();
    }
}
