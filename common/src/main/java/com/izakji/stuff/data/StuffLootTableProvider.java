package com.izakji.stuff.data;

import com.izakji.stuff.content.bookshelf.BookshelfBlock;
import com.izakji.stuff.content.desk.StudioDeskBlock;
import com.izakji.stuff.content.worktable.WorktableBlock;
import com.izakji.stuff.registry.StuffContentHolder;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

/**
 * Common loot tables for the furniture family.
 *
 * @author Izakji
 */
public final class StuffLootTableProvider extends LootTableProvider {
    public StuffLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, Set.of(), List.of(new SubProviderEntry(StuffLoot::new, LootContextParamSets.BLOCK)), lookup);
    }

    private static final class StuffLoot implements LootTableSubProvider {
        private StuffLoot(HolderLookup.Provider lookup) {}

        @Override
        public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
            Block worktable = StuffContentHolder.get().worktable();
            Block bookshelf = StuffContentHolder.get().oakStudioBookshelf();
            Block clock = StuffContentHolder.get().oakWallClock();
            Block recordPlayer = StuffContentHolder.get().oakRecordPlayer();
            Block chair = StuffContentHolder.get().oakStudioChair();
            Block desk = StuffContentHolder.get().oakStudioDesk();

            output.accept(worktable.getLootTable(), lowerHalf(worktable, WorktableBlock.HALF));
            output.accept(bookshelf.getLootTable(), lowerHalf(bookshelf, BookshelfBlock.HALF));
            output.accept(clock.getLootTable(), self(clock));
            output.accept(recordPlayer.getLootTable(), self(recordPlayer));
            output.accept(chair.getLootTable(), self(chair));
            output.accept(desk.getLootTable(), deskOwner(desk));
        }

        private static LootTable.Builder self(Block block) {
            return LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(block).when(ExplosionCondition.survivesExplosion())));
        }

        private static LootTable.Builder lowerHalf(Block block, EnumProperty<DoubleBlockHalf> halfProperty) {
            return LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                                    .setProperties(StatePropertiesPredicate.Builder.properties()
                                            .hasProperty(halfProperty, DoubleBlockHalf.LOWER)))
                            .add(LootItem.lootTableItem(block).when(ExplosionCondition.survivesExplosion())));
        }

        private static LootTable.Builder deskOwner(Block desk) {
            return LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(desk)
                                    .setProperties(StatePropertiesPredicate.Builder.properties()
                                            .hasProperty(StudioDeskBlock.RIGHT, false)))
                            .add(LootItem.lootTableItem(desk).when(ExplosionCondition.survivesExplosion())));
        }
    }
}
