package com.izakji.stuff.content.desk;

import com.izakji.stuff.platform.StuffPlatformHolder;
import com.izakji.stuff.registry.StuffContentHolder;
import com.izakji.stuff.util.ContainerNbt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Only the left half owns storage. Viewer counts are transient and authoritative on the server.
 *
 * @author Izakji
 */
public class StudioDeskBlockEntity extends BlockEntity {
    private static final int VIEWER_RECHECK_INTERVAL = 20;
    private final SimpleContainer[] inventoriesByDrawer = new SimpleContainer[Drawer.values().length];
    private final int[] viewerCounts = new int[Drawer.values().length];

    public StudioDeskBlockEntity(BlockPos pos, BlockState state) {
        super(StuffContentHolder.get().studioDeskBlockEntity(), pos, state);
        for (Drawer drawer : Drawer.values()) {
            SimpleContainer inventory = new SimpleContainer(drawer.slotCount());
            inventory.addListener(container -> setChanged());
            inventoriesByDrawer[drawer.ordinal()] = inventory;
        }
    }

    public SimpleContainer inventory(Drawer drawer) {
        return inventoriesByDrawer[drawer.ordinal()];
    }

    public void open(Player player, Drawer drawer) {
        if (level == null || level.isClientSide || !isIntact()) return;
        StuffPlatformHolder.get()
                .openMenu(
                        player,
                        new SimpleMenuProvider(
                                (id, inventory, viewer) -> new DrawerMenu(id, inventory, this, drawer),
                                drawer.titleComponent()),
                        buffer -> {
                            buffer.writeBlockPos(worldPosition);
                            buffer.writeEnum(drawer);
                        });
    }

    public boolean isIntact() {
        return !isRemoved()
                && level != null
                && level.getBlockEntity(worldPosition) == this
                && StudioDeskBlock.paired(
                        getBlockState(), level.getBlockState(StudioDeskBlock.other(worldPosition, getBlockState())));
    }

    public void startViewing(Drawer drawer) {
        viewerCounts[drawer.ordinal()]++;
        syncOpen(drawer);
        scheduleViewerCheck();
    }

    public void stopViewing(Drawer drawer) {
        viewerCounts[drawer.ordinal()] = Math.max(0, viewerCounts[drawer.ordinal()] - 1);
        syncOpen(drawer);
    }

    private void syncOpen(Drawer drawer) {
        if (level == null || level.isClientSide || isRemoved()) return;
        BlockState state = getBlockState();
        boolean open = viewerCounts[drawer.ordinal()] > 0;
        if (state.getValue(drawer.openProperty()) == open) return;
        level.setBlock(worldPosition, state.setValue(drawer.openProperty(), open), Block.UPDATE_ALL);
        BlockPos partnerPos = StudioDeskBlock.other(worldPosition, state);
        BlockState partner = level.getBlockState(partnerPos);
        if (StudioDeskBlock.paired(state, partner)) {
            level.setBlock(partnerPos, partner.setValue(drawer.openProperty(), open), Block.UPDATE_ALL);
        }
        level.playSound(
                null,
                worldPosition,
                open ? SoundEvents.BARREL_OPEN : SoundEvents.BARREL_CLOSE,
                SoundSource.BLOCKS,
                .65F,
                1.15F);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        recoverOnLoad();
    }

    /**
     * Recovers viewer state after a world load.
     *
     * <p>A world can be saved while a menu is open. Recovery uses a scheduled check rather than a
     * permanent ticker.
     */
    public void recoverOnLoad() {
        if (level != null && !level.isClientSide && hasOpenDrawers()) scheduleViewerCheck();
    }

    private boolean hasOpenDrawers() {
        for (Drawer drawer : Drawer.values()) {
            if (viewerCounts[drawer.ordinal()] > 0 || getBlockState().getValue(drawer.openProperty())) return true;
        }
        return false;
    }

    private void scheduleViewerCheck() {
        if (level != null && !level.isClientSide && !isRemoved()) {
            level.scheduleTick(worldPosition, getBlockState().getBlock(), VIEWER_RECHECK_INTERVAL);
        }
    }

    /**
     * Rechecks the players currently viewing each open drawer.
     *
     * <p>Called by scheduled block ticks only; idle desks have no block entity ticker.
     */
    public void recheckViewers() {
        if (level == null || level.isClientSide || isRemoved() || !hasOpenDrawers()) return;
        // Scan once for all three drawers, only while open. Do not retain Player references.
        int[] current = new int[Drawer.values().length];
        for (Player player : level.players()) {
            if (player.containerMenu instanceof DrawerMenu menu && menu.isViewing(this, menu.drawer)) {
                current[menu.drawer.ordinal()]++;
            }
        }
        for (Drawer drawer : Drawer.values()) {
            viewerCounts[drawer.ordinal()] = current[drawer.ordinal()];
            syncOpen(drawer);
        }
        if (hasOpenDrawers()) scheduleViewerCheck();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (Drawer drawer : Drawer.values())
            tag.put(drawer.serializedName(), ContainerNbt.save(inventory(drawer), registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (Drawer drawer : Drawer.values()) {
            if (tag.contains(drawer.serializedName())) {
                ContainerNbt.load(tag.getCompound(drawer.serializedName()), inventory(drawer), registries);
            }
        }
    }

    public void dropStoredItems() {
        if (level == null || level.isClientSide) return;
        for (SimpleContainer inventory : inventoriesByDrawer) {
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (stack.isEmpty()) continue;
                Containers.dropItemStack(
                        level, worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5, stack);
                inventory.removeItemNoUpdate(slot);
            }
            inventory.setChanged();
        }
    }
}
