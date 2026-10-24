package com.izakji.stuff.content.chair;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

/**
 * NeoForge-only piston reaction for the common chair block.
 *
 * @author Izakji
 */
public final class NeoForgeStudioChairBlock extends StudioChairBlock {
    public NeoForgeStudioChairBlock(Properties properties) {
        super(properties);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }
}
