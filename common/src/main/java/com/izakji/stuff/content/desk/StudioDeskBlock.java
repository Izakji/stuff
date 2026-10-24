package com.izakji.stuff.content.desk;

import com.mojang.serialization.MapCodec;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Two-block desk with three independently opening drawer inventories.
 *
 * @author Izakji
 */
public class StudioDeskBlock extends BaseEntityBlock {
    private static final double DRAWER_FACE_Z = 14;
    private static final double DRAWER_CLOSED_Z = 15.75;
    private static final double DRAWER_OPEN_Z = 20.75;
    public static final MapCodec<StudioDeskBlock> CODEC = simpleCodec(StudioDeskBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");
    // Scope creative removal across the nested removal callback of the other half.
    private static final ThreadLocal<Boolean> CREATIVE_REMOVAL = ThreadLocal.withInitial(() -> false);
    private final Map<BlockState, VoxelShape> shapes = new HashMap<>();

    public StudioDeskBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition
                .any()
                .setValue(FACING, Direction.SOUTH)
                .setValue(RIGHT, false)
                .setValue(Drawer.SMALL.openProperty(), false)
                .setValue(Drawer.LONG.openProperty(), false)
                .setValue(Drawer.LARGE.openProperty(), false));
        for (BlockState state : stateDefinition.getPossibleStates()) shapes.put(state, buildShape(state));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(
                FACING, RIGHT, Drawer.SMALL.openProperty(), Drawer.LONG.openProperty(), Drawer.LARGE.openProperty());
    }

    public static BlockPos other(BlockPos pos, BlockState state) {
        Direction right = state.getValue(FACING).getCounterClockWise();
        return pos.relative(state.getValue(RIGHT) ? right.getOpposite() : right);
    }

    public static BlockPos owner(BlockPos pos, BlockState state) {
        return state.getValue(RIGHT) ? other(pos, state) : pos;
    }

    public static boolean paired(BlockState first, BlockState second) {
        return second.is(first.getBlock())
                && first.getValue(FACING) == second.getValue(FACING)
                && first.getValue(RIGHT) != second.getValue(RIGHT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
        BlockPos partner = other(context.getClickedPos(), state);
        return context.getLevel().getWorldBorder().isWithinBounds(partner)
                        && context.getLevel().getBlockState(partner).canBeReplaced(context)
                ? state
                : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide) level.setBlock(other(pos, state), state.setValue(RIGHT, true), UPDATE_ALL);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighbor,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos) {
        // The item places the second half after the first; validate the pair on the following tick.
        if (neighborPos.equals(other(pos, state))) level.scheduleTick(pos, this, 1);
        return state;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!paired(state, level.getBlockState(other(pos, state)))) {
            level.destroyBlock(pos, true);
        } else if (level.getBlockEntity(pos) instanceof StudioDeskBlockEntity desk) {
            desk.recheckViewers();
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative()) {
            BlockPos partner = other(pos, state);
            if (paired(state, level.getBlockState(partner))) {
                CREATIVE_REMOVAL.set(true);
                try {
                    level.setBlock(partner, Blocks.AIR.defaultBlockState(), UPDATE_ALL);
                } finally {
                    CREATIVE_REMOVAL.remove();
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock())) {
            if (level.getBlockEntity(pos) instanceof StudioDeskBlockEntity desk) desk.dropStoredItems();
            super.onRemove(state, level, pos, replacement, moving);
            if (!level.isClientSide) {
                BlockPos partner = other(pos, state);
                if (paired(state, level.getBlockState(partner))) level.destroyBlock(partner, !CREATIVE_REMOVAL.get());
            }
        } else {
            super.onRemove(state, level, pos, replacement, moving);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (hit.getDirection() != state.getValue(FACING)) return InteractionResult.PASS;
        Vec3 local = localHit(hit.getLocation().subtract(Vec3.atLowerCornerOf(pos)), state.getValue(FACING));
        Drawer drawer = Drawer.atFrontFace(local.x * 16 + (state.getValue(RIGHT) ? 16 : 0), local.y * 16);
        if (drawer == null || player.isSpectator()) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(owner(pos, state)) instanceof StudioDeskBlockEntity desk) {
            desk.open(player, drawer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static Vec3 localHit(Vec3 point, Direction facing) {
        return switch (facing) {
            case NORTH -> new Vec3(1 - point.x, point.y, 1 - point.z);
            case WEST -> new Vec3(point.z, point.y, 1 - point.x);
            case EAST -> new Vec3(1 - point.z, point.y, point.x);
            default -> point;
        };
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(RIGHT) ? null : new StudioDeskBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state);
    }

    private static VoxelShape buildShape(BlockState state) {
        VoxelShape shape = box(0, 15, 0, 16, 16, 16);
        boolean right = state.getValue(RIGHT);
        int edge = right ? 13 : 1;
        shape = Shapes.or(
                shape,
                box(edge, 0, 1, edge + 2, 15, 3),
                box(edge, 0, 13, edge + 2, 15, 15),
                box(edge + .25, 5, 3, edge + 1.75, 14, 13),
                box(0, 6, 1.5, 16, 14.5, 2.5));
        if (!right) shape = Shapes.or(shape, box(11, 5, 2.5, 12, 15, 15));
        for (Drawer drawer : Drawer.values()) {
            Drawer.FrontFace face = drawer.frontFace();
            double cellOffset = right ? 16 : 0;
            double cellMinX = Math.max(0, face.minX() - cellOffset);
            double cellMaxX = Math.min(16, face.maxX() - cellOffset);
            if (cellMinX >= cellMaxX) continue;
            double faceZ = state.getValue(drawer.openProperty()) ? DRAWER_OPEN_Z : DRAWER_CLOSED_Z;
            shape = Shapes.or(shape, box(cellMinX, face.minY(), DRAWER_FACE_Z, cellMaxX, face.maxY(), faceZ));
        }
        return rotateShape(shape, state.getValue(FACING));
    }

    private static VoxelShape rotateShape(VoxelShape shape, Direction facing) {
        VoxelShape[] result = {Shapes.empty()};
        shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
            AABB box = switch (facing) {
                case NORTH -> new AABB(1 - x2, y1, 1 - z2, 1 - x1, y2, 1 - z1);
                case WEST -> new AABB(1 - z2, y1, x1, 1 - z1, y2, x2);
                case EAST -> new AABB(z1, y1, 1 - x2, z2, y2, 1 - x1);
                default -> new AABB(x1, y1, z1, x2, y2, z2);
            };
            result[0] = Shapes.or(result[0], Shapes.create(box));
        });
        return result[0].optimize();
    }
}
