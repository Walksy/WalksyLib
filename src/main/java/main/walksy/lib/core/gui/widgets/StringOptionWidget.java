package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.widgets.sub.TextboxSubWidget;
import main.walksy.lib.core.utils.MainColors;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.awt.*;

public class StringOptionWidget extends OptionWidget {

    private final Option<String> option;
    private final TextboxSubWidget textbox;

    public StringOptionWidget(final OptionGroup parent, final WalksyLibConfigScreen screen, final int x, final int y, final int width, final int height, final Option<String> option) {
        super(parent, screen, option, x, y, width, height, option.getName());
        this.option = option;
        this.textbox = new TextboxSubWidget(screen, this.getWidth() - 30, this.getTextYCentered() - 4, this.getWidth(), 35, 10, option.getValue(), option::setValue, false);
    }

    @Override
    public void draw(final GuiGraphicsExtractor context, final int mouseX, final int mouseY, final float delta) {
        context.verticalLine(this.getWidth() - 33 - this.textbox.getScrollOffset(), this.getY(), this.getY() + this.height - 1, this.isHovered() ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
        this.textbox.render(context, mouseX, mouseY, delta);
        this.textbox.hovered = this.isHovered();
    }

    @Override
    public void onMouseClick(final MouseButtonEvent click, final boolean doubled) {
        super.onMouseClick(click, doubled);
        this.textbox.onClick(click, doubled);
    }

    @Override
    public void onKeyPress(final KeyEvent input) {
        super.onKeyPress(input);
        this.textbox.onKeyPress(input);
    }

    @Override
    public void onCharTyped(final CharacterEvent input) {
        super.onCharTyped(input);
        this.textbox.onCharTyped(input);
    }

    @Override
    public void onWidgetUpdate() {
        this.textbox.setPos(new Point(this.getWidth() - 30, this.getTextYCentered() - 4));
    }

    @Override
    public <V> void onThirdPartyChange(final V value) {
        super.onThirdPartyChange(value);
        this.textbox.setText(this.option.getValue());
    }
}
