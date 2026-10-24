package com.izakji.stuff.content.clock;

import com.izakji.stuff.registry.StuffContentHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Render anchor only: no ticker, saved animation state, or custom packets.
 *
 * @author Izakji
 */
public final class WallClockBlockEntity extends BlockEntity {
    public WallClockBlockEntity(BlockPos pos, BlockState state) {
        super(StuffContentHolder.get().wallClockBlockEntity(), pos, state);
    }
}
