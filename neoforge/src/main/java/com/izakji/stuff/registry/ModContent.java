package com.izakji.stuff.registry;

import com.izakji.stuff.Stuff;
import com.izakji.stuff.content.bookshelf.BookshelfBlock;
import com.izakji.stuff.content.bookshelf.BookshelfBlockEntity;
import com.izakji.stuff.content.bookshelf.NeoForgeBookshelfBlock;
import com.izakji.stuff.content.chair.ChairSeatEntity;
import com.izakji.stuff.content.chair.NeoForgeStudioChairBlock;
import com.izakji.stuff.content.chair.StudioChairBlock;
import com.izakji.stuff.content.clock.WallClockBlock;
import com.izakji.stuff.content.clock.WallClockBlockEntity;
import com.izakji.stuff.content.desk.DrawerMenu;
import com.izakji.stuff.content.desk.NeoForgeStudioDeskBlock;
import com.izakji.stuff.content.desk.StudioDeskBlock;
import com.izakji.stuff.content.desk.StudioDeskBlockEntity;
import com.izakji.stuff.content.recordplayer.NeoForgeRecordPlayerBlock;
import com.izakji.stuff.content.recordplayer.RecordPlayerBlock;
import com.izakji.stuff.content.recordplayer.RecordPlayerBlockEntity;
import com.izakji.stuff.content.worktable.NeoForgeWorktableBlock;
import com.izakji.stuff.content.worktable.WorktableBlock;
import com.izakji.stuff.content.worktable.WorktableMenu;
import com.izakji.stuff.content.worktable.WorktableRecipe;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * NeoForge registration for the mod's first furniture family.
 *
 * @author Izakji
 */
public final class ModContent implements StuffContent {
    public static final ResourceKey<JukeboxSong> HAPPY_DAYS_SONG =
            ResourceKey.create(Registries.JUKEBOX_SONG, id("happy_days"));
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Stuff.MOD_ID);
    public static final DeferredHolder<SoundEvent, SoundEvent> HAPPY_DAYS_SOUND = SOUNDS.register(
            "music_disc.happy_days", () -> SoundEvent.createVariableRangeEvent(id("music_disc.happy_days")));
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Stuff.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Stuff.MOD_ID);
    public static final DeferredHolder<RecipeType<?>, RecipeType<WorktableRecipe>> WORKTABLE_RECIPES =
            RECIPE_TYPES.register("worktable", () -> RecipeType.<WorktableRecipe>simple(id("worktable")));
    public static final DeferredHolder<RecipeSerializer<?>, WorktableRecipe.Serializer> WORKTABLE_SERIALIZER =
            RECIPE_SERIALIZERS.register("worktable", WorktableRecipe.Serializer::new);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Stuff.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Stuff.MOD_ID);
    public static final DeferredItem<Item> HAPPY_DAYS_DISC = ITEMS.registerSimpleItem(
            "music_disc_happy_days",
            new Item.Properties().stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(HAPPY_DAYS_SONG));
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Stuff.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Stuff.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Stuff.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Stuff.MOD_ID);

    public static final DeferredBlock<WorktableBlock> WORKTABLE =
            BLOCKS.register("worktable", () -> new NeoForgeWorktableBlock(woodenFurniture(2.5F)));
    public static final DeferredItem<BlockItem> WORKTABLE_ITEM = ITEMS.registerSimpleBlockItem(WORKTABLE);
    public static final DeferredHolder<MenuType<?>, MenuType<WorktableMenu>> WORKTABLE_MENU =
            MENUS.register("worktable", () -> IMenuTypeExtension.create(WorktableMenu::new));

    public static final DeferredBlock<StudioDeskBlock> OAK_STUDIO_DESK =
            BLOCKS.register("oak_studio_desk", () -> new NeoForgeStudioDeskBlock(woodenFurniture(2.5F)));
    public static final DeferredItem<BlockItem> OAK_STUDIO_DESK_ITEM = ITEMS.registerSimpleBlockItem(OAK_STUDIO_DESK);
    public static final DeferredBlock<StudioChairBlock> OAK_STUDIO_CHAIR =
            BLOCKS.register("oak_studio_chair", () -> new NeoForgeStudioChairBlock(woodenFurniture(2.5F)));
    public static final DeferredItem<BlockItem> OAK_STUDIO_CHAIR_ITEM = ITEMS.registerSimpleBlockItem(OAK_STUDIO_CHAIR);
    public static final DeferredBlock<RecordPlayerBlock> OAK_RECORD_PLAYER =
            BLOCKS.register("oak_record_player", () -> new NeoForgeRecordPlayerBlock(woodenFurniture(2.5F)));
    public static final DeferredItem<BlockItem> OAK_RECORD_PLAYER_ITEM =
            ITEMS.registerSimpleBlockItem(OAK_RECORD_PLAYER);
    public static final DeferredBlock<BookshelfBlock> OAK_STUDIO_BOOKSHELF =
            BLOCKS.register("oak_studio_bookshelf", () -> new NeoForgeBookshelfBlock(woodenFurniture(2.5F)));
    public static final DeferredItem<BlockItem> OAK_STUDIO_BOOKSHELF_ITEM =
            ITEMS.registerSimpleBlockItem(OAK_STUDIO_BOOKSHELF);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BookshelfBlockEntity>> BOOKSHELF_ENTITY =
            BLOCK_ENTITIES.register(
                    "bookshelf",
                    () -> BlockEntityType.Builder.of(BookshelfBlockEntity::new, OAK_STUDIO_BOOKSHELF.get())
                            .build(null));
    public static final DeferredBlock<WallClockBlock> OAK_WALL_CLOCK = BLOCKS.register(
            "oak_wall_clock",
            () -> new WallClockBlock(woodenFurniture(1.0F).noCollission().pushReaction(PushReaction.DESTROY)));
    public static final DeferredItem<BlockItem> OAK_WALL_CLOCK_ITEM = ITEMS.registerSimpleBlockItem(OAK_WALL_CLOCK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WallClockBlockEntity>> WALL_CLOCK_ENTITY =
            BLOCK_ENTITIES.register(
                    "wall_clock",
                    () -> BlockEntityType.Builder.of(WallClockBlockEntity::new, OAK_WALL_CLOCK.get())
                            .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RecordPlayerBlockEntity>>
            RECORD_PLAYER_ENTITY = BLOCK_ENTITIES.register(
                    "record_player",
                    () -> BlockEntityType.Builder.of(RecordPlayerBlockEntity::new, OAK_RECORD_PLAYER.get())
                            .build(null));
    public static final DeferredHolder<EntityType<?>, EntityType<ChairSeatEntity>> CHAIR_SEAT = ENTITIES.register(
            "chair_seat",
            () -> EntityType.Builder.<ChairSeatEntity>of(ChairSeatEntity::new, MobCategory.MISC)
                    .sized(.1F, .1F)
                    .noSave()
                    .noSummon()
                    .clientTrackingRange(8)
                    .updateInterval(20)
                    .build(id("chair_seat").toString()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StudioDeskBlockEntity>> DESK_ENTITY =
            BLOCK_ENTITIES.register(
                    "studio_desk",
                    () -> BlockEntityType.Builder.of(StudioDeskBlockEntity::new, OAK_STUDIO_DESK.get())
                            .build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<DrawerMenu>> DRAWER_MENU =
            MENUS.register("desk_drawer", () -> IMenuTypeExtension.create(DrawerMenu::new));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> STUFF_TAB = TABS.register(
            "stuff",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.stuff"))
                    .icon(() -> OAK_STUDIO_DESK_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(WORKTABLE_ITEM.get());
                        output.accept(OAK_STUDIO_DESK_ITEM.get());
                        output.accept(OAK_STUDIO_CHAIR_ITEM.get());
                        output.accept(OAK_STUDIO_BOOKSHELF_ITEM.get());
                        output.accept(OAK_RECORD_PLAYER_ITEM.get());
                        output.accept(OAK_WALL_CLOCK_ITEM.get());
                        output.accept(HAPPY_DAYS_DISC.get());
                    })
                    .build());

    private static final ModContent INSTANCE = new ModContent();

    static {
        StuffContentHolder.set(INSTANCE);
    }

    @Override
    public Block worktable() {
        return WORKTABLE.get();
    }

    @Override
    public Item worktableItem() {
        return WORKTABLE_ITEM.get();
    }

    @Override
    public Block oakStudioDesk() {
        return OAK_STUDIO_DESK.get();
    }

    @Override
    public Item oakStudioDeskItem() {
        return OAK_STUDIO_DESK_ITEM.get();
    }

    @Override
    public Block oakStudioChair() {
        return OAK_STUDIO_CHAIR.get();
    }

    @Override
    public Item oakStudioChairItem() {
        return OAK_STUDIO_CHAIR_ITEM.get();
    }

    @Override
    public Block oakRecordPlayer() {
        return OAK_RECORD_PLAYER.get();
    }

    @Override
    public Item oakRecordPlayerItem() {
        return OAK_RECORD_PLAYER_ITEM.get();
    }

    @Override
    public Block oakStudioBookshelf() {
        return OAK_STUDIO_BOOKSHELF.get();
    }

    @Override
    public Item oakStudioBookshelfItem() {
        return OAK_STUDIO_BOOKSHELF_ITEM.get();
    }

    @Override
    public Block oakWallClock() {
        return OAK_WALL_CLOCK.get();
    }

    @Override
    public Item oakWallClockItem() {
        return OAK_WALL_CLOCK_ITEM.get();
    }

    @Override
    public Item happyDaysDisc() {
        return HAPPY_DAYS_DISC.get();
    }

    @Override
    public BlockEntityType<BookshelfBlockEntity> bookshelfBlockEntity() {
        return BOOKSHELF_ENTITY.get();
    }

    @Override
    public BlockEntityType<WallClockBlockEntity> wallClockBlockEntity() {
        return WALL_CLOCK_ENTITY.get();
    }

    @Override
    public BlockEntityType<RecordPlayerBlockEntity> recordPlayerBlockEntity() {
        return RECORD_PLAYER_ENTITY.get();
    }

    @Override
    public BlockEntityType<StudioDeskBlockEntity> studioDeskBlockEntity() {
        return DESK_ENTITY.get();
    }

    @Override
    public MenuType<WorktableMenu> worktableMenu() {
        return WORKTABLE_MENU.get();
    }

    @Override
    public MenuType<DrawerMenu> drawerMenu() {
        return DRAWER_MENU.get();
    }

    @Override
    public EntityType<ChairSeatEntity> chairSeat() {
        return CHAIR_SEAT.get();
    }

    @Override
    public RecipeType<WorktableRecipe> worktableRecipeType() {
        return WORKTABLE_RECIPES.get();
    }

    @Override
    public RecipeSerializer<WorktableRecipe> worktableRecipeSerializer() {
        return WORKTABLE_SERIALIZER.get();
    }

    @Override
    public List<Block> knownBlocks() {
        return BLOCKS.getEntries().stream().map(holder -> (Block) holder.get()).toList();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Stuff.MOD_ID, path);
    }

    private static BlockBehaviour.Properties woodenFurniture(float strength) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(strength)
                .sound(SoundType.WOOD)
                .noOcclusion();
    }

    private ModContent() {}

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
        RECIPE_TYPES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        ENTITIES.register(modBus);
        TABS.register(modBus);
    }
}
