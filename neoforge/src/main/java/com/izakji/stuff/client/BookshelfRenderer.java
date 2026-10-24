package com.izakji.stuff.client;

import com.izakji.stuff.Stuff;
import com.izakji.stuff.content.bookshelf.BookshelfBlock;
import com.izakji.stuff.content.bookshelf.BookshelfBlockEntity;
import com.izakji.stuff.content.bookshelf.BookshelfLayout;
import com.izakji.stuff.content.bookshelf.BookshelfSlots;
import com.izakji.stuff.content.bookshelf.BookshelfVariants;
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
 * Static case is chunk-baked; only occupied shelf slots submit book models.
 *
 * @author Izakji
 */
public final class BookshelfRenderer implements BlockEntityRenderer<BookshelfBlockEntity> {
    private static final double BOOK_DEPTH_CENTER = 4.5;
    private static final double BOOK_MODEL_HEIGHT = 4.2;
    private static final double BOOK_CENTER_OFFSET = 2.0;
    public static final ResourceLocation[] BOOK_RESOURCES = new ResourceLocation[BookshelfVariants.VARIANT_COUNT];
    private static final ModelResourceLocation[] BOOK_MODELS =
            new ModelResourceLocation[BookshelfVariants.VARIANT_COUNT];

    static {
        for (int i = 0; i < BookshelfVariants.VARIANT_COUNT; i++) {
            BOOK_RESOURCES[i] = ResourceLocation.fromNamespaceAndPath(Stuff.MOD_ID, "block/bookshelf/book_" + i);
            BOOK_MODELS[i] = ModelResourceLocation.standalone(BOOK_RESOURCES[i]);
        }
    }

    public BookshelfRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            BookshelfBlockEntity shelf,
            float partialTick,
            PoseStack poses,
            MultiBufferSource buffers,
            int light,
            int overlay) {
        var level = shelf.getLevel();
        if (level == null) return;
        poses.pushPose();
        poses.translate(.5, 0, .5);
        poses.mulPose(Axis.YP.rotationDegrees(
                -shelf.getBlockState().getValue(BookshelfBlock.FACING).toYRot()));
        poses.translate(-.5, 0, -.5);
        var minecraft = Minecraft.getInstance();
        var cutout = RenderType.cutout();
        float[] slotCenters = new float[BookshelfSlots.SLOTS_PER_SHELF];
        for (int shelfIndex = 0; shelfIndex < BookshelfSlots.SHELF_COUNT; shelfIndex++) {
            BookshelfLayout.fillPackedSlotCenters(shelf, shelfIndex, slotCenters);
            for (int slotIndex = 0; slotIndex < BookshelfSlots.SLOTS_PER_SHELF; slotIndex++) {
                int bookIndex = BookshelfSlots.slotIndex(shelfIndex, slotIndex);
                var book = shelf.bookAt(bookIndex);
                if (book.isEmpty()) continue;
                BookshelfVariants.BookShape shape = BookshelfVariants.shapeForSlot(book, bookIndex);
                poses.pushPose();
                poses.translate(
                        slotCenters[slotIndex] / BookshelfSlots.MODEL_UNITS_PER_BLOCK,
                        (BookshelfSlots.shelfSurfaceY(shelfIndex) + .01) / BookshelfSlots.MODEL_UNITS_PER_BLOCK,
                        BOOK_DEPTH_CENTER / BookshelfSlots.MODEL_UNITS_PER_BLOCK);
                poses.scale(shape.thickness(), shape.height() / (float) BOOK_MODEL_HEIGHT, 1.0F);
                poses.translate(0, 0, -BOOK_CENTER_OFFSET / BookshelfSlots.MODEL_UNITS_PER_BLOCK);
                var model = minecraft
                        .getModelManager()
                        .getModel(BOOK_MODELS[BookshelfVariants.variantForSlot(book, bookIndex)]);
                minecraft
                        .getBlockRenderer()
                        .getModelRenderer()
                        .renderModel(
                                poses.last(),
                                buffers.getBuffer(cutout),
                                shelf.getBlockState(),
                                model,
                                1,
                                1,
                                1,
                                light,
                                overlay,
                                ModelData.EMPTY,
                                cutout);
                poses.popPose();
            }
        }
        poses.popPose();
    }

    @Override
    public int getViewDistance() {
        return 32;
    }

    @Override
    public AABB getRenderBoundingBox(BookshelfBlockEntity shelf) {
        return new AABB(shelf.getBlockPos()).expandTowards(0, 1, 0);
    }
}
