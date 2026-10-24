package com.izakji.stuff.data;

import com.izakji.stuff.Stuff;
import com.izakji.stuff.registry.ModContent;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Generates block tags for Stuff furniture.
 *
 * @author Izakji
 */
public final class StuffBlockTagsProvider extends BlockTagsProvider {
    public StuffBlockTagsProvider(
            PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper files) {
        super(output, lookup, Stuff.MOD_ID, files);
    }

    @Override
    protected void addTags(HolderLookup.Provider lookup) {
        tag(BlockTags.MINEABLE_WITH_AXE)
                .add(
                        ModContent.WORKTABLE.get(),
                        ModContent.OAK_WALL_CLOCK.get(),
                        ModContent.OAK_STUDIO_BOOKSHELF.get(),
                        ModContent.OAK_RECORD_PLAYER.get(),
                        ModContent.OAK_STUDIO_DESK.get(),
                        ModContent.OAK_STUDIO_CHAIR.get());
    }
}
