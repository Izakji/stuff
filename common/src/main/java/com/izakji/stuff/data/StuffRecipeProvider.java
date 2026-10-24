package com.izakji.stuff.data;

import com.izakji.stuff.StuffConstants;
import com.izakji.stuff.content.worktable.CountedIngredient;
import com.izakji.stuff.content.worktable.WorktableCategory;
import com.izakji.stuff.content.worktable.WorktableRecipe;
import com.izakji.stuff.registry.StuffContentHolder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/**
 * Generates the worktable bootstrap recipe and the furniture recipes.
 *
 * @author Izakji
 */
public final class StuffRecipeProvider extends RecipeProvider {
    public StuffRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.DECORATIONS, StuffContentHolder.get().worktable())
                .pattern("AAA")
                .pattern("B B")
                .pattern("B B")
                .define('A', Items.OAK_LOG)
                .define('B', Items.IRON_INGOT)
                .unlockedBy("has_oak_log", has(Items.OAK_LOG))
                .save(output);
        furniture(
                output,
                "oak_studio_desk",
                WorktableCategory.TABLES_AND_CHAIRS,
                StuffContentHolder.get().oakStudioDesk(),
                material(Items.OAK_PLANKS, 7));
        furniture(
                output,
                "oak_studio_chair",
                WorktableCategory.TABLES_AND_CHAIRS,
                StuffContentHolder.get().oakStudioChair(),
                material(Items.OAK_PLANKS, 6));
        furniture(
                output,
                "oak_wall_clock",
                WorktableCategory.DECORATION,
                StuffContentHolder.get().oakWallClock(),
                material(Items.OAK_LOG, 1),
                material(Items.IRON_INGOT, 2));
        furniture(
                output,
                "oak_studio_bookshelf",
                WorktableCategory.STORAGE,
                StuffContentHolder.get().oakStudioBookshelf(),
                material(Items.OAK_PLANKS, 8),
                material(Items.BOOK, 3));
        furniture(
                output,
                "oak_record_player",
                WorktableCategory.UTILITY,
                StuffContentHolder.get().oakRecordPlayer(),
                material(Items.SPRUCE_PLANKS, 6),
                material(Items.IRON_INGOT, 2),
                material(Items.JUKEBOX, 1));
    }

    private static CountedIngredient material(ItemLike item, int count) {
        return new CountedIngredient(Ingredient.of(item), count);
    }

    /**
     * Writes one worktable recipe.
     *
     * <p>New furniture needs only a category, output, and counted ingredient list.
     *
     * @param output the recipe output
     * @param id the recipe path under the mod namespace
     * @param category the worktable catalogue category
     * @param item the crafted result
     * @param materials the counted ingredients
     */
    private static void furniture(
            RecipeOutput output, String id, WorktableCategory category, ItemLike item, CountedIngredient... materials) {
        output.accept(
                ResourceLocation.fromNamespaceAndPath(StuffConstants.MOD_ID, id),
                new WorktableRecipe(category, List.of(materials), new ItemStack(item)),
                null);
    }
}
