package com.izakji.stuff.content.worktable;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

/**
 * NeoForge-only piston reaction for the common worktable block.
 *
 * @author Izakji
 */
public final class NeoForgeWorktableBlock extends WorktableBlock {
    public NeoForgeWorktableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }
}
