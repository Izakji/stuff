package com.izakji.stuff.data;

import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Registers independent client and server data providers for runData.
 *
 * @author Izakji
 */
public final class StuffData {
    private StuffData() {}

    public static void gather(GatherDataEvent event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();
        var lookup = event.getLookupProvider();
        var existingFiles = event.getExistingFileHelper();
        if (event.includeClient()) {
            generator.addProvider(true, new StuffBlockStateProvider(output, existingFiles));
        }
        generator.addProvider(event.includeServer(), new StuffRecipeProvider(output, lookup));
        generator.addProvider(event.includeServer(), new StuffLootTableProvider(output, lookup));
        generator.addProvider(event.includeServer(), new StuffBlockTagsProvider(output, lookup, existingFiles));
    }
}
