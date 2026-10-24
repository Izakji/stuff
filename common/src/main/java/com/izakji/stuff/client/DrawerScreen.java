package com.izakji.stuff.client;

import com.izakji.stuff.content.desk.DrawerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Client screen for one desk drawer inventory.
 *
 * @author Izakji
 */
public final class DrawerScreen extends AbstractContainerScreen<DrawerMenu> {
    public DrawerScreen(DrawerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 196;
        inventoryLabelY = 103;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        graphics.fill(x, y, x + 176, y + 196, 0xFF37302A);
        graphics.fill(x + 1, y + 1, x + 175, y + 195, 0xFFEEE5D3);
        graphics.fill(x + 3, y + 3, x + 173, y + 193, 0xFFC6BBA6);
        graphics.fill(x + 4, y + 21, x + 172, y + 97, 0xFFB3A58D);
        for (Slot slot : menu.slots) {
            int sx = x + slot.x - 1, sy = y + slot.y - 1;
            graphics.fill(sx, sy, sx + 18, sy + 18, 0xFFF4EBD9);
            graphics.fill(sx, sy, sx + 17, sy + 17, 0xFF554B3C);
            graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF8C8271);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
