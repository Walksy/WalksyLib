package walksy.lib.core.gui.widgets;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import walksy.lib.core.config.impl.Option;
import walksy.lib.core.config.impl.options.groups.OptionGroup;
import walksy.lib.core.gui.WalksyLibScreenManager;
import walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import walksy.lib.core.utils.MainColors;
import walksy.lib.core.utils.Renderer;
import walksy.lib.core.utils.SearchUtils;

import java.awt.*;

public abstract class OptionWidget extends AbstractWidget {

    private final OptionGroup parent;
    private final Option<?> option;
    private String searchQuery;
    private boolean isHovered;

    private WalksyLibConfigScreen screen;

    public boolean changesMade;

    public OptionWidget(OptionGroup parent, WalksyLibConfigScreen screen, Option<?> option, int x, int y, int width, int height, String name) {
        super(x, y, width, height, Text.of(name));
        this.setPosition(x, y);
        this.parent = parent;
        this.option = option;
        this.searchQuery = "";
        this.screen = screen;
        this.changesMade = false;
        this.isHovered = false;
    }


    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!isVisible()) return;

        int hoverLeft = getX();

        isHovered = mouseX >= getX() && mouseX < (getX() + getWidth() - 6)
                && mouseY >= getY() && mouseY < (getY() + getHeight());


        if (isHovered())
        {
            screen.setFocusedOption(option);
        }

        context.enableScissor(0, 49, screen.width, screen.height - 28);
        if (WalksyLibScreenManager.Globals.DEBUG) {
            renderHoverBackground(context, hoverLeft, getWidth());
        }
        renderBase(context);
        renderResetButton(context);

        draw(context, mouseX, mouseY, delta);
        context.disableScissor();
    }

    public abstract void onMouseClick(double mouseX, double mouseY, int button);

    private void renderBase(DrawContext context)
    {
        Renderer.fillRoundedRectOutline(context, getX(), getY(), getWidth() - 22, getHeight(), 2, 1, isHovered() ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
        Renderer.fillRoundedRectOutline(context, getX() - 1, getY() - 1, getWidth() - 20, getHeight() + 2, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
        renderName(context);
    }

    protected void renderName(DrawContext context)
    {
        context.drawTextWithShadow(screen.getTextRenderer(),
                option.getName(),
                getX() + 5,
                getTextYCentered() + 1,
                -1);
    }

    //TODO ICON
    private void renderResetButton(DrawContext context) {
        int size = getHeight();
        int x = getWidth() - size - 4;
        int y = getY();

        Color outlineColor = changesMade ? new Color(255, 255, 255, 80) : new Color(180, 180, 180, 50);
        Color shadowColor = changesMade ? new Color(0, 0, 0, 191) : new Color(30, 30, 30, 120);

        int buttonX = x + 19;
        int buttonY = y;
        int centerX = buttonX + size / 2;
        int centerY = buttonY + size / 2;

        Renderer.fillRoundedRectOutline(context, buttonX, buttonY, size, size, 2, 1, outlineColor.getRGB());
        Renderer.fillRoundedRectOutline(context, buttonX - 1, buttonY - 1, size + 2, size + 2, 2, 1, shadowColor.getRGB());
        Renderer.renderCircleArrow(context, centerX, centerY, Renderer.ArrowDirection.UP, active ? 0xFFFFFFFF : 0xFFAAAAAA);
    }


    protected void renderHoverBackground(DrawContext context, int hoverLeft, int hoverRight) {
        if (isHovered()) {
            context.fill(hoverLeft, getY(), hoverRight, getY() + getHeight(), 0x64FFFFFF);
        }
    }


    protected int getTextYCentered() {
        int textHeight = screen.getTextRenderer().fontHeight;
        return getY() + (getHeight() - textHeight) / 2;
    }

    public boolean isHovered() {
        return isHovered && isVisible();
    }

    public abstract void draw(DrawContext context, int mouseX, int mouseY, float delta);

    public boolean isVisible() {
        return parent.isExpanded() && searched();
    }

    public boolean searched() {
        if (searchQuery.isEmpty()) return true;

        String[] queryWords = searchQuery.toLowerCase().trim().split("\\s+");
        String[] nameWords = option.getName().toLowerCase().trim().split("\\s+");

        outer:
        for (String qWord : queryWords) {
            for (String numWord : nameWords) {
                if (numWord.contains(qWord) || numWord.startsWith(qWord)) {
                    continue outer;
                }
                if (SearchUtils.levenshteinDistance(numWord, qWord) <= 2) {
                    continue outer;
                }
            }
            return false;
        }
        return true;
    }

    public void updateSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public OptionGroup getParent() {
        return parent;
    }

    public Option<?> getOption() {
        return option;
    }
}
