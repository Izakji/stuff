package com.izakji.stuff.content.chair;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Static furniture: no block entity, occupied property, or ticker.
 *
 * @author Izakji
 */
public class StudioChairBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final Map<Direction, VoxelShape> SHAPES = createShapes();

    public StudioChairBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSpectator() || player.isShiftKeyDown() || player.isPassenger()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        return ChairSeatEntity.trySit(level, pos, player) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide) {
            for (ChairSeatEntity seat : level.getEntitiesOfClass(ChairSeatEntity.class, new AABB(pos))) {
                if (seat.blockPosition().equals(pos)) {
                    seat.ejectPassengers();
                    seat.discard();
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
        return SHAPES.get(state.getValue(FACING));
    }

    private static Map<Direction, VoxelShape> createShapes() {
        VoxelShape base = Shapes.or(
                box(2, 7, 2, 14, 9, 14),
                box(2, 0, 2, 4, 19, 4),
                box(12, 0, 2, 14, 19, 4),
                box(2, 0, 12, 4, 7, 14),
                box(12, 0, 12, 14, 7, 14),
                box(4, 9.5, 2, 12, 18.5, 4),
                box(2, 18.5, 2, 14, 20, 4));
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
