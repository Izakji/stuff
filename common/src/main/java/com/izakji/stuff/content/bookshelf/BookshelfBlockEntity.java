package com.izakji.stuff.content.bookshelf;

import com.izakji.stuff.registry.StuffContentHolder;
import com.izakji.stuff.util.ContainerNbt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Single owner for all 40 shelf slots; the upper half only delegates here.
 *
 * @author Izakji
 */
public final class BookshelfBlockEntity extends BlockEntity {
    private static final String BOOKS_TAG = "Books";
    private final SimpleContainer bookInventory = new SimpleContainer(BookshelfSlots.TOTAL_SLOTS) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.is(ItemTags.BOOKSHELF_BOOKS);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    };

    public BookshelfBlockEntity(BlockPos pos, BlockState state) {
        super(StuffContentHolder.get().bookshelfBlockEntity(), pos, state);
        bookInventory.addListener(container -> setChanged());
    }

    public ItemStack bookAt(int bookIndex) {
        return bookInventory.getItem(bookIndex);
    }

    public boolean insertBook(int bookIndex, ItemStack stack, @Nullable Player player) {
        if (level == null
                || level.isClientSide
                || bookIndex < 0
                || bookIndex >= BookshelfSlots.TOTAL_SLOTS
                || !bookAt(bookIndex).isEmpty()
                || !stack.is(ItemTags.BOOKSHELF_BOOKS)) return false;
        bookInventory.setItem(bookIndex, stack.copyWithCount(1));
        if (player == null) stack.shrink(1);
        else if (!player.isCreative()) stack.consume(1, player);
        sync();
        return true;
    }

    public ItemStack removeBook(int bookIndex, @Nullable Player player) {
        if (level == null
                || level.isClientSide
                || bookIndex < 0
                || bookIndex >= BookshelfSlots.TOTAL_SLOTS
                || bookAt(bookIndex).isEmpty()) return ItemStack.EMPTY;
        ItemStack removed = bookInventory.removeItem(bookIndex, 1);
        if (player != null && !player.getInventory().add(removed.copy())) player.drop(removed.copy(), false);
        sync();
        return removed;
    }

    public void dropStoredBooks() {
        if (level == null || level.isClientSide) return;
        for (int slot = 0; slot < bookInventory.getContainerSize(); slot++) {
            ItemStack stack = bookInventory.getItem(slot);
            if (stack.isEmpty()) continue;
            Containers.dropItemStack(
                    level,
                    worldPosition.getX() + .5,
                    worldPosition.getY() + .5,
                    worldPosition.getZ() + .5,
                    stack.copy());
            bookInventory.removeItemNoUpdate(slot);
        }
        bookInventory.setChanged();
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(BOOKS_TAG, ContainerNbt.save(bookInventory, registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(BOOKS_TAG)) ContainerNbt.load(tag.getCompound(BOOKS_TAG), bookInventory, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
