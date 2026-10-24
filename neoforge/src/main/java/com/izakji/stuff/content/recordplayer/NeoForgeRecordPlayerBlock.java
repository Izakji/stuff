package com.izakji.stuff.content.recordplayer;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

/**
 * NeoForge-only piston reaction for the common record player block.
 *
 * @author Izakji
 */
public final class NeoForgeRecordPlayerBlock extends RecordPlayerBlock {
    public NeoForgeRecordPlayerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }
}
