package com.izakji.stuff.content.desk;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

/**
 * NeoForge-only piston reaction for the common desk block.
 *
 * @author Izakji
 */
public final class NeoForgeStudioDeskBlock extends StudioDeskBlock {
    public NeoForgeStudioDeskBlock(Properties properties) {
        super(properties);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }
}
