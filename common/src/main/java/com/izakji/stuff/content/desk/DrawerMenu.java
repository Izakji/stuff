package com.izakji.stuff.content.desk;

import com.izakji.stuff.registry.StuffContentHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Container menu for one desk drawer.
 *
 * @author Izakji
 */
public final class DrawerMenu extends AbstractContainerMenu {
    public final Drawer drawer;
    public final BlockPos deskPos;

    @Nullable
    private final StudioDeskBlockEntity desk;

    private boolean closed;

    /**
     * Creates the client-side menu from the server packet data.
     *
     * @param id the container window id
     * @param playerInventory the viewing player's inventory
     * @param data the packet data containing the desk position and drawer type
     */
    public DrawerMenu(int id, Inventory playerInventory, FriendlyByteBuf data) {
        this(id, playerInventory, data.readBlockPos(), data.readEnum(Drawer.class));
    }

    private DrawerMenu(int id, Inventory playerInventory, BlockPos pos, Drawer drawer) {
        this(id, playerInventory, pos, drawer, new SimpleContainer(drawer.slotCount()), null);
    }

    public DrawerMenu(int id, Inventory playerInventory, StudioDeskBlockEntity desk, Drawer drawer) {
        this(id, playerInventory, desk.getBlockPos(), drawer, desk.inventory(drawer), desk);
        desk.startViewing(drawer);
    }

    private DrawerMenu(
            int id,
            Inventory playerInventory,
            BlockPos pos,
            Drawer drawer,
            Container inventory,
            StudioDeskBlockEntity desk) {
        super(StuffContentHolder.get().drawerMenu(), id);
        this.drawer = drawer;
        this.desk = desk;
        this.deskPos = pos;
        for (int slot = 0; slot < drawer.slotCount(); slot++) {
            addSlot(new Slot(inventory, slot, drawerX(drawer, slot), drawerY(slot, drawer)));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 114 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 172));
        }
    }

    public static int drawerX(Drawer drawer, int slot) {
        return (176 - drawer.columnCount() * 18) / 2 + 1 + (slot % drawer.columnCount()) * 18;
    }

    public static int drawerY(int slot, Drawer drawer) {
        return 30 + (slot / drawer.columnCount()) * 18;
    }

    public boolean isViewing(StudioDeskBlockEntity desk, Drawer drawer) {
        return !closed && this.desk == desk && this.drawer == drawer;
    }

    @Override
    public boolean stillValid(Player player) {
        return desk == null
                || desk.isIntact()
                        && player.level() == desk.getLevel()
                        && player.distanceToSqr(deskPos.getX() + .5, deskPos.getY() + .5, deskPos.getZ() + .5) <= 64;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!closed && desk != null) desk.stopViewing(drawer);
        closed = true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < drawer.slotCount()) {
            if (!moveItemStackTo(stack, drawer.slotCount(), slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, drawer.slotCount(), false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }
}
