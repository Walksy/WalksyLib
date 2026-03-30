package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.renderer.Renderer2D;
import main.walksy.lib.core.utils.ScreenGlobals;
import main.walksy.lib.core.utils.SearchUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class OptionGroupWidget extends AbstractWidget {

    private final OptionGroup group;
    private final List<OptionWidget> children = new ArrayList<>();
    private final WalksyLibConfigScreen parent;

    private String searchQuery = "";
    public boolean isHovered;

    public OptionGroupWidget(int x, int y, int width, int height, OptionGroup group, WalksyLibConfigScreen parent) {
        super(x, y, width, height, Component.literal(group.getName()));
        this.parent = parent;
        this.group = group;
        isHovered = false;
        int yOff = y + 20;
        for (Option<?> option : group.getOptions()) {
            OptionWidget optionWidget = option.createWidget(group, parent, 15, yOff, ScreenGlobals.OPTION_WIDTH, ScreenGlobals.OPTION_HEIGHT);
            children.add(optionWidget);
            yOff += 30;
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float a) {
        context.enableScissor(0, 49, parent.width, parent.height - 28);
        Minecraft client = Minecraft.getInstance();
        Font textRenderer = client.font;

        String text = group.getName();
        int textWidth = textRenderer.width(text);
        int fontHeight = textRenderer.lineHeight;

        int centerX = getX() + textWidth / 2;
        int hoverPadding = 58;
        int hoverHeight = fontHeight + 4;

        isHovered = context.containsPointInScissor(mouseX, mouseY)
                && mouseY >= getY() - 2 && mouseY < getY() - 2 + hoverHeight
                && mouseX >= centerX - (textWidth / 2 + hoverPadding)
                && mouseX < centerX + (textWidth / 2 + hoverPadding);

        int bgColor;
        if (group.isExpanded()) {
            bgColor = 0xFFFFFFFF;
        } else if (isHovered) {
            bgColor = 0xFFDADADA;
        } else {
            bgColor = 0xFFAAAAAA;
        }


        int midY = getY() + fontHeight / 2;
        int textCenterX = getX();
        int textStartX = textCenterX - textWidth / 2;
        int textEndX = textCenterX + textWidth / 2;

        //LEFT
        context.horizontalLine(textStartX - 50, textStartX - 8, midY - 1, bgColor);
        context.horizontalLine(textStartX - 50, textStartX - 8, midY, bgColor);
        Renderer2D.renderMiniArrow(
                context,
                textStartX - 50 - 5,
                midY - (group.isExpanded() ? 1 : 0),
                1F,
                group.isExpanded() ? Renderer2D.ArrowDirection.DOWN : Renderer2D.ArrowDirection.RIGHT,
                bgColor
        );

        //RIGHT
        context.horizontalLine(textEndX + 5, textEndX + 50, midY - 1, bgColor);
        context.horizontalLine(textEndX + 5, textEndX + 50, midY, bgColor);
        Renderer2D.renderMiniArrow(
                context,
                textEndX + 50 + 6,
                midY - (group.isExpanded() ? 1 : 0),
                1F,
                group.isExpanded() ? Renderer2D.ArrowDirection.DOWN : Renderer2D.ArrowDirection.LEFT,
                bgColor
        );


        if (ScreenGlobals.DEBUG) {
            renderDebug(
                    context,
                    centerX - (textWidth / 2 + hoverPadding),
                    getY() - 2,
                    centerX + (textWidth / 2 + hoverPadding),
                    getY() - 2 + hoverHeight
            );
        }

        context.text(textRenderer, text, getX() - textWidth / 2, getY(), bgColor);
        context.disableScissor();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {

    }

    private void renderDebug(GuiGraphicsExtractor context, int x1, int y1, int x2, int y2) {
        context.fill(x1, y1, x2, y2, 0xAAFFFFFF);
    }

    public void onMouseClick(MouseButtonEvent click, boolean doubled)
    {
        if (isHovered && click.button() == 0) {
            group.toggleExpanded();
            parent.layoutGroupWidgets();
            updateVisibility();
        }
    }


    public void updateVisibility() {
        boolean expanded = this.group.isExpanded();
        for (OptionWidget child : children) {
            child.visible = expanded;
        }
    }

    public boolean searched(boolean shouldLevenshtein) {
        if (searchQuery.isEmpty()) return true;

        String[] queryWords = searchQuery.toLowerCase().trim().split("\\s+");
        String[] nameWords = group.getName().toLowerCase().trim().split("\\s+");

        outer:
        for (String qWord : queryWords) {
            for (String nameWord : nameWords) {
                if (nameWord.contains(qWord) || nameWord.startsWith(qWord)) {
                    continue outer;
                }
                if (shouldLevenshtein) {
                    if (SearchUtils.levenshteinDistance(nameWord, qWord) <= 2) {
                        continue outer;
                    }
                }
            }
            return false;
        }

        return true;
    }

    public void updateSearchQuery(String query) {
        this.searchQuery = query;
    }

    public List<OptionWidget> getChildren() {
        return children;
    }

    public OptionGroup getGroup() {
        return group;
    }
}
