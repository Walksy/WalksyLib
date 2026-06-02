package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.gui.utils.CategoryTab;
import main.walksy.lib.core.gui.utils.TabLocation;
import main.walksy.lib.core.utils.MainColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.awt.*;
import java.util.List;

public class UniversalTabWidget extends AbstractWidget {

    private final TabManager tabManager;
    private final TabLocation location;
    private final Screen parent;

    private static final int TAB_WIDTH = 100;
    private static final int TAB_HEIGHT = 20;

    private List<CategoryTab> tabs;
    private float currentScrollOffset = 0;
    private float targetScrollOffset = 0;

    public UniversalTabWidget(final int x, final int y, final int width, final int height, final List<CategoryTab> tabs, final TabManager tabManager, final TabLocation location, final Screen parent) {
        super(x, y, width, height, Component.empty());
        this.tabManager = tabManager;
        this.location = location;
        this.parent = parent;
        this.tabs = tabs;
    }

    private void renderArrowIndicator(final GuiGraphicsExtractor ctx) {
        final MaxOffset offset = this.getMaxOffsetDirection();
        if (offset == null) return;

        ctx.pose().pushMatrix();
        int fadeAlpha = (int) ((Math.sin((System.currentTimeMillis() % 1000) / 1000.0 * 2 * Math.PI) * 0.5 + 0.5) * 255.0);
        fadeAlpha = Math.min(Math.max(fadeAlpha, 0), 255);

        if (offset != MaxOffset.RIGHT) {
            ctx.pose().pushMatrix();
            ctx.pose().translate((float) ((this.getX() + this.getWidth()) - 25), (float) ((this.getY() + this.getHeight()) / 2.0 + 2.5));
            ctx.pose().scale(2.0F, 2.0F);
            ctx.blit(RenderPipelines.GUI_TEXTURED, ScrollableTabWidget.ARROW,
                    -1, -1, 0.0F, 0.0F, 8, 8, 8, 16, new Color(255, 255, 255, fadeAlpha).getRGB());
            ctx.pose().popMatrix();
        }

        if (offset != MaxOffset.LEFT) {
            ctx.pose().pushMatrix();
            ctx.pose().translate((float) (this.getX() + 25), (float) ((this.getY() + this.getHeight()) / 2.0 + 2.5));
            ctx.pose().scale(2.0F, 2.0F);
            ctx.blit(RenderPipelines.GUI_TEXTURED, ScrollableTabWidget.ARROW,
                    1, -1, 0.0F, 8F, 8, 8, 8, 16, new Color(255, 255, 255, fadeAlpha).getRGB());
            ctx.pose().popMatrix();
        }

        ctx.pose().popMatrix();
    }

    private String getAnimatedTabTitle(final String full, final Font textRenderer, final int maxWidth) {
        if (textRenderer.width(full) <= maxWidth) return full;

        final String ellipsis = "...";
        final int ellipsisWidth = textRenderer.width(ellipsis);
        final int visibleWidth = maxWidth - ellipsisWidth;

        final int[] widths = new int[full.length() + 1];
        for (int i = 0; i < full.length(); i++) widths[i + 1] = widths[i] + textRenderer.width(full.substring(i, i + 1));

        int mChars = 0;
        for (int i = 1; i <= full.length(); i++) {
            if (widths[i] <= visibleWidth) mChars = i;
            else break;
        }

        final int steps = full.length() - mChars + 1;
        final int cycle = steps * 2 - 2;
        int pos = (int) ((System.currentTimeMillis() / 100) % cycle);
        if (pos >= steps) pos = cycle - pos;

        final String visiblePart = full.substring(pos, pos + mChars);
        return (pos == steps - 1) ? visiblePart : visiblePart + ellipsis;
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double horizontalAmount, final double verticalAmount) {
        final int totalWidth = this.tabs.size() * (TAB_WIDTH + 4) - 4;
        if (totalWidth <= this.width) return false;

        this.targetScrollOffset -= (float) (verticalAmount * 20);
        this.targetScrollOffset = Math.max(0, Math.min(this.targetScrollOffset, totalWidth - this.width));
        return true;
    }

    @Override
    protected void extractWidgetRenderState(final GuiGraphicsExtractor ctx, final int mouseX, final int mouseY, final float delta) {
        final Minecraft client = Minecraft.getInstance();
        this.currentScrollOffset = Mth.lerp(0.2f, this.currentScrollOffset, this.targetScrollOffset);

        if (this.location == TabLocation.TOP || this.location == TabLocation.BOTTOM) {
            if (this.tabs.isEmpty()) return;
            ctx.enableScissor(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height);

            final int tabCount = this.tabs.size();
            final int totalWidth = tabCount * (TAB_WIDTH + 4) - 4;
            final int baseX = totalWidth <= this.width
                    ? this.getX() + (this.width - totalWidth) / 2
                    : this.getX() - (int) this.currentScrollOffset;

            final int[] tabXs = new int[tabCount];
            final boolean[] hoveredTabs = new boolean[tabCount];
            final boolean[] selectedTabs = new boolean[tabCount];

            for (int i = 0; i < tabCount; i++) {
                final int tabX = baseX + i * (TAB_WIDTH + 4);
                tabXs[i] = tabX;

                final boolean hovered = mouseX >= tabX && mouseX <= tabX + TAB_WIDTH && mouseY >= this.getY() && mouseY <= this.getY() + TAB_HEIGHT;
                hoveredTabs[i] = hovered;

                final boolean selected = this.tabs.get(i).equals(this.tabManager.getCurrentTab());
                selectedTabs[i] = selected;
            }

            for (int i = 0; i < tabCount; i++) {
                final int tabX = tabXs[i];
                if (tabX + TAB_WIDTH < this.getX() || tabX > this.getX() + this.width) continue;

                final String fullText = this.tabs.get(i).getTabTitle().getString();
                final String text = this.getAnimatedTabTitle(fullText, client.font, TAB_WIDTH - 10);

                final int color = selectedTabs[i] ? 0xFFFFFFFF : hoveredTabs[i] ? 0xFFCCCCCC : 0xFF888888;
                ctx.text(client.font, text,
                        tabX + (TAB_WIDTH - client.font.width(text)) / 2,
                        this.getY() + (TAB_HEIGHT - 8) / 2,
                        color);
            }

            final int y = this.getY() + TAB_HEIGHT - 1;
            final int startX = baseX;
            final int endX = baseX + totalWidth;

            ctx.horizontalLine(startX, endX, y, MainColors.OUTLINE_WHITE.getRGB());
            ctx.horizontalLine(startX - 2, endX + 2, y + 1, MainColors.OUTLINE_BLACK.getRGB());

            final int leftAlpha;
            if (selectedTabs[0]) leftAlpha = 255;
            else if (hoveredTabs[0]) leftAlpha = 204;
            else leftAlpha = 51;

            final int rightAlpha;
            if (selectedTabs[tabCount - 1]) rightAlpha = 255;
            else if (hoveredTabs[tabCount - 1]) rightAlpha = 204;
            else rightAlpha = 51;

            ctx.verticalLine(startX - 1, y + 1, y - TAB_HEIGHT, new Color(255, 255, 255, leftAlpha).getRGB());
            ctx.verticalLine(startX - 2, y + 1, y - TAB_HEIGHT - 1, new Color(0, 0, 0, 191).getRGB());
            if ((tabCount - 1) * TAB_WIDTH <= this.parent.width) {
                ctx.horizontalLine(0, startX - 3, 27, new Color(0, 0, 0, 191).getRGB());
            }

            ctx.verticalLine(endX + 1, y + 1, y - TAB_HEIGHT, new Color(255, 255, 255, rightAlpha).getRGB());
            ctx.verticalLine(endX + 2, y + 1, y - TAB_HEIGHT - 1, new Color(0, 0, 0, 191).getRGB());
            ctx.horizontalLine(this.getWidth(), endX + 3, 27, new Color(0, 0, 0, 191).getRGB());

            for (int i = 0; i < tabCount; i++) {
                final int tabX = tabXs[i];
                if (tabX + TAB_WIDTH < this.getX() || tabX > this.getX() + this.width) continue;

                if (selectedTabs[i] || hoveredTabs[i]) {
                    int fillRight = tabX + TAB_WIDTH;
                    if (i == tabCount - 1) {
                        fillRight += 1;
                    }
                    ctx.fill(tabX, this.getY() + TAB_HEIGHT - 1, fillRight, this.getY() + TAB_HEIGHT, selectedTabs[i] ? 0xFFFFFFFF : 0xFFCCCCCC);
                }
            }

            this.renderArrowIndicator(ctx);

            ctx.disableScissor();
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent click, final boolean doubled) {
        final double mouseX = click.x();
        final double mouseY = click.y();
        for (int i = 0; i < this.tabs.size(); i++) {
            final int tabX = (this.tabs.size() * (TAB_WIDTH + 4) - 4 <= this.width
                    ? this.getX() + (this.width - (this.tabs.size() * (TAB_WIDTH + 4) - 4)) / 2
                    : this.getX() - (int) this.currentScrollOffset) + i * (TAB_WIDTH + 4);

            if (mouseX >= tabX && mouseX <= tabX + TAB_WIDTH && mouseY >= this.getY() && mouseY <= this.getY() + TAB_HEIGHT) {
                if (this.tabManager.getCurrentTab() != this.tabs.get(i)) {
                    this.tabManager.setCurrentTab(this.tabs.get(i), false);
                    AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
                }
                return true;
            }
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput output) {}

    public void setTabs(final List<CategoryTab> tabs) {
        this.tabs.clear();
        this.tabs.addAll(tabs);
    }

    public void selectTab(final int index, final boolean bl) {
        if (index >= 0 && index < this.tabs.size()) {
            this.tabManager.setCurrentTab(this.tabs.get(index), false);
        }
    }

    public MaxOffset getMaxOffsetDirection() {
        final int totalWidth = this.tabs.size() * (TAB_WIDTH + 4) - 4;
        if (totalWidth <= this.width) return null;
        if (this.targetScrollOffset <= 0) return MaxOffset.LEFT;
        if (this.targetScrollOffset >= totalWidth - this.width) return MaxOffset.RIGHT;
        return MaxOffset.MID;
    }

    public enum MaxOffset {
        LEFT,
        RIGHT,
        MID
    }
}
