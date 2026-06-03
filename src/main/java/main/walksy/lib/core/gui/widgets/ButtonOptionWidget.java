package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.Graphics;
import main.walksy.lib.core.utils.MainColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;

public class ButtonOptionWidget extends OptionWidget {

    private final Option<Runnable> option;
    private boolean hoveredButton;

    public ButtonOptionWidget(final OptionGroup parent, final WalksyLibConfigScreen screen, final int x, final int y, final int width, final int height, final Option<Runnable> option) {
        super(parent, screen, option, x, y, width, height, option.getName());
        this.option = option;
        this.hoveredButton = false;
        this.resetButton.visible = false;
    }

    @Override
    public void extract(final Graphics graphics, final int mouseX, final int mouseY, final float delta) {
        final GuiGraphicsExtractor extractor = graphics.extractor();
        this.hoveredButton = mouseX >= this.getWidth() - 30 && mouseX <= this.getWidth() - 30 + 38 &&
                mouseY >= this.getY() + 3 && mouseY <= this.getY() + 3 + this.getHeight() - 6;
        graphics.fillRoundedRectOutline(this.getWidth() - 31, this.getY() + 2, 40, this.getHeight() - 4, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
        graphics.fillRoundedRectOutline(this.getWidth() - 30, this.getY() + 3, 38, this.getHeight() - 6, 2, 1, this.hoveredButton ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
        extractor.text(this.screen.getFont(), "Press", this.getWidth() - 29 + (38 - this.screen.getFont().width("Press")) / 2, this.getTextYCentered() + 1, -1, true);
    }

    @Override
    public void onMouseClick(final MouseButtonEvent click, final boolean doubled) {
        if (this.hoveredButton && click.button() == 0) {
            if (this.option.getValue() == null) return;
            this.option.getValue().run();
            AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
        }
        super.onMouseClick(click, doubled);
    }

    @Override
    public void onWidgetUpdate() {}

    @Override
    public boolean isHovered() {
        return false;
    }
}
