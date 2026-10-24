package com.izakji.stuff.data;

import com.izakji.stuff.Stuff;
import com.izakji.stuff.content.bookshelf.BookshelfBlock;
import com.izakji.stuff.content.chair.StudioChairBlock;
import com.izakji.stuff.content.clock.WallClockBlock;
import com.izakji.stuff.content.desk.Drawer;
import com.izakji.stuff.content.desk.StudioDeskBlock;
import com.izakji.stuff.content.recordplayer.RecordPlayerBlock;
import com.izakji.stuff.content.worktable.WorktableBlock;
import com.izakji.stuff.registry.ModContent;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Emits blockstates only; model JSON is authored in common resources.
 *
 * @author Izakji
 */
public final class StuffBlockStateProvider extends BlockStateProvider {
    StuffBlockStateProvider(PackOutput output, ExistingFileHelper files) {
        super(output, Stuff.MOD_ID, files);
    }

    @Override
    protected void registerStatesAndModels() {
        ModelFile worktableLower = existing("block/worktable_lower");
        ModelFile worktableUpper = existing("block/worktable_upper");
        getVariantBuilder(ModContent.WORKTABLE.get())
                .forAllStates(state -> ConfiguredModel.builder()
                        .modelFile(halfModel(state, WorktableBlock.HALF, worktableLower, worktableUpper))
                        .rotationY(rotationY(state.getValue(WorktableBlock.FACING)))
                        .build());

        ModelFile bookshelfLower = existing("block/bookshelf_lower");
        ModelFile bookshelfUpper = existing("block/bookshelf_upper");
        getVariantBuilder(ModContent.OAK_STUDIO_BOOKSHELF.get())
                .forAllStates(state -> ConfiguredModel.builder()
                        .modelFile(halfModel(state, BookshelfBlock.HALF, bookshelfLower, bookshelfUpper))
                        .rotationY(rotationY(state.getValue(BookshelfBlock.FACING)))
                        .build());

        ModelFile clock = existing("block/wall_clock/frame");
        getVariantBuilder(ModContent.OAK_WALL_CLOCK.get())
                .forAllStates(state -> ConfiguredModel.builder()
                        .modelFile(clock)
                        .rotationY(rotationY(state.getValue(WallClockBlock.FACING)))
                        .build());

        ModelFile recordBase = existing("block/record_player/base");
        ModelFile recordClosed = existing("block/record_player/closed");
        getVariantBuilder(ModContent.OAK_RECORD_PLAYER.get())
                .forAllStates(state -> ConfiguredModel.builder()
                        .modelFile(
                                state.getValue(RecordPlayerBlock.ACTIVE) || state.getValue(RecordPlayerBlock.HAS_RECORD)
                                        ? recordBase
                                        : recordClosed)
                        .rotationY(rotationY(state.getValue(RecordPlayerBlock.FACING)))
                        .build());

        ModelFile chair = existing("block/oak_studio_chair");
        getVariantBuilder(ModContent.OAK_STUDIO_CHAIR.get())
                .forAllStates(state -> ConfiguredModel.builder()
                        .modelFile(chair)
                        .rotationY(rotationY(state.getValue(StudioChairBlock.FACING)))
                        .build());

        ModelFile[][] deskVariants = new ModelFile[2][1 << Drawer.values().length];
        for (int half = 0; half < 2; half++) {
            for (int mask = 0; mask < deskVariants[half].length; mask++) {
                int geometryMask = half == 1 ? mask & Drawer.LONG.stateMask() : mask;
                String path = "oak_studio_desk/" + (half == 0 ? "left_" : "right_") + geometryMask;
                deskVariants[half][mask] = existing(path);
            }
        }
        getVariantBuilder(ModContent.OAK_STUDIO_DESK.get()).forAllStates(state -> {
            int mask = drawerMask(state);
            return ConfiguredModel.builder()
                    .modelFile(deskVariants[state.getValue(StudioDeskBlock.RIGHT) ? 1 : 0][mask])
                    .rotationY(rotationY(state.getValue(StudioDeskBlock.FACING)))
                    .build();
        });
    }

    private ModelFile existing(String path) {
        return models().getExistingFile(modLoc(path));
    }

    private static int drawerMask(BlockState state) {
        int mask = 0;
        for (Drawer drawer : Drawer.values()) {
            if (state.getValue(drawer.openProperty())) mask |= drawer.stateMask();
        }
        return mask;
    }

    private static ModelFile halfModel(
            BlockState state, EnumProperty<DoubleBlockHalf> half, ModelFile lower, ModelFile upper) {
        return state.getValue(half) == DoubleBlockHalf.UPPER ? upper : lower;
    }

    private static int rotationY(Direction facing) {
        return switch (facing) {
            case WEST -> 90;
            case NORTH -> 180;
            case EAST -> 270;
            default -> 0;
        };
    }
}
