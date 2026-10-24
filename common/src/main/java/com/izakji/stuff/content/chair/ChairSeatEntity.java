package com.izakji.stuff.content.chair;

import com.izakji.stuff.registry.StuffContentHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * Ephemeral riding anchor. Never saved and never spawned for an empty chair.
 *
 * @author Izakji
 */
public final class ChairSeatEntity extends Entity {
    private static final double SEAT_HEIGHT = 9.0 / 16.0;

    public ChairSeatEntity(EntityType<? extends ChairSeatEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setInvisible(true);
    }

    public static boolean trySit(Level level, BlockPos pos, Player player) {
        if (level.isClientSide
                || player.isSpectator()
                || player.isPassenger()
                || !player.isAlive()
                || !level.getBlockState(pos).is(StuffContentHolder.get().oakStudioChair())) return false;
        // Only an interaction-time local query; no global list or world scan.
        for (ChairSeatEntity seat : level.getEntitiesOfClass(ChairSeatEntity.class, new AABB(pos))) {
            if (!seat.blockPosition().equals(pos)) continue;
            if (seat.isVehicle()) return false;
            seat.discard();
        }
        // Require standing headroom above the seat. Ignore the chair's own backrest.
        AABB headroom = new AABB(
                pos.getX() + .2, pos.getY() + 1, pos.getZ() + .2, pos.getX() + .8, pos.getY() + 2, pos.getZ() + .8);
        if (!hasHeadroom(level, pos, player, headroom)) return false;
        ChairSeatEntity seat = new ChairSeatEntity(StuffContentHolder.get().chairSeat(), level);
        seat.setPos(Vec3.atBottomCenterOf(pos));
        seat.setYRot(level.getBlockState(pos).getValue(StudioChairBlock.FACING).toYRot());
        if (!level.addFreshEntity(seat)) return false;
        if (!player.startRiding(seat)) {
            seat.discard();
            return false;
        }
        player.setYRot(seat.getYRot());
        player.setYHeadRot(seat.getYRot());
        seat.positionRider(player);
        return true;
    }

    private static boolean hasHeadroom(Level level, BlockPos chairPos, Player player, AABB headroom) {
        // The backrest extends above its block. Exclude only its owning position,
        // not overlapping geometry from a ceiling or another block.
        var collisions = new BlockCollisions<Boolean>(
                level, player, headroom, false, (blockPos, shape) -> !blockPos.equals(chairPos));
        while (collisions.hasNext()) {
            if (collisions.next()) return false;
        }
        if (!level.getEntityCollisions(player, headroom).isEmpty()) return false;
        var border = level.getWorldBorder();
        return !border.isInsideCloseToBorder(player, headroom)
                || !Shapes.joinIsNotEmpty(border.getCollisionShape(), Shapes.create(headroom), BooleanOp.AND);
    }

    @Override
    public void tick() {
        if (!level().isClientSide
                && (!isVehicle()
                        || !level().getBlockState(blockPosition())
                                .is(StuffContentHolder.get().oakStudioChair()))) {
            ejectPassengers();
            discard();
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player && !isVehicle();
    }

    @Override
    public Vec3 getPassengerRidingPosition(Entity passenger) {
        return position().add(0, SEAT_HEIGHT, 0);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Direction facing = getDirection();
        for (Direction side :
                new Direction[] {facing, facing.getClockWise(), facing.getCounterClockWise(), facing.getOpposite()}) {
            BlockPos candidate = blockPosition().relative(side);
            if (!level().hasChunkAt(candidate)) continue;
            Vec3 safe = DismountHelper.findSafeDismountLocation(passenger.getType(), level(), candidate, true);
            if (safe != null) return safe;
        }
        Vec3 above = DismountHelper.findSafeDismountLocation(
                passenger.getType(), level(), blockPosition().above(), true);
        return above != null ? above : super.getDismountLocationForPassenger(passenger);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
