package com.izakji.stuff.content.worktable;

import com.izakji.stuff.platform.StuffPlatformHolder;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Baked static model. Each player owns a transient menu, so the table needs no block entity.
 *
 * @author Izakji
 */
public class WorktableBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    private static final Map<Direction, VoxelShape> LOWER_SHAPES = shapes(false);
    private static final Map<Direction, VoxelShape> UPPER_SHAPES = shapes(true);

    public WorktableBlock(Properties properties) {
        super(properties);
        registerDefaultState(
                stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos above = context.getClickedPos().above();
        if (above.getY() >= context.getLevel().getMaxBuildHeight()
                || !context.getLevel().getWorldBorder().isWithinBounds(above)
                || !context.getLevel().getBlockState(above).canBeReplaced(context)) return null;
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), UPDATE_ALL);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos lower = pos.below();
            if (level.getBlockState(lower).is(this)) {
                level.destroyBlock(lower, false);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide) {
            boolean upper = state.getValue(HALF) == DoubleBlockHalf.UPPER;
            BlockPos other = upper ? pos.below() : pos.above();
            BlockState partner = level.getBlockState(other);
            if (partner.is(this) && partner.getValue(HALF) != state.getValue(HALF)) {
                // Only the lower half owns a loot drop. Its removal clears the upper without a second drop.
                if (upper) {
                    level.destroyBlock(other, true);
                } else {
                    level.setBlock(other, Blocks.AIR.defaultBlockState(), UPDATE_ALL);
                }
            }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(HALF) == DoubleBlockHalf.UPPER ? UPPER_SHAPES : LOWER_SHAPES)
                .get(state.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos owner = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        if (!level.isClientSide && level.getBlockState(owner).is(this)) {
            StuffPlatformHolder.get()
                    .openMenu(
                            player,
                            new SimpleMenuProvider(
                                    (id, inventory, viewer) -> new WorktableMenu(id, inventory, owner),
                                    Component.translatable("block.stuff.worktable")),
                            buffer -> buffer.writeBlockPos(owner));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static Map<Direction, VoxelShape> shapes(boolean upper) {
        // Cache the structural silhouette; small tools and jars do not need individual collision boxes.
        VoxelShape base = upper
                ? Shapes.or(box(.5, 0, .25, 15.5, 7, 4), box(2, 0, 7, 15, 2, 13))
                : Shapes.or(
                        box(0, 14.5, 0, 16, 16, 15),
                        box(1, 0, 1, 3, 14.5, 3),
                        box(13, 0, 1, 15, 14.5, 3),
                        box(1, 0, 12, 3, 14.5, 14),
                        box(13, 0, 12, 15, 14.5, 14),
                        box(2, 2, 2, 14, 3, 3),
                        box(1, 2, 3, 15, 14.5, 14.8));
        Map<Direction, VoxelShape> result = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape[] rotated = {Shapes.empty()};
            base.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
                AABB bounds = switch (facing) {
                    case NORTH -> new AABB(1 - x2, y1, 1 - z2, 1 - x1, y2, 1 - z1);
                    case WEST -> new AABB(1 - z2, y1, x1, 1 - z1, y2, x2);
                    case EAST -> new AABB(z1, y1, 1 - x2, z2, y2, 1 - x1);
                    default -> new AABB(x1, y1, z1, x2, y2, z2);
                };
                rotated[0] = Shapes.or(rotated[0], Shapes.create(bounds));
            });
            result.put(facing, rotated[0].optimize());
        }
        return result;
    }
}
