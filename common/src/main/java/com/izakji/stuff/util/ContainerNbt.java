package com.izakji.stuff.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

/**
 * Shared vanilla-container persistence for block entities that do not use loader item handlers.
 *
 * @author Izakji
 */
public final class ContainerNbt {
    private ContainerNbt() {}

    public static CompoundTag save(Container container, HolderLookup.Provider registries) {
        NonNullList<ItemStack> items = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            items.set(slot, container.getItem(slot));
        }
        return ContainerHelper.saveAllItems(new CompoundTag(), items, registries);
    }

    public static void load(CompoundTag tag, Container container, HolderLookup.Provider registries) {
        NonNullList<ItemStack> items = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            container.setItem(slot, items.get(slot));
        }
    }
}
