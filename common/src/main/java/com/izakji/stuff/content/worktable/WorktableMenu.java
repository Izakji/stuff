package com.izakji.stuff.content.worktable;

import com.izakji.stuff.platform.StuffPlatformHolder;
import com.izakji.stuff.registry.StuffContentHolder;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Provides the WorktableMenu implementation.
 *
 * @author Izakji
 */
public final class WorktableMenu extends AbstractContainerMenu {
    public static final int INPUTS = WorktableRecipe.MAX_INGREDIENTS;
    public static final int OUTPUT = INPUTS;
    public static final int PLAYER_START = OUTPUT + 1;
    public static final int CRAFT_BUTTON = 0;
    private static final int INPUT_GRID_X = 155;
    private static final int INPUT_GRID_Y = 51;
    private static final int INPUT_COLUMNS = 3;
    private static final int INPUT_COLUMN_SPACING = 45;
    private static final int INPUT_ROW_SPACING = 22;
    private static final int OUTPUT_SLOT_X = 211;
    private static final int OUTPUT_SLOT_Y = 121;
    private static final int PLAYER_GRID_X = 69;
    private static final int PLAYER_GRID_Y = 155;
    private static final int HOTBAR_Y = 213;
    private static final int PLAYER_COLUMNS = 9;
    private static final int SLOT_SIZE = 18;
    private final SimpleContainer inputs = new SimpleContainer(INPUTS);
    private final SimpleContainer output = new SimpleContainer(1);
    private final DataSlot selection = DataSlot.standalone();
    private final ContainerLevelAccess access;
    private final Level level;
    private final List<RecipeHolder<WorktableRecipe>> catalogue;
    private final RecipeInput recipeInput = new RecipeInput() {
        @Override
        public ItemStack getItem(int slot) {
            return inputs.getItem(slot);
        }

        @Override
        public int size() {
            return INPUTS;
        }
    };

    public WorktableMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos());
    }

    public WorktableMenu(int id, Inventory inventory, BlockPos pos) {
        super(StuffContentHolder.get().worktableMenu(), id);
        level = inventory.player.level();
        access = ContainerLevelAccess.create(level, pos);
        catalogue = level
                .getRecipeManager()
                .getAllRecipesFor(StuffContentHolder.get().worktableRecipeType())
                .stream()
                .sorted(Comparator.comparing((RecipeHolder<WorktableRecipe> holder) ->
                                holder.value().category())
                        .thenComparing(holder -> holder.id().toString()))
                .toList();
        selection.set(-1);
        addDataSlot(selection);
        for (int index = 0; index < INPUTS; index++) {
            final int slotIndex = index;
            addSlot(
                    new Slot(
                            inputs,
                            index,
                            INPUT_GRID_X + index % INPUT_COLUMNS * INPUT_COLUMN_SPACING,
                            INPUT_GRID_Y + index / INPUT_COLUMNS * INPUT_ROW_SPACING) {
                        @Override
                        public boolean isActive() {
                            return selected() != null
                                    && slotIndex < selected().materials().size();
                        }

                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return isActive()
                                    && selected()
                                            .materials()
                                            .get(slotIndex)
                                            .ingredient()
                                            .test(stack);
                        }
                    });
        }
        addSlot(new Slot(output, 0, OUTPUT_SLOT_X, OUTPUT_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < PLAYER_COLUMNS; column++) {
                addSlot(new Slot(
                        inventory,
                        column + row * PLAYER_COLUMNS + PLAYER_COLUMNS,
                        PLAYER_GRID_X + column * SLOT_SIZE,
                        PLAYER_GRID_Y + row * SLOT_SIZE));
            }
        }
        for (int column = 0; column < PLAYER_COLUMNS; column++) {
            addSlot(new Slot(inventory, column, PLAYER_GRID_X + column * SLOT_SIZE, HOTBAR_Y));
        }
    }

    public List<RecipeHolder<WorktableRecipe>> catalogue() {
        return catalogue;
    }

    public int selectedIndex() {
        return selection.get();
    }

    @Nullable
    public WorktableRecipe selected() {
        int index = selection.get();
        return index >= 0 && index < catalogue.size() ? catalogue.get(index).value() : null;
    }

    public boolean canCraft() {
        return selected() != null && output.isEmpty() && selected().matches(recipeInput, level);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, StuffContentHolder.get().worktable());
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (level.isClientSide || player.isSpectator() || !stillValid(player)) return false;
        if (button > 0 && button <= catalogue.size()) {
            int index = button - 1;
            if (selection.get() != index) {
                returnContents(player, inputs);
                returnContents(player, output);
                selection.set(index);
                broadcastChanges();
            }
            return true;
        }
        if (button != CRAFT_BUTTON || selected() == null || !output.isEmpty()) return false;
        // Revalidate the server snapshot after a datapack reload, rather than crafting a removed recipe.
        var current = level.getRecipeManager()
                .byKey(catalogue.get(selection.get()).id())
                .orElse(null);
        if (current == null || current.value() != selected() || !canCraft()) return false;
        WorktableRecipe recipe = selected();
        ItemStack crafted = recipe.assemble(recipeInput, level.registryAccess());
        for (int i = 0; i < recipe.materials().size(); i++) {
            int count = recipe.materials().get(i).count();
            ItemStack consumed = inputs.removeItem(i, count);
            if (StuffPlatformHolder.get().hasCraftingRemainingItem(consumed)) {
                for (int n = 0; n < count; n++) {
                    giveBack(player, StuffPlatformHolder.get().craftingRemainingItem(consumed));
                }
            }
        }
        returnContents(player, inputs);
        output.setItem(0, crafted);
        broadcastChanges();
        return true;
    }

    private static void giveBack(Player player, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!player.isAlive() || player instanceof ServerPlayer serverPlayer && serverPlayer.hasDisconnected()) {
            player.drop(stack, false);
        } else if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private static void returnContents(Player player, SimpleContainer container) {
        for (int i = 0; i < container.getContainerSize(); i++) giveBack(player, container.removeItemNoUpdate(i));
        container.setChanged();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!level.isClientSide) {
            returnContents(player, inputs);
            returnContents(player, output);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // Restrict each move to a matching slot; vanilla merging can bypass Slot.mayPlace.
            WorktableRecipe recipe = selected();
            if (recipe == null) return ItemStack.EMPTY;
            for (int i = 0; i < recipe.materials().size() && !stack.isEmpty(); i++)
                if (recipe.materials().get(i).ingredient().test(stack)) moveItemStackTo(stack, i, i + 1, false);
            if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
