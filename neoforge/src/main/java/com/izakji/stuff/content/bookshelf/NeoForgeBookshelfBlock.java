package com.izakji.stuff.content.bookshelf;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

/**
 * NeoForge-only piston reaction for the common bookshelf block.
 *
 * @author Izakji
 */
public final class NeoForgeBookshelfBlock extends BookshelfBlock {
    public NeoForgeBookshelfBlock(Properties properties) {
        super(properties);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }
}
