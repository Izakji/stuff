package com.izakji.stuff.client;

import com.izakji.stuff.Stuff;
import com.izakji.stuff.content.recordplayer.RecordPlayerBlock;
import com.izakji.stuff.content.recordplayer.RecordPlayerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Only moving parts use a BER. Closed, empty players are entirely chunk-baked.
 *
 * @author Izakji
 */
public final class RecordPlayerRenderer implements BlockEntityRenderer<RecordPlayerBlockEntity> {
    public static final ResourceLocation LID =
            ResourceLocation.fromNamespaceAndPath(Stuff.MOD_ID, "block/record_player/lid");
    public static final ResourceLocation ARM =
            ResourceLocation.fromNamespaceAndPath(Stuff.MOD_ID, "block/record_player/arm");
    public static final ResourceLocation LID_FRAME =
            ResourceLocation.fromNamespaceAndPath(Stuff.MOD_ID, "block/record_player/lid_frame");
    private static final ModelResourceLocation LID_MODEL = ModelResourceLocation.standalone(LID);
    private static final ModelResourceLocation ARM_MODEL = ModelResourceLocation.standalone(ARM);
    private static final ModelResourceLocation LID_FRAME_MODEL = ModelResourceLocation.standalone(LID_FRAME);

    public RecordPlayerRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            RecordPlayerBlockEntity record,
            float partialTick,
            PoseStack poses,
            MultiBufferSource buffers,
            int light,
            int overlay) {
        if (record.phase() == RecordPlayerBlockEntity.Phase.IDLE) return;
        poses.pushPose();
        poses.translate(.5, 0, .5);
        poses.mulPose(Axis.YP.rotationDegrees(
                -record.getBlockState().getValue(RecordPlayerBlock.FACING).toYRot()));
        poses.translate(-.5, 0, -.5);

        poses.pushPose();
        poses.translate(.5, 14.5 / 16, 1.0 / 16);
        poses.mulPose(Axis.XP.rotationDegrees(-85 * record.lidProgress(partialTick)));
        poses.translate(-.5, -14.5 / 16, -1.0 / 16);
        renderPart(record, LID_FRAME_MODEL, poses, buffers, light, overlay, RenderType.cutout());
        renderPart(record, LID_MODEL, poses, buffers, light, overlay, RenderType.translucent());
        poses.popPose();

        poses.pushPose();
        poses.translate(13.0 / 16, 15.0 / 16, 4.0 / 16);
        poses.mulPose(Axis.YP.rotationDegrees(-35 * record.armProgress(partialTick)));
        poses.translate(-13.0 / 16, -15.0 / 16, -4.0 / 16);
        renderPart(record, ARM_MODEL, poses, buffers, light, overlay, RenderType.cutout());
        poses.popPose();

        if (!record.disc().isEmpty()
                && (record.phase() != RecordPlayerBlockEntity.Phase.INSERTING || record.elapsed(partialTick) >= 12)) {
            poses.pushPose();
            poses.translate(6.5 / 16, 14.35 / 16 + record.discLift(partialTick), 7.5 / 16);
            if (record.phase() == RecordPlayerBlockEntity.Phase.PLAYING) {
                poses.mulPose(Axis.YP.rotationDegrees((record.elapsed(partialTick) * 6) % 360));
            }
            RoundRecordRenderer.render(record, poses, buffers, light, overlay);
            poses.popPose();
        }
        poses.popPose();
    }

    private static void renderPart(
            RecordPlayerBlockEntity record,
            ModelResourceLocation id,
            PoseStack poses,
            MultiBufferSource buffers,
            int light,
            int overlay,
            RenderType layer) {
        var minecraft = Minecraft.getInstance();
        // Resolve through ModelManager: never retain a BakedModel across resource reloads.
        var model = minecraft.getModelManager().getModel(id);
        minecraft
                .getBlockRenderer()
                .getModelRenderer()
                .renderModel(
                        poses.last(),
                        buffers.getBuffer(layer),
                        record.getBlockState(),
                        model,
                        1,
                        1,
                        1,
                        light,
                        overlay,
                        ModelData.EMPTY,
                        layer);
    }

    @Override
    public int getViewDistance() {
        return 48;
    }

    @Override
    public AABB getRenderBoundingBox(RecordPlayerBlockEntity record) {
        return new AABB(record.getBlockPos()).expandTowards(0, 1, 0);
    }
}
