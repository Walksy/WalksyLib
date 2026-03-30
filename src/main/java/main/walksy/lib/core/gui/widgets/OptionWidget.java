package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.renderer.Renderer2D;
import main.walksy.lib.core.utils.MainColors;
import main.walksy.lib.core.utils.ScreenGlobals;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.awt.*;

public abstract class OptionWidget extends AbstractWidget {

    private final OptionGroup parent;
    private final Option<?> option;
    private boolean isHovered;

    public WalksyLibConfigScreen screen;
    public ButtonWidget resetButton;

    public boolean changesMade;
    public int mouseX, mouseY;


    public OptionWidget(OptionGroup parent, WalksyLibConfigScreen screen, Option<?> option, int x, int y, int width, int height, String name) {
        super(x, y, width, height, Component.literal(name));
        this.setPosition(x, y);
        this.parent = parent;
        this.option = option;
        this.screen = screen;
        this.changesMade = false;
        this.isHovered = false;

        int size = ScreenGlobals.OPTION_HEIGHT;
        resetButton = new ButtonWidget(getX() + getWidth() - size + 15, getY(), size, size, false, Identifier.fromNamespaceAndPath("walksylib", "gui/widget/reset.png"), this::handleResetButtonClick, -1, 0);
        resetButton.setEnabled(option.hasChanged());
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (!isVisible()) return;
        this.mouseX = mouseX;
        this.mouseY = mouseY;

        isHovered = (mouseX >= getX() && mouseX < (getX() + getWidth())
                && mouseY >= getY() && mouseY < (getY() + getHeight()));

        if (isHovered) {
            screen.setFocusedOption(option);
        }

        int scissorX1 = 0;
        int scissorY1 = 49;
        int scissorX2 = screen.width;
        int scissorY2 = screen.height - 28;

        if (getX() + getWidth() < scissorX1 || getX() > scissorX2
                || getY() + getHeight() < scissorY1 || getY() > scissorY2) {
            return;
        }

        context.enableScissor(scissorX1, scissorY1, scissorX2, scissorY2);

        renderBase(context);

        resetButton.setEnabled(option.hasChanged() && isAvailable());
        resetButton.extractWidgetRenderState(context, mouseX, mouseY, delta);
        context.enableScissor(getX(), getY(), getX() + getWidth(), getY() + getHeight() - 1);
        this.draw(context, isAvailable() ? mouseX : 0, isAvailable() ? mouseY : 0, delta);
        context.disableScissor();
        this.drawOutsideScissor(context, isAvailable() ? mouseX : 0, isAvailable() ? mouseY : 0, delta);

        if (!isAvailable()) {
            Renderer2D.fillRoundedRect(context, getX() - 1, getY() - 1, getWidth() + 2, getHeight() + 2, 2, new Color(0, 0, 0, 180).getRGB());
            if (isHovered) {
                context.setTooltipForNextFrame(Component.literal(this.option.getAvailabilityHelper()), mouseX, mouseY);
            }
        }
        context.disableScissor();
    }

    public void onMouseClick(MouseButtonEvent click, boolean doubled) {}
    public void onMouseRelease(MouseButtonEvent click) {}
    public void onMouseDrag(MouseButtonEvent click, double offsetX, double offsetY) {}
    public void onMouseMove(double mouseX, double mouseY) {}
    public void onMouseScroll(double mouseX, double mouseY, double verticalAmount) {}
    public void tick() {}
    public void onKeyPress(KeyEvent input) {}
    public void onCharTyped(CharacterEvent input) {}
    public void onWidgetUpdate(int x, int y)
    {
        this.resetButton.setPosition(x, y);
        this.onWidgetUpdate();
    }

    public abstract void onWidgetUpdate();

    public abstract void draw(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta);

    public void drawOutsideScissor(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {}

    private void renderBase(GuiGraphicsExtractor context) {
        Renderer2D.fillRoundedRectOutline(context, getX(), getY(), getWidth(), getHeight(), 2, 1, isHovered() ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
        Renderer2D.fillRoundedRectOutline(context, getX() - 1, getY() - 1, getWidth() + 2, getHeight() + 2, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
        renderName(context);
    }

    public boolean isAvailable()
    {
        return option.isAvailable();
    }

    protected void renderName(GuiGraphicsExtractor context) {
        context.text(screen.getFont(), option.getName(), getX() + 5, getTextYCentered() + 1, -1);
    }


    protected void handleResetButtonClick() {
        this.option.reset();
        this.onThirdPartyChange(this.option.getDefaultValue());
    }

    protected void renderHoverBackground(GuiGraphicsExtractor context, int hoverLeft, int hoverRight) {
        if (isHovered()) {
            context.fill(hoverLeft, getY(), hoverRight, getY() + getHeight(), 0x64FFFFFF);
        }
    }

    protected int getTextYCentered() {
        int textHeight = screen.getFont().lineHeight;
        return getY() + (ScreenGlobals.OPTION_HEIGHT - textHeight) / 2;
    }

    public boolean isHovered() {
        return isAvailable() && isHovered && isVisible();
    }

    public boolean isVisible() {
        return parent.isExpanded() && option.searched();
    }

    public boolean isInScissor(int scissorX, int scissorY, int scissorWidth, int scissorHeight) {
        return getX() + getWidth() > scissorX &&
                getX() < scissorX + scissorWidth &&
                getY() + getHeight() - 2 > scissorY &&
                getY() < scissorY + scissorHeight;
    }

    public void updateSearchQuery(String searchQuery) {
        this.option.updateSearchQ(searchQuery);
    }

    public void update()
    {
        screen.layoutGroupWidgets();
    }

    public void onChange()
    {
        screen.onChangesMade(option);
    }

    public <V> void onThirdPartyChange(V value) {
    }

    public OptionGroup getParent() {
        return parent;
    }

    public Option<?> getOption() {
        return option;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {

    }
}
