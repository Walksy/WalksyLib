package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.Graphics;
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


    public OptionWidget(final OptionGroup parent, final WalksyLibConfigScreen screen, final Option<?> option, final int x, final int y, final int width, final int height, final String name) {
        super(x, y, width, height, Component.literal(name));
        this.setPosition(x, y);
        this.parent = parent;
        this.option = option;
        this.screen = screen;
        this.changesMade = false;
        this.isHovered = false;

        final int size = ScreenGlobals.OPTION_HEIGHT;
        this.resetButton = new ButtonWidget(this.getX() + this.getWidth() - size + 15, this.getY(), size, size, false, Identifier.fromNamespaceAndPath("walksylib", "gui/widget/reset.png"), this::handleResetButtonClick, -1, 0);
        this.resetButton.setEnabled(option.hasChanged());
    }

    @Override
    protected void extractWidgetRenderState(final GuiGraphicsExtractor extractor, final int mouseX, final int mouseY, final float delta) {
        if (!this.isVisible()) {
            return;
        }
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.isHovered = (mouseX >= this.getX() && mouseX < (this.getX() + this.getWidth()) && mouseY >= this.getY() && mouseY < (this.getY() + this.getHeight()));
        if (this.isHovered) {
            this.screen.setFocusedOption(this.option);
        }
        final int scissorX1 = 0;
        final int scissorY1 = 49;
        final int scissorX2 = this.screen.width;
        final int scissorY2 = this.screen.height - 28;
        if (this.getX() + this.getWidth() < scissorX1 || this.getX() > scissorX2 || this.getY() + this.getHeight() < scissorY1 || this.getY() > scissorY2) {
            return;
        }
        final Graphics graphics = this.screen.currentGraphicsContext();
        extractor.enableScissor(scissorX1, scissorY1, scissorX2, scissorY2);
        graphics.fillRoundedRectOutline(this.getX(), this.getY(), this.getWidth(), this.getHeight(), 2, 1, this.isHovered() ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
        graphics.fillRoundedRectOutline(this.getX() - 1, this.getY() - 1, this.getWidth() + 2, this.getHeight() + 2, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
        extractor.text(this.screen.getFont(), this.option.getName(), this.getX() + 5, this.getTextYCentered() + 1, -1);
        this.resetButton.setEnabled(this.option.hasChanged() && this.isAvailable());
        this.resetButton.extractWidgetRenderState(extractor, mouseX, mouseY, delta);
        extractor.enableScissor(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight() - 1);
        this.extract(graphics, this.isAvailable() ? mouseX : 0, this.isAvailable() ? mouseY : 0, delta);
        extractor.disableScissor();
        if (!this.isAvailable()) {
            graphics.fillRoundedRect(this.getX() - 1, this.getY() - 1, this.getWidth() + 2, this.getHeight() + 2, 2, new Color(0, 0, 0, 180).getRGB());
            if (this.isHovered) {
                extractor.setTooltipForNextFrame(Component.literal(this.option.getAvailabilityHelper()), mouseX, mouseY);
            }
        }
        extractor.disableScissor();
    }

    public void onMouseClick(final MouseButtonEvent click, final boolean doubled) {}
    public void onMouseRelease(final MouseButtonEvent click) {}
    public void onMouseDrag(final MouseButtonEvent click, final double offsetX, final double offsetY) {}
    public void onMouseMove(final double mouseX, final double mouseY) {}
    public void onMouseScroll(final double mouseX, final double mouseY, final double verticalAmount) {}
    public void tick() {}
    public void onKeyPress(final KeyEvent input) {}
    public void onCharTyped(final CharacterEvent input) {}
    public void onWidgetUpdate(final int x, final int y) {
        this.resetButton.setPosition(x, y);
        this.onWidgetUpdate();
    }

    public abstract void onWidgetUpdate();

    public abstract void extract(Graphics graphics, int mouseX, int mouseY, float delta);


    public boolean isAvailable() {
        return this.option.isAvailable();
    }

    protected void handleResetButtonClick() {
        this.option.reset();
        this.onThirdPartyChange(this.option.getDefaultValue());
    }

    protected int getTextYCentered() {
        final int textHeight = this.screen.getFont().lineHeight;
        return this.getY() + (ScreenGlobals.OPTION_HEIGHT - textHeight) / 2;
    }

    public boolean isHovered() {
        return this.isAvailable() && this.isHovered && this.isVisible();
    }

    public boolean isVisible() {
        return this.parent.isExpanded() && this.option.searched();
    }

    public boolean isInScissor(final int scissorX, final int scissorY, final int scissorWidth, final int scissorHeight) {
        return this.getX() + this.getWidth() > scissorX &&
                this.getX() < scissorX + scissorWidth &&
                this.getY() + this.getHeight() - 2 > scissorY &&
                this.getY() < scissorY + scissorHeight;
    }

    public void updateSearchQuery(final String searchQuery) {
        this.option.updateSearchQ(searchQuery);
    }

    public void update() {
        this.screen.layoutGroupWidgets();
    }

    public void onChange() {
        this.screen.onChangesMade(this.option);
    }

    public <V> void onThirdPartyChange(final V value) {}

    public OptionGroup getParent() {
        return this.parent;
    }

    public Option<?> getOption() {
        return this.option;
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput output) {}
}
