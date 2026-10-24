package com.izakji.stuff.content.bookshelf;

import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Empty 5-shelf bookcase. One lower-half owner; all edits are direct slot interactions.
 *
 * @author Izakji
 */
public class BookshelfBlock extends BaseEntityBlock {
    public static final MapCodec<BookshelfBlock> CODEC = simpleCodec(BookshelfBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    private static final Map<Direction, VoxelShape> SHAPES = createShapes();

    public BookshelfBlock(Properties properties) {
        super(properties);
        registerDefaultState(
                stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
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
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new BookshelfBlockEntity(pos, state) : null;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSpectator()
                || hit.getDirection() != state.getValue(FACING)
                || !player.getMainHandItem().isEmpty()
                || !player.getOffhandItem().isEmpty()) return InteractionResult.PASS;
        BlockPos owner = owner(pos, state);
        BookshelfBlockEntity shelf = shelf(level, owner);
        if (shelf == null) return InteractionResult.PASS;
        int bookIndex = slotIndexAtHit(hit, owner, shelf, state.getValue(FACING));
        if (bookIndex < 0 || shelf.bookAt(bookIndex).isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide && shelf.removeBook(bookIndex, player).isEmpty()) return InteractionResult.PASS;
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (player.isSpectator() || hit.getDirection() != state.getValue(FACING) || !stack.is(ItemTags.BOOKSHELF_BOOKS))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        BlockPos owner = owner(pos, state);
        BookshelfBlockEntity shelf = shelf(level, owner);
        if (shelf == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        int bookIndex = slotIndexAtHit(hit, owner, shelf, state.getValue(FACING));
        if (bookIndex < 0) return ItemInteractionResult.FAIL;
        boolean inserted = level.isClientSide || shelf.insertBook(bookIndex, stack, player);
        return inserted ? ItemInteractionResult.sidedSuccess(level.isClientSide) : ItemInteractionResult.FAIL;
    }

    private static @Nullable BookshelfBlockEntity shelf(Level level, BlockPos owner) {
        return level.getBlockEntity(owner) instanceof BookshelfBlockEntity shelf ? shelf : null;
    }

    /**
     * Resolves a block hit to one shelf slot.
     *
     * <p>The visible packed-book position is used first, then the raw shelf cell so an empty cell
     * can still accept a book. Removal shares this mapping; a click in the gap between books
     * therefore resolves to the cell under the cursor.
     *
     * @param hit the block hit result
     * @param owner the lower-half owner position
     * @param shelf the bookshelf block entity
     * @param facing the block's facing direction
     * @return the global slot index
     */
    private static int slotIndexAtHit(
            BlockHitResult hit, BlockPos owner, BookshelfBlockEntity shelf, Direction facing) {
        Vec3 local = localHit(hit.getLocation().subtract(Vec3.atLowerCornerOf(owner)), facing);
        int shelfIndex = BookshelfSlots.shelfIndexAt(local.y * BookshelfSlots.MODEL_UNITS_PER_BLOCK);
        int visualSlot =
                BookshelfLayout.visualSlotAt(shelf, shelfIndex, local.x * BookshelfSlots.MODEL_UNITS_PER_BLOCK);
        int slot = visualSlot >= 0
                ? visualSlot
                : BookshelfSlots.slotIndexAt(local.x * BookshelfSlots.MODEL_UNITS_PER_BLOCK);
        return BookshelfSlots.slotIndex(shelfIndex, slot);
    }

    public static BlockPos owner(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    /**
     * Rotates a world-space point into the authored +Z-facing model space.
     *
     * @param point the world-space point relative to the block
     * @param facing the block's facing direction
     * @return the point in authored model space
     */
    public static Vec3 localHit(Vec3 point, Direction facing) {
        return switch (facing) {
            case NORTH -> new Vec3(1 - point.x, point.y, 1 - point.z);
            case WEST -> new Vec3(point.z, point.y, 1 - point.x);
            case EAST -> new Vec3(1 - point.z, point.y, point.x);
            default -> point;
        };
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative()) {
            BlockPos lower = owner(pos, state);
            if (level.getBlockState(lower).is(this)) level.setBlock(lower, Blocks.AIR.defaultBlockState(), UPDATE_ALL);
            BlockPos upper = lower.above();
            if (level.getBlockState(upper).is(this)) level.setBlock(upper, Blocks.AIR.defaultBlockState(), UPDATE_ALL);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!level.isClientSide && !state.is(replacement.getBlock())) {
            if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
                if (level.getBlockEntity(pos) instanceof BookshelfBlockEntity shelf) shelf.dropStoredBooks();
                BlockPos upper = pos.above();
                if (level.getBlockState(upper).is(this))
                    level.setBlock(upper, Blocks.AIR.defaultBlockState(), UPDATE_ALL);
            } else {
                BlockPos lower = pos.below();
                if (paired(state, level.getBlockState(lower))) level.destroyBlock(lower, true);
            }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    public static boolean paired(BlockState first, BlockState second) {
        return second.is(first.getBlock())
                && first.getValue(FACING) == second.getValue(FACING)
                && first.getValue(HALF) != second.getValue(HALF);
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
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    private static Map<Direction, VoxelShape> createShapes() {
        VoxelShape south = box(.5, 0, 0, 15.5, 16, 7);
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape[] rotated = {Shapes.empty()};
            south.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
                var bounds = switch (facing) {
                    case NORTH -> new AABB(1 - x2, y1, 1 - z2, 1 - x1, y2, 1 - z1);
                    case WEST -> new AABB(1 - z2, y1, x1, 1 - z1, y2, x2);
                    case EAST -> new AABB(z1, y1, 1 - x2, z2, y2, 1 - x1);
                    default -> new AABB(x1, y1, z1, x2, y2, z2);
                };
                rotated[0] = Shapes.or(rotated[0], Shapes.create(bounds));
            });
            shapes.put(facing, rotated[0].optimize());
        }
        return shapes;
    }
}
