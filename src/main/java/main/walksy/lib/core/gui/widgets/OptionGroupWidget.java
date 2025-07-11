package main.walksy.lib.core.gui.widgets;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;
import main.walksy.lib.core.config.impl.Option;
import main.walksy.lib.core.config.impl.options.groups.OptionGroup;
import main.walksy.lib.core.gui.WalksyLibScreenManager;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.utils.Renderer;
import main.walksy.lib.core.utils.SearchUtils;

import java.util.ArrayList;
import java.util.List;

public class OptionGroupWidget extends AbstractWidget {

    private final OptionGroup group;
    private final List<OptionWidget> children = new ArrayList<>();
    private final WalksyLibConfigScreen parent;

    private String searchQuery = "";
    public boolean isHovered;

    public OptionGroupWidget(int x, int y, int width, int height, OptionGroup group, WalksyLibConfigScreen parent) {
        super(x, y, width, height, Text.of(group.getName()));
        this.parent = parent;
        this.group = group;
        isHovered = false;
        int yOff = y + 20;
        for (Option<?> option : group.getOptions()) {
            OptionWidget optionWidget = option.createWidget(group, parent, 15, yOff, WalksyLibScreenManager.Globals.OPTION_WIDTH, WalksyLibScreenManager.Globals.OPTION_HEIGHT);
            children.add(optionWidget);
            yOff += 30;
        }
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        context.enableScissor(0, 49, parent.width, parent.height - 28);
        MinecraftClient client = MinecraftClient.getInstance();
        //System.out.println(group.isExpanded() + " " + isHovered);
        TextRenderer textRenderer = client.textRenderer;

        String text = group.getName();
        int textWidth = textRenderer.getWidth(text);
        int fontHeight = textRenderer.fontHeight;

        int centerX = getX() + textWidth / 2;
        int hoverPadding = 58;
        int hoverHeight = fontHeight + 4;

        isHovered = context.scissorContains(mouseX, mouseY)
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
        context.drawHorizontalLine(textStartX - 50, textStartX - 8, midY - 1, bgColor);
        context.drawHorizontalLine(textStartX - 50, textStartX - 8, midY, bgColor);
        Renderer.renderMiniArrow(
                context,
                textStartX - 50 - 5,
                midY - (group.isExpanded() ? 1 : 0),
                1F,
                group.isExpanded() ? Renderer.ArrowDirection.DOWN : Renderer.ArrowDirection.RIGHT,
                bgColor
        );

        //RIGHT
        context.drawHorizontalLine(textEndX + 5, textEndX + 50, midY - 1, bgColor);
        context.drawHorizontalLine(textEndX + 5, textEndX + 50, midY, bgColor);
        Renderer.renderMiniArrow(
                context,
                textEndX + 50 + 6,
                midY - (group.isExpanded() ? 1 : 0),
                1F,
                group.isExpanded() ? Renderer.ArrowDirection.DOWN : Renderer.ArrowDirection.LEFT,
                bgColor
        );


        if (WalksyLibScreenManager.Globals.DEBUG) {
            renderDebug(
                    context,
                    centerX - (textWidth / 2 + hoverPadding),
                    getY() - 2,
                    centerX + (textWidth / 2 + hoverPadding),
                    getY() - 2 + hoverHeight
            );
        }

        context.drawTextWithShadow(textRenderer, text, getX() - textWidth / 2, getY(), bgColor);
        context.disableScissor();
    }

    private void renderDebug(DrawContext context, int x1, int y1, int x2, int y2)
    {
        context.fill(x1, y1, x2, y2, 0xAAFFFFFF);
    }

    public void onMouseClick(double mouseX, double mouseY, int button)
    {
        if (isHovered && button == 0) {
            //TODO probably move this to it's own method in the parent class, a lot of important stuff here
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

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

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
