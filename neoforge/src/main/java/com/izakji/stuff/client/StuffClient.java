package com.izakji.stuff.client;

import com.izakji.stuff.Stuff;
import com.izakji.stuff.registry.ModContent;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client-only NeoForge entrypoint for screens, renderers, and standalone models.
 *
 * @author Izakji
 */
@Mod(value = Stuff.MOD_ID, dist = Dist.CLIENT)
public final class StuffClient {
    public StuffClient(IEventBus modBus) {
        modBus.addListener(StuffClient::screens);
        modBus.addListener(StuffClient::renderers);
        modBus.addListener(StuffClient::models);
        modBus.addListener(StuffClient::modelsReloaded);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModContent.BOOKSHELF_ENTITY.get(), BookshelfRenderer::new);
        event.registerBlockEntityRenderer(ModContent.WALL_CLOCK_ENTITY.get(), WallClockRenderer::new);
        event.registerBlockEntityRenderer(ModContent.RECORD_PLAYER_ENTITY.get(), RecordPlayerRenderer::new);
        event.registerEntityRenderer(ModContent.CHAIR_SEAT.get(), ChairSeatRenderer::new);
    }

    private static void models(ModelEvent.RegisterAdditional event) {
        for (ResourceLocation book : BookshelfRenderer.BOOK_RESOURCES) {
            event.register(ModelResourceLocation.standalone(book));
        }
        event.register(ModelResourceLocation.standalone(WallClockRenderer.HOUR));
        event.register(ModelResourceLocation.standalone(WallClockRenderer.MINUTE));
        event.register(ModelResourceLocation.standalone(RecordPlayerRenderer.LID));
        event.register(ModelResourceLocation.standalone(RecordPlayerRenderer.LID_FRAME));
        event.register(ModelResourceLocation.standalone(RecordPlayerRenderer.ARM));
    }

    private static void modelsReloaded(ModelEvent.BakingCompleted event) {
        RoundRecordRenderer.clearPalettes();
    }

    private static void screens(RegisterMenuScreensEvent event) {
        event.register(ModContent.WORKTABLE_MENU.get(), WorktableScreen::new);
        event.register(ModContent.DRAWER_MENU.get(), DrawerScreen::new);
    }
}
