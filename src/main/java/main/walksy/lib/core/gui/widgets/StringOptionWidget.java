package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.widgets.sub.TextboxSubWidget;
import main.walksy.lib.core.utils.MainColors;
import net.minecraft.client.gui.DrawContext;

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
    public void draw(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawVerticalLine(getWidth() - 33 - this.textbox.getScrollOffset(), getY(), getY() + height - 1, isHovered() ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
        this.textbox.render(context, mouseX, mouseY, delta);
        this.textbox.hovered = this.isHovered();
    }

    @Override
    public void onMouseClick(double mouseX, double mouseY, int button) {
        super.onMouseClick(mouseX, mouseY, button);
        this.textbox.onClick((int) mouseX, (int) mouseY, button);
    }

    @Override
    public void onKeyPress(int keyCode, int scanCode, int modifiers) {
        super.onKeyPress(keyCode, scanCode, modifiers);
        this.textbox.onKeyPress(keyCode, scanCode, modifiers);
    }

    @Override
    public void onCharTyped(char chr, int modifiers) {
        super.onCharTyped(chr, modifiers);
        this.textbox.onCharTyped(chr, modifiers);
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
