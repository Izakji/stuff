package com.izakji.stuff.content.recordplayer;

import com.izakji.stuff.registry.StuffContentHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Server-owned sequence; clients interpolate timestamps rather than receiving per-frame packets.
 *
 * @author Izakji
 */
public final class RecordPlayerBlockEntity extends BlockEntity {
    public enum Phase {
        IDLE,
        INSERTING,
        PLAYING,
        LOADED,
        CLOSING
    }

    public static final int START_TICKS = 34;
    public static final int CLOSE_TICKS = 14;
    private static final int LID_OPEN_TICKS = 12;
    private static final int ARM_OPEN_START_TICKS = 22;
    private static final int ARM_OPEN_TICKS = 12;
    private static final int ARM_CLOSE_TICKS = 10;
    private static final int DISC_LIFT_START_TICKS = 12;
    private static final int DISC_LIFT_TICKS = 10;
    private static final String DISC_TAG = "Disc";
    private static final String PHASE_TAG = "Phase";
    private static final String PHASE_START_TAG = "PhaseStart";
    private static final String CLOSING_LID_TAG = "ClosingLid";
    private static final String CLOSING_ARM_TAG = "ClosingArm";
    private ItemStack disc = ItemStack.EMPTY;
    private Phase phase = Phase.IDLE;
    private long phaseStart;
    private float closingLid;
    private float closingArm;
    private boolean restoreOnLoad;
    private final JukeboxSongPlayer song = new JukeboxSongPlayer(this::setChanged, worldPosition);

    public RecordPlayerBlockEntity(BlockPos pos, BlockState state) {
        super(StuffContentHolder.get().recordPlayerBlockEntity(), pos, state);
    }

    public ItemStack disc() {
        return disc;
    }

    public Phase phase() {
        return phase;
    }

    public boolean isPlaying() {
        return song.isPlaying();
    }

    public float elapsed(float partialTick) {
        return level == null ? 0 : Math.max(0, level.getGameTime() - phaseStart + partialTick);
    }

    public boolean canInsert() {
        return level != null
                && phase == Phase.IDLE
                && disc.isEmpty()
                && level.getBlockState(worldPosition.above())
                        .getCollisionShape(level, worldPosition.above())
                        .isEmpty();
    }

    public boolean insert(ItemStack stack, Player player) {
        if (level == null
                || level.isClientSide
                || !canInsert()
                || JukeboxSong.fromStack(level.registryAccess(), stack).isEmpty()) return false;
        disc = stack.consumeAndReturn(1, player);
        transition(Phase.INSERTING);
        return true;
    }

    public void eject() {
        if (level == null || level.isClientSide || disc.isEmpty()) return;
        closingLid = lidProgress(0);
        closingArm = armProgress(0);
        dropStoredDisc();
        transition(Phase.CLOSING);
    }

    public void dropStoredDisc() {
        if (level == null || level.isClientSide) return;
        song.stop(level, getBlockState());
        if (disc.isEmpty()) return;
        ItemStack dropped = disc;
        disc = ItemStack.EMPTY;
        ItemEntity item = new ItemEntity(
                level, worldPosition.getX() + .5, worldPosition.getY() + 1.1, worldPosition.getZ() + .5, dropped);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
        setChanged();
    }

    private void transition(Phase next) {
        phase = next;
        phaseStart = level.getGameTime();
        boolean active = next == Phase.INSERTING || next == Phase.PLAYING || next == Phase.CLOSING;
        if (getBlockState().getValue(RecordPlayerBlock.ACTIVE) != active
                || getBlockState().getValue(RecordPlayerBlock.HAS_RECORD) != !disc.isEmpty()) {
            level.setBlock(
                    worldPosition,
                    getBlockState()
                            .setValue(RecordPlayerBlock.ACTIVE, active)
                            .setValue(RecordPlayerBlock.HAS_RECORD, !disc.isEmpty()),
                    Block.UPDATE_CLIENTS);
        }
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RecordPlayerBlockEntity record) {
        if (record.phase == Phase.INSERTING && record.elapsed(0) >= START_TICKS) {
            JukeboxSong.fromStack(level.registryAccess(), record.disc)
                    .ifPresentOrElse(
                            holder -> {
                                record.song.play(level, holder);
                                record.transition(Phase.PLAYING);
                            },
                            () -> record.transition(Phase.LOADED));
        } else if (record.phase == Phase.PLAYING) {
            record.song.tick(level, state);
            if (!record.song.isPlaying()) record.transition(Phase.LOADED);
        } else if (record.phase == Phase.CLOSING && record.elapsed(0) >= CLOSE_TICKS) {
            record.transition(Phase.IDLE);
        }
    }

    private static float ease(float value) {
        float t = Mth.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }

    public float lidProgress(float partialTick) {
        return switch (phase) {
            case IDLE -> 0;
            case INSERTING -> ease(elapsed(partialTick) / LID_OPEN_TICKS);
            case PLAYING, LOADED -> 1;
            case CLOSING -> closingLid * (1 - ease(elapsed(partialTick) / CLOSE_TICKS));
        };
    }

    public float armProgress(float partialTick) {
        return switch (phase) {
            case INSERTING -> ease((elapsed(partialTick) - ARM_OPEN_START_TICKS) / ARM_OPEN_TICKS);
            case PLAYING -> 1;
            case LOADED -> 1 - ease(elapsed(partialTick) / ARM_CLOSE_TICKS);
            case CLOSING -> closingArm * (1 - ease(elapsed(partialTick) / ARM_CLOSE_TICKS));
            default -> 0;
        };
    }

    public float discLift(float partialTick) {
        return phase == Phase.INSERTING
                ? .45F * (1 - ease((elapsed(partialTick) - DISC_LIFT_START_TICKS) / DISC_LIFT_TICKS))
                : 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        if (!disc.isEmpty()) tag.put(DISC_TAG, disc.save(lookup));
        tag.putInt(PHASE_TAG, phase.ordinal());
        tag.putLong(PHASE_START_TAG, phaseStart);
        tag.putFloat(CLOSING_LID_TAG, closingLid);
        tag.putFloat(CLOSING_ARM_TAG, closingArm);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        disc = tag.contains(DISC_TAG, 10)
                ? ItemStack.parse(lookup, tag.getCompound(DISC_TAG)).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        phase = Phase.values()[Mth.clamp(tag.getInt(PHASE_TAG), 0, Phase.values().length - 1)];
        phaseStart = tag.getLong(PHASE_START_TAG);
        closingLid = Mth.clamp(tag.getFloat(CLOSING_LID_TAG), 0, 1);
        closingArm = Mth.clamp(tag.getFloat(CLOSING_ARM_TAG), 0, 1);
        restoreOnLoad = true;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
        return saveWithoutMetadata(lookup);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        recoverOnLoad();
    }

    /**
     * Restores the loaded disc state after a world reload.
     *
     * <p>Loaded block entities retain the disc but never replay audio after a world reload.
     */
    public void recoverOnLoad() {
        if (level != null && !level.isClientSide && restoreOnLoad) {
            transition(disc.isEmpty() ? Phase.IDLE : Phase.LOADED);
            restoreOnLoad = false;
        }
    }

    @Override
    public void setRemoved() {
        if (level != null) {
            if (level.isClientSide && !disc.isEmpty()) level.levelEvent(1011, worldPosition, 0);
            else if (!level.isClientSide) song.stop(level, getBlockState());
        }
        super.setRemoved();
    }
}
