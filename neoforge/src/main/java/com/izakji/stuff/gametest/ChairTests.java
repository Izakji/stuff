package com.izakji.stuff.gametest;

import com.izakji.stuff.content.chair.ChairSeatEntity;
import com.izakji.stuff.content.chair.StudioChairBlock;
import com.izakji.stuff.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests for chair seating, headroom, and cleanup.
 *
 * @author Izakji
 */
@GameTestHolder("stuff")
@PrefixGameTestTemplate(false)
public final class ChairTests {
    private static final BlockPos LOCAL_POS = new BlockPos(3, 1, 3);

    private static BlockPos place(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(LOCAL_POS);
        helper.getLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(pos, ModContent.OAK_STUDIO_CHAIR.get().defaultBlockState(), 3);
        return pos;
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void seatingAndDismountCleanup(GameTestHelper helper) {
        BlockPos pos = place(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(pos.relative(Direction.SOUTH)));
        helper.useBlock(LOCAL_POS, player);
        helper.assertTrue(player.getVehicle() instanceof ChairSeatEntity, "Right click did not mount chair");
        var seat = (ChairSeatEntity) player.getVehicle();
        helper.assertTrue(
                Math.abs(seat.getPassengerRidingPosition(player).y - (pos.getY() + 9.0 / 16)) < .001,
                "Incorrect seat attachment height");
        var second = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(!ChairSeatEntity.trySit(helper.getLevel(), pos, second), "Two players occupied one chair");
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) == null, "Static chair allocated a block entity");
        player.stopRiding();
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(seat.isRemoved(), "Dismount left a live seat entity");
            helper.assertTrue(
                    helper.getLevel()
                            .getEntitiesOfClass(ChairSeatEntity.class, new AABB(pos))
                            .isEmpty(),
                    "Empty chair retained a riding anchor");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void breakingOccupiedChair(GameTestHelper helper) {
        BlockPos pos = place(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(ChairSeatEntity.trySit(helper.getLevel(), pos, player), "Could not mount chair");
        var seat = player.getVehicle();
        helper.getLevel().destroyBlock(pos, true);
        helper.assertTrue(!player.isPassenger() && seat.isRemoved(), "Breaking chair did not release rider");
        int count = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1)).stream()
                .filter(item -> item.getItem().is(ModContent.OAK_STUDIO_CHAIR_ITEM.get()))
                .mapToInt(item -> item.getItem().getCount())
                .sum();
        helper.assertTrue(count == 1, "Expected exactly one chair drop");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void tallBackrestHeadroomAllFacings(GameTestHelper helper) {
        BlockPos pos = place(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            helper.getLevel()
                    .setBlock(
                            pos,
                            ModContent.OAK_STUDIO_CHAIR
                                    .get()
                                    .defaultBlockState()
                                    .setValue(StudioChairBlock.FACING, facing),
                            3);
            player.setPos(Vec3.atCenterOf(pos.relative(facing)));
            helper.getLevel().setBlock(pos.above(), Blocks.STONE.defaultBlockState(), 3);
            helper.assertTrue(
                    !ChairSeatEntity.trySit(helper.getLevel(), pos, player), "Ignored ceiling while facing " + facing);
            helper.assertTrue(
                    helper.getLevel()
                            .getEntitiesOfClass(ChairSeatEntity.class, new AABB(pos))
                            .isEmpty(),
                    "Blocked mounting leaked an anchor while facing " + facing);
            helper.getLevel().setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
            helper.assertTrue(
                    ChairSeatEntity.trySit(helper.getLevel(), pos, player),
                    "Own backrest blocked mounting while facing " + facing);
            var seat = player.getVehicle();
            player.stopRiding();
            seat.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void blockedHeadroom(GameTestHelper helper) {
        BlockPos pos = place(helper);
        helper.getLevel().setBlock(pos.above(), Blocks.STONE.defaultBlockState(), 3);
        helper.assertTrue(
                !ChairSeatEntity.trySit(helper.getLevel(), pos, helper.makeMockPlayer(GameType.SURVIVAL)),
                "Mounted through a solid ceiling");
        helper.assertTrue(
                helper.getLevel()
                        .getEntitiesOfClass(ChairSeatEntity.class, new AABB(pos))
                        .isEmpty(),
                "Failed mounting leaked a seat");
        helper.succeed();
    }
}
