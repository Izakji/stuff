package com.izakji.stuff;

import com.izakji.stuff.data.StuffData;
import com.izakji.stuff.platform.NeoForgePlatform;
import com.izakji.stuff.platform.StuffPlatformHolder;
import com.izakji.stuff.registry.ModContent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * NeoForge mod entrypoint.
 *
 * @author Izakji
 */
@Mod(Stuff.MOD_ID)
public final class Stuff {
    public static final String MOD_ID = StuffConstants.MOD_ID;

    public Stuff(IEventBus modBus) {
        StuffPlatformHolder.set(new NeoForgePlatform());
        ModContent.register(modBus);
        modBus.addListener(StuffData::gather);
    }
}
