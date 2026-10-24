package com.izakji.stuff.client;

import com.izakji.stuff.content.chair.ChairSeatEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * The riding anchor has no mesh; the chair is rendered in the chunk.
 *
 * @author Izakji
 */
public final class ChairSeatRenderer extends EntityRenderer<ChairSeatEntity> {
    public ChairSeatRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ChairSeatEntity seat) {
        return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    }
}
