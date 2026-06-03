package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.utils.CategoryTab;
import main.walksy.lib.core.gui.utils.TabLocation;
import main.walksy.lib.core.mixin.ScreenAccessor;
import main.walksy.lib.core.utils.MainColors;
import main.walksy.lib.core.utils.MarqueeUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ScrollableTabWidget extends AbstractWidget {

    public static final Identifier ARROW = Identifier.fromNamespaceAndPath("walksylib", "gui/arrow.png");

    private List<CategoryTab> tabs;
    private final TabManager tabManager;
    private final TabLocation location;
    private final WalksyLibConfigScreen parent;

    private static final int TAB_WIDTH = 100;
    private static final int TAB_HEIGHT = 20;

    private float currentScrollOffset = 0;
    private float targetScrollOffset = 0;

    public ScrollableTabWidget(final int x, final int y, final int width, final int height, final List<CategoryTab> tabs, final TabManager tabManager, final TabLocation location, final WalksyLibConfigScreen parent) {
        super(x, y, width, height, Component.empty());
        this.tabs = tabs;
        this.tabManager = tabManager;
        this.location = location;
        this.parent = parent;
    }

    @Override
    protected void extractWidgetRenderState(final GuiGraphicsExtractor ctx, final int mouseX, final int mouseY, final float a) {
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
                final String text = MarqueeUtil.get(fullText, TAB_WIDTH - 10, 10);

                final int color = selectedTabs[i] ? 0xFFFFFFFF : hoveredTabs[i] ? 0xFFCCCCCC : 0xFF888888;
                ctx.text(client.font, text,
                        tabX + (TAB_WIDTH - client.font.width(text)) / 2,
                        this.getY() + (TAB_HEIGHT - 8) / 2,
                        color,
                        true);
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
    protected void updateWidgetNarration(final NarrationElementOutput output) {}

    private void renderArrowIndicator(final GuiGraphicsExtractor ctx) {
        final MaxOffset offset = this.getMaxOffsetDirection();
        if (offset == null) return;

        ctx.pose().pushMatrix();

        final double offsetX = 0;
        int fadeAlpha = (int) ((Math.sin((this.parent.getUpTime() % 60) / 60.0 * 2 * Math.PI) * 0.5 + 0.5) * 255.0);
        fadeAlpha = Math.min(Math.max(fadeAlpha, 0), 255);

        if (offset != MaxOffset.RIGHT) {
            ctx.pose().pushMatrix();
            ctx.pose().translate((float) ((this.getX() + this.getWidth()) - 25 + offsetX), (float) ((this.getY() + this.getHeight()) / 2.0 + 2.5));
            ctx.pose().scale(2.0F, 2.0F);
            ctx.pose().translate(1.0F, 1.0F);

            ctx.blit(RenderPipelines.GUI_TEXTURED, ARROW,
                -1, -1, 0.0F, 0.0F, 8, 8, 8, 16, new Color(255, 255, 255, fadeAlpha).getRGB());

            ctx.pose().popMatrix();
        }

        if (offset != MaxOffset.LEFT) {
            ctx.pose().pushMatrix();
            ctx.pose().translate((float) (this.getX() + 25 - offsetX), (float) ((this.getY() + this.getHeight()) / 2.0 + 2.5));
            ctx.pose().scale(2.0F, 2.0F);
            ctx.pose().translate(-9.0F, 1.0F);

            ctx.blit(RenderPipelines.GUI_TEXTURED, ARROW,
                1, -1, 0.0F, 8F, 8, 8, 8, 16, new Color(255, 255, 255, fadeAlpha).getRGB());

            ctx.pose().popMatrix();
        }

        ctx.pose().popMatrix();
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double horizontalAmount, final double verticalAmount) {
        if (this.location == TabLocation.TOP || this.location == TabLocation.BOTTOM) {
            final int totalWidth = this.tabs.size() * (TAB_WIDTH + 4) - 4;
            if (totalWidth <= this.width) return false;

            this.targetScrollOffset -= (float) (verticalAmount * 20);
            this.targetScrollOffset = Math.max(0, Math.min(this.targetScrollOffset, totalWidth - this.width));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent click, final boolean doubled) {
        final double mouseX = click.x();
        final double mouseY = click.y();
        if (this.location == TabLocation.TOP || this.location == TabLocation.BOTTOM) {
            for (int i = 0; i < this.tabs.size(); i++) {
                final int tabX = (this.tabs.size() * (TAB_WIDTH + 4) - 4 <= this.width
                    ? this.getX() + (this.width - (this.tabs.size() * (TAB_WIDTH + 4) - 4)) / 2
                    : this.getX() - (int) this.currentScrollOffset) + i * (TAB_WIDTH + 4);

                if (mouseX >= tabX && mouseX <= tabX + TAB_WIDTH && mouseY >= this.getY() && mouseY <= this.getY() + TAB_HEIGHT) {
                    if (this.tabManager.getCurrentTab() != this.tabs.get(i)) {
                        this.selectTab(i, true);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public void updateVisibleWidgetsForTab(final CategoryTab tab) {
        this.parent.children().removeIf(w -> w instanceof OptionGroupWidget || w instanceof OptionWidget);
        ((ScreenAccessor) this.parent).getDrawables().removeIf(w -> w instanceof OptionGroupWidget || w instanceof OptionWidget);

        for (final OptionGroupWidget groupWidget : tab.getOptionGroupWidgets()) {
            this.parent.addWidget(groupWidget);

            for (final OptionWidget optionWidget : groupWidget.getChildren()) {
                this.parent.addWidget(optionWidget);
            }
        }
        this.parent.layoutGroupWidgets();
    }

    public void setTabs(final List<CategoryTab> tabs) {
        this.tabs = new ArrayList<>(tabs);
    }

    public int tabSize() {
        return this.tabs.size();
    }

    public MaxOffset getMaxOffsetDirection() {
        final int totalWidth = this.tabs.size() * (TAB_WIDTH + 4) - 4;
        if (totalWidth <= this.width) {
            return null;
        }
        if (this.targetScrollOffset <= 0) {
            return MaxOffset.LEFT;
        }
        if (this.targetScrollOffset >= totalWidth - this.width) {
            return MaxOffset.RIGHT;
        }
        return MaxOffset.MID;
    }

    public void selectTab(final int index, final boolean bl) {
        if (index >= 0 && index < this.tabs.size()) {
            this.tabManager.setCurrentTab(this.tabs.get(index), true);
            if (bl) {
                this.parent.showWidgetsForCategory(this.tabs.get(index).getCategory());
            }
            this.parent.setFocusedOption(null);
        }
    }

    public boolean isHoveringOverAnyTab(final double mouseX, final double mouseY) {
        if (this.location != TabLocation.TOP && this.location != TabLocation.BOTTOM || this.tabs.isEmpty()) return false;

        final int totalWidth = this.tabs.size() * (TAB_WIDTH + 4) - 4;
        final int baseX = totalWidth <= this.width
                ? this.getX() + (this.width - totalWidth) / 2
                : this.getX() - (int) this.currentScrollOffset;

        for (int i = 0; i < this.tabs.size(); i++) {
            final int tabX = baseX + i * (TAB_WIDTH + 4);
            if (mouseX >= tabX && mouseX <= tabX + TAB_WIDTH && mouseY >= this.getY() && mouseY <= this.getY() + TAB_HEIGHT) {
                return true;
            }
        }
        return false;
    }

    public enum MaxOffset {
        LEFT,
        RIGHT,
        MID
    }
}
