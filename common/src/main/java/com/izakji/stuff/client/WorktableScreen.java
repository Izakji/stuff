package com.izakji.stuff.client;

import com.izakji.stuff.content.worktable.WorktableCategory;
import com.izakji.stuff.content.worktable.WorktableMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Six visible catalogue rows; only visible recipe widgets exist, independent of catalogue size.
 *
 * @author Izakji
 */
public final class WorktableScreen extends AbstractContainerScreen<WorktableMenu> {
    private static final int VISIBLE_ROWS = 6;
    private final List<CatalogueRow> rows = new ArrayList<>();
    private final List<AbstractButton> recipeButtons = new ArrayList<>();
    private int scroll;
    private boolean draggingScroll;
    private AbstractButton craft;

    private record CatalogueRow(Component title, int recipeIndex) {}

    public WorktableScreen(WorktableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 300;
        imageHeight = 236;
        inventoryLabelX = 69;
        inventoryLabelY = 144;
        for (WorktableCategory category : WorktableCategory.values()) {
            rows.add(new CatalogueRow(category.title(), -1));
            for (int i = 0; i < menu.catalogue().size(); i++) {
                var recipe = menu.catalogue().get(i).value();
                if (recipe.category() == category)
                    rows.add(new CatalogueRow(recipe.result().getHoverName(), i));
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        craft = addRenderableWidget(
                new AbstractButton(
                        leftPos + 166, topPos + 98, 108, 20, Component.translatable("gui.stuff.worktable.craft")) {
                    @Override
                    public void onPress() {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, WorktableMenu.CRAFT_BUTTON);
                    }

                    @Override
                    protected void updateWidgetNarration(NarrationElementOutput output) {
                        defaultButtonNarrationText(output);
                    }

                    @Override
                    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
                        int x = getX(), y = getY();
                        g.fill(x, y, x + width, y + height, 0xff35261c);
                        g.fill(
                                x + 1,
                                y + 1,
                                x + width - 1,
                                y + height - 1,
                                !active ? 0xff655441 : isHoveredOrFocused() ? 0xff90673e : 0xff74502f);
                        g.fill(x + 1, y + 1, x + width - 1, y + 2, active ? 0xffb58b59 : 0xff82705a);
                        g.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, 0xff493320);
                        g.drawCenteredString(
                                font, getMessage(), x + width / 2, y + 6, active ? 0xffffedcf : 0xffb7a38b);
                    }
                });
        refreshRows();
    }

    private void refreshRows() {
        recipeButtons.forEach(this::removeWidget);
        recipeButtons.clear();
        for (int visible = 0; visible < VISIBLE_ROWS && scroll + visible < rows.size(); visible++) {
            CatalogueRow row = rows.get(scroll + visible);
            if (row.recipeIndex() < 0) continue;
            var button = new AbstractButton(leftPos + 10, topPos + 26 + visible * 18, 115, 18, row.title()) {
                private long hoverStart = -1;

                @Override
                protected void updateWidgetNarration(NarrationElementOutput output) {
                    defaultButtonNarrationText(output);
                }

                @Override
                public void onPress() {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, row.recipeIndex() + 1);
                }

                @Override
                protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                    int color = menu.selectedIndex() == row.recipeIndex()
                            ? 0xffc8a46c
                            : isHoveredOrFocused() ? 0xffdec49e : 0xffe7d4b5;
                    graphics.fill(getX(), getY(), getX() + width, getY() + height - 1, color);
                    ItemStack item =
                            menu.catalogue().get(row.recipeIndex()).value().result();
                    graphics.renderItem(item, getX() + 1, getY());
                    int textX = getX() + 20, available = width - 23;
                    String full = getMessage().getString();
                    int overflow = font.width(full) - available;
                    boolean hoverText = mouseX >= textX
                            && mouseX < textX + available
                            && mouseY >= getY()
                            && mouseY < getY() + height;
                    if (hoverText && overflow > 0) {
                        long now = Util.getMillis();
                        if (hoverStart < 0) hoverStart = now;
                        // Pause at both ends, then repeat from the start while hovering.
                        double travel = overflow / 24.0;
                        double phase = ((now - hoverStart) / 1000.0) % (travel + 1.5);
                        int offset = (int) Math.round(Mth.clamp((phase - .75) * 24, 0, overflow));
                        graphics.enableScissor(textX, getY(), textX + available, getY() + height);
                        graphics.drawString(font, full, textX - offset, getY() + 5, 0xff382d23, false);
                        graphics.disableScissor();
                    } else {
                        hoverStart = -1;
                        String label = overflow > 0
                                ? font.plainSubstrByWidth(full, available - font.width("...")) + "..."
                                : full;
                        graphics.drawString(font, label, textX, getY() + 5, 0xff382d23, false);
                    }
                }
            };
            recipeButtons.add(addRenderableWidget(button));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        craft.active = menu.canCraft();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xff4c3928);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xffd5bb93);
        g.fill(x + 7, y + 23, x + 137, y + 139, 0xffa18360);
        g.fill(x + 8, y + 24, x + 128, y + 138, 0xffead9bd);
        g.fill(x + 142, y + 23, x + 292, y + 140, 0xffead9bd);
        for (int i = 0; i < VISIBLE_ROWS && scroll + i < rows.size(); i++) {
            CatalogueRow row = rows.get(scroll + i);
            if (row.recipeIndex() < 0)
                g.drawString(
                        font,
                        font.plainSubstrByWidth(row.title().getString(), 113),
                        x + 11,
                        y + 31 + i * 18,
                        0xff735132,
                        false);
        }
        int max = Math.max(0, rows.size() - VISIBLE_ROWS);
        int thumb = y + 26 + (max == 0 ? 0 : scroll * 88 / max);
        g.fill(x + 130, y + 26, x + 135, y + 134, 0xff71553b);
        g.fill(x + 130, thumb, x + 135, thumb + 20, 0xffceb18b);
        var recipe = menu.selected();
        if (recipe != null) {
            g.drawString(
                    font,
                    font.plainSubstrByWidth(recipe.result().getHoverName().getString(), 140),
                    x + 147,
                    y + 28,
                    0xff382d23,
                    false);
            g.drawString(
                    font, Component.translatable("gui.stuff.worktable.materials"), x + 147, y + 40, 0xff735132, false);
        } else {
            g.drawString(
                    font, Component.translatable("gui.stuff.worktable.select"), x + 150, y + 60, 0xff735132, false);
        }
        for (var slot : menu.slots) {
            if (!slot.isActive()) continue;
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, 0xff80664a);
            g.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0xffb9a07c);
        }
        if (recipe != null)
            for (int i = 0; i < recipe.materials().size(); i++) {
                var material = recipe.materials().get(i);
                var slot = menu.slots.get(i);
                if (!slot.hasItem()) {
                    ItemStack[] alternatives = material.ingredient().getItems();
                    if (alternatives.length > 0) {
                        ItemStack ghost =
                                alternatives[(int) ((System.currentTimeMillis() / 1500) % alternatives.length)];
                        g.renderFakeItem(ghost, x + slot.x, y + slot.y);
                        g.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0x88ead9bd);
                    }
                }
                g.drawString(
                        font,
                        Integer.toString(material.count()),
                        x + slot.x + 19,
                        y + slot.y + 5,
                        material.matches(slot.getItem()) ? 0xff36703e : 0xff834332,
                        false);
            }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        craft.active = menu.canCraft();
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        var recipe = menu.selected();
        if (recipe != null)
            for (int i = 0; i < recipe.materials().size(); i++) {
                var slot = menu.slots.get(i);
                if (!slot.hasItem() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    var material = recipe.materials().get(i);
                    var items = material.ingredient().getItems();
                    if (items.length > 0)
                        g.renderTooltip(
                                font,
                                Component.translatable(
                                        "gui.stuff.worktable.required",
                                        material.count(),
                                        items[(int) ((System.currentTimeMillis() / 1500) % items.length)]
                                                .getHoverName()),
                                mouseX,
                                mouseY);
                }
            }
    }

    private void setScroll(int value) {
        int next = Mth.clamp(value, 0, Math.max(0, rows.size() - VISIBLE_ROWS));
        if (next != scroll) {
            scroll = next;
            refreshRows();
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (isHovering(7, 23, 130, 116, x, y)) {
            setScroll(scroll - (int) Math.signum(vertical));
            return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && isHovering(129, 24, 8, 112, x, y)) {
            draggingScroll = true;
            dragScroll(y);
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    private void dragScroll(double y) {
        setScroll((int) Math.round((y - topPos - 36) / 88 * Math.max(0, rows.size() - VISIBLE_ROWS)));
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (draggingScroll) {
            dragScroll(y);
            return true;
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        draggingScroll = false;
        return super.mouseReleased(x, y, button);
    }
}
