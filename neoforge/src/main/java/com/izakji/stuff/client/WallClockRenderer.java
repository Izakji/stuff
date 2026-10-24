package com.izakji.stuff.client;

import com.izakji.stuff.Stuff;
import com.izakji.stuff.content.clock.ClockTime;
import com.izakji.stuff.content.clock.WallClockBlock;
import com.izakji.stuff.content.clock.WallClockBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Only the two hands are dynamic. Reads the client's already synchronized world clock.
 *
 * @author Izakji
 */
public final class WallClockRenderer implements BlockEntityRenderer<WallClockBlockEntity> {
    public static final ResourceLocation HOUR =
            ResourceLocation.fromNamespaceAndPath(Stuff.MOD_ID, "block/wall_clock/hour");
    public static final ResourceLocation MINUTE =
            ResourceLocation.fromNamespaceAndPath(Stuff.MOD_ID, "block/wall_clock/minute");
    private static final ModelResourceLocation HOUR_MODEL = ModelResourceLocation.standalone(HOUR);
    private static final ModelResourceLocation MINUTE_MODEL = ModelResourceLocation.standalone(MINUTE);
    private long sampledTick = Long.MIN_VALUE;
    private long sampledDay = Long.MIN_VALUE;
    private boolean advancing;

    public WallClockRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            WallClockBlockEntity clock,
            float partialTick,
            PoseStack poses,
            MultiBufferSource buffers,
            int light,
            int overlay) {
        var level = clock.getLevel();
        if (level == null) return;
        long day = level.getDayTime();
        long tick = level.getGameTime();
        if (tick != sampledTick || day != sampledDay) {
            advancing = day != sampledDay;
            sampledTick = tick;
            sampledDay = day;
        }
        // Detect a stopped day clock without relying on client gamerule synchronization.
        float fraction = advancing ? partialTick : 0;
        poses.pushPose();
        poses.translate(.5, 0, .5);
        poses.mulPose(Axis.YP.rotationDegrees(
                -clock.getBlockState().getValue(WallClockBlock.FACING).toYRot()));
        poses.translate(-.5, 0, -.5);
        hand(clock, HOUR_MODEL, ClockTime.hourAngle(day, fraction), poses, buffers, light, overlay);
        hand(clock, MINUTE_MODEL, ClockTime.minuteAngle(day, fraction), poses, buffers, light, overlay);
        poses.popPose();
    }

    private static void hand(
            WallClockBlockEntity clock,
            ModelResourceLocation id,
            float angle,
            PoseStack poses,
            MultiBufferSource buffers,
            int light,
            int overlay) {
        poses.pushPose();
        poses.translate(.5, .5, 0);
        poses.mulPose(Axis.ZP.rotationDegrees(-angle));
        poses.translate(-.5, -.5, 0);
        var minecraft = Minecraft.getInstance();
        var model = minecraft.getModelManager().getModel(id);
        minecraft
                .getBlockRenderer()
                .getModelRenderer()
                .renderModel(
                        poses.last(),
                        buffers.getBuffer(RenderType.solid()),
                        clock.getBlockState(),
                        model,
                        1,
                        1,
                        1,
                        light,
                        overlay,
                        ModelData.EMPTY,
                        RenderType.solid());
        poses.popPose();
    }

    @Override
    public int getViewDistance() {
        return 48;
    }
}
