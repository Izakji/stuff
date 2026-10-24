package com.izakji.stuff.client;

import com.izakji.stuff.Stuff;
import com.izakji.stuff.content.recordplayer.RecordPlayerBlockEntity;
import com.izakji.stuff.registry.ModContent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Shared circular mesh. Palette sampling happens once per sprite, never once per frame.
 *
 * @author Izakji
 */
public final class RoundRecordRenderer {
    private static final int SEGMENTS = 24;
    private static final float[] X = new float[SEGMENTS + 1];
    private static final float[] Z = new float[SEGMENTS + 1];
    private static final ResourceLocation SURFACE =
            ResourceLocation.fromNamespaceAndPath(Stuff.MOD_ID, "textures/misc/record_surface.png");
    private static final ResourceLocation HAPPY_DAYS_SURFACE =
            ResourceLocation.fromNamespaceAndPath(Stuff.MOD_ID, "textures/misc/happy_days_record.png");
    private static final Palette DEFAULT = new Palette(0x202225, 0xd4bb84);
    private static final Palette HAPPY_DAYS_PALETTE = new Palette(0x56D17F, 0xFF66E8);
    // Keys and colors only: no ItemStacks, worlds, native images, or baked models retained.
    private static final Map<ResourceLocation, Palette> PALETTES = new LinkedHashMap<>(32, .75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<ResourceLocation, Palette> entry) {
            return size() > 256;
        }
    };

    static {
        for (int i = 0; i <= SEGMENTS; i++) {
            double angle = i * Math.PI * 2 / SEGMENTS;
            X[i] = (float) Math.cos(angle);
            Z[i] = (float) Math.sin(angle);
        }
    }

    private RoundRecordRenderer() {}

    public static void clearPalettes() {
        PALETTES.clear();
    }

    public static void render(
            RecordPlayerBlockEntity record, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
        boolean happyDays = record.disc().is(ModContent.HAPPY_DAYS_DISC.get());
        var model = Minecraft.getInstance().getItemRenderer().getModel(record.disc(), record.getLevel(), null, 0);
        var sprite = model.getParticleIcon(ModelData.EMPTY);
        Palette palette = happyDays
                ? HAPPY_DAYS_PALETTE
                : model.isCustomRenderer()
                        ? DEFAULT
                        : PALETTES.computeIfAbsent(sprite.contents().name(), ignored -> sample(sprite));
        var pose = poses.last();
        if (happyDays) {
            // Original full-color pixel artwork preserves the label and purple midpoint ring.
            ring(
                    buffers.getBuffer(RenderType.entityCutoutNoCull(HAPPY_DAYS_SURFACE)),
                    pose,
                    .018F,
                    .263F,
                    .0125F,
                    0xffffff,
                    light,
                    overlay,
                    false);
        }
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(SURFACE));
        if (!happyDays) {
            // Hole, printed label, vinyl, a subtle groove ring, and the thin outside edge.
            ring(vertices, pose, .018F, .105F, .0135F, palette.label, light, overlay, true);
            ring(vertices, pose, .105F, .225F, .0125F, palette.vinyl, light, overlay, false);
            ring(vertices, pose, .225F, .229F, .0125F, shade(palette.vinyl, 1.22F), light, overlay, false);
            ring(vertices, pose, .229F, .263F, .0125F, palette.vinyl, light, overlay, false);
        }
        ring(vertices, pose, .018F, .263F, 0, shade(palette.vinyl, .65F), light, overlay, false);
        for (int i = 0; i < SEGMENTS; i++) {
            int color = shade(palette.vinyl, .65F);
            vertex(vertices, pose, .263F * X[i], 0, .263F * Z[i], color, light, overlay, X[i], 0, Z[i]);
            vertex(vertices, pose, .263F * X[i + 1], 0, .263F * Z[i + 1], color, light, overlay, X[i + 1], 0, Z[i + 1]);
            vertex(
                    vertices,
                    pose,
                    .263F * X[i + 1],
                    .0125F,
                    .263F * Z[i + 1],
                    color,
                    light,
                    overlay,
                    X[i + 1],
                    0,
                    Z[i + 1]);
            vertex(vertices, pose, .263F * X[i], .0125F, .263F * Z[i], color, light, overlay, X[i], 0, Z[i]);
        }
    }

    private static void ring(
            VertexConsumer out,
            PoseStack.Pose pose,
            float inner,
            float outer,
            float y,
            int color,
            int light,
            int overlay,
            boolean label) {
        for (int i = 0; i < SEGMENTS; i++) {
            // A small printed sector makes the rotation readable without another texture.
            int tint = label && i < 3 ? shade(color, .62F) : color;
            vertex(out, pose, inner * X[i], y, inner * Z[i], tint, light, overlay, 0, 1, 0);
            vertex(out, pose, inner * X[i + 1], y, inner * Z[i + 1], tint, light, overlay, 0, 1, 0);
            vertex(out, pose, outer * X[i + 1], y, outer * Z[i + 1], tint, light, overlay, 0, 1, 0);
            vertex(out, pose, outer * X[i], y, outer * Z[i], tint, light, overlay, 0, 1, 0);
        }
    }

    private static void vertex(
            VertexConsumer out,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            int color,
            int light,
            int overlay,
            float nx,
            float ny,
            float nz) {
        out.addVertex(pose.pose(), x, y, z)
                .setColor((color >> 16) & 255, (color >> 8) & 255, color & 255, 255)
                .setUv(.5F + x / .526F, .5F + z / .526F)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    private static Palette sample(TextureAtlasSprite sprite) {
        if (sprite.contents().name().getPath().equals("missingno")) return DEFAULT;
        // NativeImage stores ABGR. Center = printed label; surrounding pixels = vinyl material.
        long[] label = new long[4];
        long[] vinyl = new long[4];
        int width = sprite.contents().width(), height = sprite.contents().height();
        for (int y = 0; y < 12; y++) {
            for (int x = 0; x < 12; x++) {
                float u = .15F + x * .7F / 11, v = .2F + y * .6F / 11;
                int pixel = sprite.getPixelRGBA(
                        0, Math.min(width - 1, (int) (u * width)), Math.min(height - 1, (int) (v * height)));
                if ((pixel >>> 24) < 192) continue;
                int r = pixel & 255, g = (pixel >> 8) & 255, b = (pixel >> 16) & 255;
                int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
                boolean center = u > .35F && u < .65F && v > .30F && v < .62F;
                if (center && max > 85 && max - min > 25) accumulate(label, r, g, b);
                if (!center && max > 12 && max < 180) accumulate(vinyl, r, g, b);
            }
        }
        return new Palette(average(vinyl, DEFAULT.vinyl), average(label, DEFAULT.label));
    }

    private static void accumulate(long[] total, int r, int g, int b) {
        total[0] += r;
        total[1] += g;
        total[2] += b;
        total[3]++;
    }

    private static int average(long[] total, int fallback) {
        return total[3] == 0
                ? fallback
                : ((int) (total[0] / total[3]) << 16)
                        | ((int) (total[1] / total[3]) << 8)
                        | (int) (total[2] / total[3]);
    }

    private static int shade(int color, float factor) {
        return (Math.min(255, (int) (((color >> 16) & 255) * factor)) << 16)
                | (Math.min(255, (int) (((color >> 8) & 255) * factor)) << 8)
                | Math.min(255, (int) ((color & 255) * factor));
    }

    private record Palette(int vinyl, int label) {}
}
