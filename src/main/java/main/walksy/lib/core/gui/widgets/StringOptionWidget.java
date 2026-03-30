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

    public StringOptionWidget(OptionGroup parent, WalksyLibConfigScreen screen, int x, int y, int width, int height, Option<String> option) {
        super(parent, screen, option, x, y, width, height, option.getName());
        this.option = option;
        this.textbox = new TextboxSubWidget(screen, getWidth() - 30, getTextYCentered() - 4, getWidth(), 35, 10, option.getValue(), option::setValue, false);
    }

    @Override
    public void draw(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.verticalLine(getWidth() - 33 - this.textbox.getScrollOffset(), getY(), getY() + height - 1, isHovered() ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
        this.textbox.render(context, mouseX, mouseY, delta);
        this.textbox.hovered = this.isHovered();
    }

    @Override
    public void onMouseClick(MouseButtonEvent click, boolean doubled) {
        super.onMouseClick(click, doubled);
        this.textbox.onClick(click, doubled);
    }

    @Override
    public void onKeyPress(KeyEvent input) {
        super.onKeyPress(input);
        this.textbox.onKeyPress(input);
    }

    @Override
    public void onCharTyped(CharacterEvent input) {
        super.onCharTyped(input);
        this.textbox.onCharTyped(input);
    }

    @Override
    public void onWidgetUpdate() {
        this.textbox.setPos(new Point(getWidth() - 30, getTextYCentered() - 4));
    }

    @Override
    public <V> void onThirdPartyChange(V value) {
        super.onThirdPartyChange(value);
        this.textbox.setText(this.option.getValue());
    }
}
