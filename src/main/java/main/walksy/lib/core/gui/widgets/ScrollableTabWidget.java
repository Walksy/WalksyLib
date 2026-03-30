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

    public ScrollableTabWidget(int x, int y, int width, int height, List<CategoryTab> tabs, TabManager tabManager, TabLocation location, WalksyLibConfigScreen parent) {
        super(x, y, width, height, Component.empty());
        this.tabs = tabs;
        this.tabManager = tabManager;
        this.location = location;
        this.parent = parent;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float a) {
        Minecraft client = Minecraft.getInstance();
        currentScrollOffset = Mth.lerp(0.2f, currentScrollOffset, targetScrollOffset);

        if (location == TabLocation.TOP || location == TabLocation.BOTTOM) {
            if (tabs.isEmpty()) return;
            ctx.enableScissor(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height);

            final int tabCount = tabs.size();
            final int totalWidth = tabCount * (TAB_WIDTH + 4) - 4;
            final int baseX = totalWidth <= this.width
                    ? this.getX() + (this.width - totalWidth) / 2
                    : this.getX() - (int) currentScrollOffset;

            int[] tabXs = new int[tabCount];
            boolean[] hoveredTabs = new boolean[tabCount];
            boolean[] selectedTabs = new boolean[tabCount];

            for (int i = 0; i < tabCount; i++) {
                int tabX = baseX + i * (TAB_WIDTH + 4);
                tabXs[i] = tabX;

                boolean hovered = mouseX >= tabX && mouseX <= tabX + TAB_WIDTH && mouseY >= this.getY() && mouseY <= this.getY() + TAB_HEIGHT;
                hoveredTabs[i] = hovered;

                boolean selected = tabs.get(i).equals(tabManager.getCurrentTab());
                selectedTabs[i] = selected;

                //ctx.fill(tabX, getY(), tabX + TAB_WIDTH + (i == (tabCount - 1) ? 1 : 4), getY() + TAB_HEIGHT - 1, new Color(0, 0, 0, 100).getRGB());
            }


            //Draw tab titles
            for (int i = 0; i < tabCount; i++) {
                int tabX = tabXs[i];
                if (tabX + TAB_WIDTH < this.getX() || tabX > this.getX() + this.width) continue;

                String fullText = tabs.get(i).getTabTitle().getString();
                String text = MarqueeUtil.get(fullText, TAB_WIDTH - 10, 10);

                int color = selectedTabs[i] ? 0xFFFFFFFF : hoveredTabs[i] ? 0xFFCCCCCC : 0xFF888888;
                ctx.text(client.font, text,
                        tabX + (TAB_WIDTH - client.font.width(text)) / 2,
                        this.getY() + (TAB_HEIGHT - 8) / 2,
                        color,
                        true);
            }

            //Draw base lines
            int y = this.getY() + TAB_HEIGHT - 1;
            int startX = baseX;
            int endX = baseX + totalWidth;

            ctx.horizontalLine(startX, endX, y, MainColors.OUTLINE_WHITE.getRGB());
            ctx.horizontalLine(startX - 2, endX + 2, y + 1, MainColors.OUTLINE_BLACK.getRGB());

            int leftAlpha;
            if (selectedTabs[0]) leftAlpha = 255;
            else if (hoveredTabs[0]) leftAlpha = 204; //0xCC, same as hovered bottom highlight alpha
            else leftAlpha = 51;

            int rightAlpha;
            if (selectedTabs[tabCount - 1]) rightAlpha = 255;
            else if (hoveredTabs[tabCount - 1]) rightAlpha = 204;
            else rightAlpha = 51;

            //LEFT vertical lines
            ctx.verticalLine(startX - 1, y + 1, y - TAB_HEIGHT, new Color(255, 255, 255, leftAlpha).getRGB()); //this
            ctx.verticalLine(startX - 2, y + 1, y - TAB_HEIGHT - 1, new Color(0, 0, 0, 191).getRGB());
            if ((tabCount - 1) * TAB_WIDTH <= parent.width) {
                ctx.horizontalLine(0, startX - 3, 27, new Color(0, 0, 0, 191).getRGB());
            }

            //RIGHT vertical lines
            ctx.verticalLine(endX + 1, y + 1, y - TAB_HEIGHT, new Color(255, 255, 255, rightAlpha).getRGB()); //this
            ctx.verticalLine(endX + 2, y + 1, y - TAB_HEIGHT - 1, new Color(0, 0, 0, 191).getRGB());
            ctx.horizontalLine(this.getWidth(), endX + 3, 27, new Color(0, 0, 0, 191).getRGB());

            //Draw bottom highlight for hovered or selected tabs
            for (int i = 0; i < tabCount; i++) {
                int tabX = tabXs[i];
                if (tabX + TAB_WIDTH < this.getX() || tabX > this.getX() + this.width) continue;

                if (selectedTabs[i] || hoveredTabs[i]) {
                    int fillRight = tabX + TAB_WIDTH;
                    if (i == tabCount - 1) {
                        fillRight += 1; //extend by 1 pixel on the last tab to cover full width
                    }
                    ctx.fill(tabX, this.getY() + TAB_HEIGHT - 1, fillRight, this.getY() + TAB_HEIGHT, selectedTabs[i] ? 0xFFFFFFFF : 0xFFCCCCCC);
                }
            }

            renderArrowIndicator(ctx);

            ctx.disableScissor();
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        
    }

    private void renderArrowIndicator(GuiGraphicsExtractor ctx) {
        MaxOffset offset = getMaxOffsetDirection();
        if (offset == null) return;

        ctx.pose().pushMatrix();

        double offsetX = 0; //(Math.sin((parent.tickCount % 60) / 60.0 * 2 * Math.PI) * 0.5 + 0.5) * 6.0
        int fadeAlpha = (int) ((Math.sin((parent.tickCount % 60) / 60.0 * 2 * Math.PI) * 0.5 + 0.5) * 255.0);
        fadeAlpha = Math.min(Math.max(fadeAlpha, 0), 255);

        if (offset != MaxOffset.RIGHT) {
            ctx.pose().pushMatrix();
            ctx.pose().translate((float) ((getX() + getWidth()) - 25 + offsetX), (float) ((getY() + getHeight()) / 2.0 + 2.5));
            ctx.pose().scale(2.0F, 2.0F);
            ctx.pose().translate(1.0F, 1.0F);

            ctx.blit(RenderPipelines.GUI_TEXTURED, ARROW,
                -1, -1, 0.0F, 0.0F, 8, 8, 8, 16, new Color(255, 255, 255, fadeAlpha).getRGB());

            ctx.pose().popMatrix();
        }

        if (offset != MaxOffset.LEFT) {
            ctx.pose().pushMatrix();
            ctx.pose().translate((float) (getX() + 25 - offsetX), (float) ((getY() + getHeight()) / 2.0 + 2.5));
            ctx.pose().scale(2.0F, 2.0F);
            ctx.pose().translate(-9.0F, 1.0F);

            ctx.blit(RenderPipelines.GUI_TEXTURED, ARROW,
                1, -1, 0.0F, 8F, 8, 8, 8, 16, new Color(255, 255, 255, fadeAlpha).getRGB());

            ctx.pose().popMatrix();
        }

        ctx.pose().popMatrix();
    }


    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (location == TabLocation.TOP || location == TabLocation.BOTTOM) {
            int totalWidth = tabs.size() * (TAB_WIDTH + 4) - 4;
            if (totalWidth <= this.width) return false;

            targetScrollOffset -= (float) (verticalAmount * 20);
            targetScrollOffset = Math.max(0, Math.min(targetScrollOffset, totalWidth - this.width));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (location == TabLocation.TOP || location == TabLocation.BOTTOM) {
            for (int i = 0; i < tabs.size(); i++) {
                int tabX = (tabs.size() * (TAB_WIDTH + 4) - 4 <= this.width
                    ? this.getX() + (this.width - (tabs.size() * (TAB_WIDTH + 4) - 4)) / 2
                    : this.getX() - (int) currentScrollOffset) + i * (TAB_WIDTH + 4);

                if (mouseX >= tabX && mouseX <= tabX + TAB_WIDTH && mouseY >= this.getY() && mouseY <= this.getY() + TAB_HEIGHT) {
                    if (tabManager.getCurrentTab() != tabs.get(i)) {
                        selectTab(i, true);
                        //Minecraft.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public void updateVisibleWidgetsForTab(CategoryTab tab) {
        parent.children().removeIf(w -> w instanceof OptionGroupWidget || w instanceof OptionWidget);
        ((ScreenAccessor)parent).getDrawables().removeIf(w -> w instanceof OptionGroupWidget || w instanceof OptionWidget);


        for (OptionGroupWidget groupWidget : tab.getOptionGroupWidgets()) {
            parent.addWidget(groupWidget);

            for (OptionWidget optionWidget : groupWidget.getChildren()) {
                parent.addWidget(optionWidget);
            }
        }
        parent.layoutGroupWidgets();
    }

    public void setTabs(List<CategoryTab> tabs) {
        this.tabs = new ArrayList<>(tabs);
    }

    public int tabSize()
    {
        return this.tabs.size();
    }

    public MaxOffset getMaxOffsetDirection() {
        int totalWidth = tabs.size() * (TAB_WIDTH + 4) - 4;
        if (totalWidth <= this.width) {
            return null;
        }
        if (targetScrollOffset <= 0) {
            return MaxOffset.LEFT;
        }
        if (targetScrollOffset >= totalWidth - this.width) {
            return MaxOffset.RIGHT;
        }
        return MaxOffset.MID;
    }

    public void selectTab(int index, boolean bl) {
        if (index >= 0 && index < tabs.size()) {
            tabManager.setCurrentTab(tabs.get(index), true);
            if (bl) {
                parent.showWidgetsForCategory(tabs.get(index).getCategory());
            }
            parent.setFocusedOption(null);
        }
    }

    public boolean isHoveringOverAnyTab(double mouseX, double mouseY) {
        if (location != TabLocation.TOP && location != TabLocation.BOTTOM || tabs.isEmpty()) return false;

        int totalWidth = tabs.size() * (TAB_WIDTH + 4) - 4;
        int baseX = totalWidth <= this.width
                ? this.getX() + (this.width - totalWidth) / 2
                : this.getX() - (int) currentScrollOffset;

        for (int i = 0; i < tabs.size(); i++) {
            int tabX = baseX + i * (TAB_WIDTH + 4);
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
