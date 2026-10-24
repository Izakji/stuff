package com.izakji.stuff.content.worktable;

import com.izakji.stuff.registry.StuffContentHolder;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * One ordered insertion slot per counted ingredient. Normal recipe-manager datapack sync is sufficient.
 *
 * @param category the worktable catalogue category
 * @param materials the ordered counted ingredients
 * @param result the crafted result
 *
 * @author Izakji
 */
public record WorktableRecipe(WorktableCategory category, List<CountedIngredient> materials, ItemStack result)
        implements Recipe<RecipeInput> {
    public static final int MAX_INGREDIENTS = 6;

    public WorktableRecipe {
        materials = List.copyOf(materials);
        result = result.copy();
        if (materials.isEmpty()
                || materials.size() > MAX_INGREDIENTS
                || result.isEmpty()
                || result.getCount() > result.getMaxStackSize())
            throw new IllegalArgumentException("Invalid worktable recipe");
    }

    @Override
    public ItemStack result() {
        return result.copy();
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        if (input.size() < materials.size()) return false;
        for (int i = 0; i < materials.size(); i++) if (!materials.get(i).matches(input.getItem(i))) return false;
        for (int i = materials.size(); i < input.size(); i++)
            if (!input.getItem(i).isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider lookup) {
        return result.copy();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider lookup) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= materials.size();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        var ingredients = NonNullList.<Ingredient>create();
        materials.forEach(material -> ingredients.add(material.ingredient()));
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return StuffContentHolder.get().worktableRecipeSerializer();
    }

    @Override
    public RecipeType<?> getType() {
        return StuffContentHolder.get().worktableRecipeType();
    }

    public static final class Serializer implements RecipeSerializer<WorktableRecipe> {
        private static final MapCodec<WorktableRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        WorktableCategory.CODEC.fieldOf("category").forGetter(WorktableRecipe::category),
                        CountedIngredient.CODEC
                                .listOf(1, MAX_INGREDIENTS)
                                .fieldOf("ingredients")
                                .forGetter(WorktableRecipe::materials),
                        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(WorktableRecipe::result))
                .apply(instance, WorktableRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, WorktableRecipe> STREAM = StreamCodec.composite(
                ByteBufCodecs.fromCodec(WorktableCategory.CODEC),
                WorktableRecipe::category,
                CountedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_INGREDIENTS)),
                WorktableRecipe::materials,
                ItemStack.STREAM_CODEC,
                WorktableRecipe::result,
                WorktableRecipe::new);

        @Override
        public MapCodec<WorktableRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WorktableRecipe> streamCodec() {
            return STREAM;
        }
    }
}
