package com.izakji.stuff.registry;

import com.izakji.stuff.content.bookshelf.BookshelfBlockEntity;
import com.izakji.stuff.content.chair.ChairSeatEntity;
import com.izakji.stuff.content.clock.WallClockBlockEntity;
import com.izakji.stuff.content.desk.DrawerMenu;
import com.izakji.stuff.content.desk.StudioDeskBlockEntity;
import com.izakji.stuff.content.recordplayer.RecordPlayerBlockEntity;
import com.izakji.stuff.content.worktable.WorktableMenu;
import com.izakji.stuff.content.worktable.WorktableRecipe;
import java.util.List;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Loader-neutral access to registered Stuff content. The loader module installs
 * the concrete registry during mod construction.
 *
 * @author Izakji
 */
public interface StuffContent {
    Block worktable();

    Item worktableItem();

    Block oakStudioDesk();

    Item oakStudioDeskItem();

    Block oakStudioChair();

    Item oakStudioChairItem();

    Block oakRecordPlayer();

    Item oakRecordPlayerItem();

    Block oakStudioBookshelf();

    Item oakStudioBookshelfItem();

    Block oakWallClock();

    Item oakWallClockItem();

    Item happyDaysDisc();

    BlockEntityType<BookshelfBlockEntity> bookshelfBlockEntity();

    BlockEntityType<WallClockBlockEntity> wallClockBlockEntity();

    BlockEntityType<RecordPlayerBlockEntity> recordPlayerBlockEntity();

    BlockEntityType<StudioDeskBlockEntity> studioDeskBlockEntity();

    MenuType<WorktableMenu> worktableMenu();

    MenuType<DrawerMenu> drawerMenu();

    EntityType<ChairSeatEntity> chairSeat();

    RecipeType<WorktableRecipe> worktableRecipeType();

    RecipeSerializer<WorktableRecipe> worktableRecipeSerializer();

    List<Block> knownBlocks();
}
