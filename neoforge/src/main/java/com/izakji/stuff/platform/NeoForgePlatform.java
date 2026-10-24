package com.izakji.stuff.platform;

import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * NeoForge implementation of the small common platform bridge.
 *
 * @author Izakji
 */
public final class NeoForgePlatform implements StuffPlatform {
    @Override
    public void openMenu(Player player, MenuProvider provider, Consumer<FriendlyByteBuf> data) {
        player.openMenu(provider, buffer -> data.accept(buffer));
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return stack.hasCraftingRemainingItem();
    }

    @Override
    public ItemStack craftingRemainingItem(ItemStack stack) {
        return stack.getCraftingRemainingItem();
    }
}
