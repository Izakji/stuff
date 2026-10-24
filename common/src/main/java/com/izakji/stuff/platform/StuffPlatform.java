package com.izakji.stuff.platform;

import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Small loader bridge for APIs that differ between NeoForge and future Fabric support.
 *
 * @author Izakji
 */
public interface StuffPlatform {
    void openMenu(Player player, MenuProvider provider, Consumer<FriendlyByteBuf> data);

    boolean hasCraftingRemainingItem(ItemStack stack);

    ItemStack craftingRemainingItem(ItemStack stack);
}
