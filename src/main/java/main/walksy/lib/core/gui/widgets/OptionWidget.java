package main.walksy.lib.core.gui.widgets;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;
import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.manager.WalksyLibScreenManager;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.utils.MainColors;
import main.walksy.lib.core.utils.Renderer;
import main.walksy.lib.core.utils.SearchUtils;
import net.minecraft.util.Identifier;

import java.awt.*;

public abstract class OptionWidget extends AbstractWidget {

    private final OptionGroup parent;
    private final Option<?> option;
    private String searchQuery;
    private boolean isHovered;

    public WalksyLibConfigScreen screen;
    public ButtonWidget resetButton;

    public boolean changesMade;
    public int mouseX, mouseY;

    public OptionWidget(OptionGroup parent, WalksyLibConfigScreen screen, Option<?> option, int x, int y, int width, int height, String name) {
        super(x, y, width, height, Text.of(name));
        this.setPosition(x, y);
        this.parent = parent;
        this.option = option;
        this.searchQuery = "";
        this.screen = screen;
        this.changesMade = false;
        this.isHovered = false;

        int size = WalksyLibScreenManager.Globals.OPTION_HEIGHT;
        resetButton = new ButtonWidget(getX() + getWidth() - size + 15, getY(), size, size, false, Identifier.of("walksylib", "gui/widget/reset.png"), this::handleResetButtonClick, -1, 0);
        resetButton.setEnabled(option.hasChanged());
    }


    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!isVisible()) return;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        int hoverLeft = getX();

        isHovered = mouseX >= getX() && mouseX < (getX() + getWidth())
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
        //renderResetButton(context);
        resetButton.render(context, mouseX, mouseY, delta);
        resetButton.setEnabled(option.hasChanged());
        draw(context, mouseX, mouseY, delta);
        context.disableScissor();
    }

    public void onMouseClick(double mouseX, double mouseY, int button) {}
    public void onMouseRelease(double mouseX, double mouseY, int button) {}
    public void onMouseDrag(double mouseX, double mouseY, int button, double deltaX, double deltaY) {}
    public void onMouseMove(double mouseX, double mouseY) {}
    public void onMouseScroll(double mouseX, double mouseY, double verticalAmount) {}
    public void tick() {}
    public void onWidgetUpdate(int x, int y)
    {
        this.resetButton.setPosition(x, y);
        this.onWidgetUpdate();
    }

    public abstract void onWidgetUpdate();

    private void renderBase(DrawContext context)
    {
        Renderer.fillRoundedRectOutline(context, getX(), getY(), getWidth(), getHeight(), 2, 1, isHovered() ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
        Renderer.fillRoundedRectOutline(context, getX() - 1, getY() - 1, getWidth() + 2, getHeight() + 2, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
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


    protected void handleResetButtonClick()
    {
        this.option.reset();
        this.onThirdPartyChange(this.option.getDefaultValue());
    }

    protected void renderHoverBackground(DrawContext context, int hoverLeft, int hoverRight) {
        if (isHovered()) {
            context.fill(hoverLeft, getY(), hoverRight, getY() + getHeight(), 0x64FFFFFF);
        }
    }


    protected int getTextYCentered() {
        int textHeight = screen.getTextRenderer().fontHeight;
        return getY() + (WalksyLibScreenManager.Globals.OPTION_HEIGHT - textHeight) / 2;
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

    public boolean isInScissor(int scissorX, int scissorY, int scissorWidth, int scissorHeight) {
        return getX() + getWidth() > scissorX &&
                getX() < scissorX + scissorWidth &&
                getY() + getHeight() - 2 > scissorY &&
                getY() < scissorY + scissorHeight;
    }

    public void updateSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public void update()
    {
        screen.layoutGroupWidgets();
    }

    public void onChange()
    {
        screen.onChangesMade(option);
    }

    public <V> void onThirdPartyChange(V value)
    {
    }

    public OptionGroup getParent() {
        return parent;
    }

    public Option<?> getOption() {
        return option;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {

    }
}
