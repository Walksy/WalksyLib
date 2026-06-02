package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.Graphics;
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

    public OptionGroupWidget(final int x, final int y, final int width, final int height, final OptionGroup group, final WalksyLibConfigScreen parent) {
        super(x, y, width, height, Component.literal(group.getName()));
        this.parent = parent;
        this.group = group;
        this.isHovered = false;
        int yOff = y + 20;
        for (final Option<?> option : group.getOptions()) {
            final OptionWidget optionWidget = option.createWidget(group, parent, 15, yOff, ScreenGlobals.OPTION_WIDTH, ScreenGlobals.OPTION_HEIGHT);
            this.children.add(optionWidget);
            yOff += 30;
        }
    }

    @Override
    protected void extractWidgetRenderState(final GuiGraphicsExtractor context, final int mouseX, final int mouseY, final float a) {
        context.enableScissor(0, 49, this.parent.width, this.parent.height - 28);
        final Minecraft client = Minecraft.getInstance();
        final Font textRenderer = client.font;

        final String text = this.group.getName();
        final int textWidth = textRenderer.width(text);
        final int fontHeight = textRenderer.lineHeight;

        final int centerX = this.getX() + textWidth / 2;
        final int hoverPadding = 58;
        final int hoverHeight = fontHeight + 4;

        this.isHovered = context.containsPointInScissor(mouseX, mouseY)
                && mouseY >= this.getY() - 2 && mouseY < this.getY() - 2 + hoverHeight
                && mouseX >= centerX - (textWidth / 2 + hoverPadding)
                && mouseX < centerX + (textWidth / 2 + hoverPadding);

        final int bgColor;
        if (this.group.isExpanded()) {
            bgColor = 0xFFFFFFFF;
        } else if (this.isHovered) {
            bgColor = 0xFFDADADA;
        } else {
            bgColor = 0xFFAAAAAA;
        }

        final int midY = this.getY() + fontHeight / 2;
        final int textCenterX = this.getX();
        final int textStartX = textCenterX - textWidth / 2;
        final int textEndX = textCenterX + textWidth / 2;

        context.horizontalLine(textStartX - 50, textStartX - 8, midY - 1, bgColor);
        context.horizontalLine(textStartX - 50, textStartX - 8, midY, bgColor);
        new Graphics(context).renderMiniArrow(
                textStartX - 50 - 5,
                midY - (this.group.isExpanded() ? 1 : 0),
                1F,
                this.group.isExpanded() ? Graphics.ArrowDirection.DOWN : Graphics.ArrowDirection.RIGHT,
                bgColor
        );

        context.horizontalLine(textEndX + 5, textEndX + 50, midY - 1, bgColor);
        context.horizontalLine(textEndX + 5, textEndX + 50, midY, bgColor);
        new Graphics(context).renderMiniArrow(
                textEndX + 50 + 6,
                midY - (this.group.isExpanded() ? 1 : 0),
                1F,
                this.group.isExpanded() ? Graphics.ArrowDirection.DOWN : Graphics.ArrowDirection.LEFT,
                bgColor
        );

        if (ScreenGlobals.DEBUG) {
            this.renderDebug(
                    context,
                    centerX - (textWidth / 2 + hoverPadding),
                    this.getY() - 2,
                    centerX + (textWidth / 2 + hoverPadding),
                    this.getY() - 2 + hoverHeight
            );
        }

        context.text(textRenderer, text, this.getX() - textWidth / 2, this.getY(), bgColor);
        context.disableScissor();
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput output) {}

    private void renderDebug(final GuiGraphicsExtractor context, final int x1, final int y1, final int x2, final int y2) {
        context.fill(x1, y1, x2, y2, 0xAAFFFFFF);
    }

    public void onMouseClick(final MouseButtonEvent click, final boolean doubled) {
        if (this.isHovered && click.button() == 0) {
            this.group.toggleExpanded();
            this.parent.layoutGroupWidgets();
            this.updateVisibility();
        }
    }

    public void updateVisibility() {
        final boolean expanded = this.group.isExpanded();
        for (final OptionWidget child : this.children) {
            child.visible = expanded;
        }
    }

    public boolean searched(final boolean shouldLevenshtein) {
        if (this.searchQuery.isEmpty()) return true;

        final String[] queryWords = this.searchQuery.toLowerCase().trim().split("\\s+");
        final String[] nameWords = this.group.getName().toLowerCase().trim().split("\\s+");

        outer:
        for (final String qWord : queryWords) {
            for (final String nameWord : nameWords) {
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

    public void updateSearchQuery(final String query) {
        this.searchQuery = query;
    }

    public List<OptionWidget> getChildren() {
        return this.children;
    }

    public OptionGroup getGroup() {
        return this.group;
    }
}
