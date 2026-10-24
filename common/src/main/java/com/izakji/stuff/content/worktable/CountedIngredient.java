package com.izakji.stuff.content.worktable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * One ingredient and the exact count consumed by a worktable recipe.
 *
 * @param ingredient the accepted ingredient
 * @param count the exact number consumed
 *
 * @author Izakji
 */
public record CountedIngredient(Ingredient ingredient, int count) {
    public static final Codec<CountedIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CountedIngredient::ingredient),
                    Codec.intRange(1, 64).fieldOf("count").forGetter(CountedIngredient::count))
            .apply(instance, CountedIngredient::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CountedIngredient> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC,
            CountedIngredient::ingredient,
            ByteBufCodecs.VAR_INT,
            CountedIngredient::count,
            CountedIngredient::new);

    public CountedIngredient {
        if (count < 1 || count > 64) throw new IllegalArgumentException("Ingredient count must be 1..64");
    }

    public boolean matches(ItemStack stack) {
        return stack.getCount() >= count && ingredient.test(stack);
    }
}
