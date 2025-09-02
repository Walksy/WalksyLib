package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.config.local.options.BooleanOption;
import main.walksy.lib.core.gui.popup.impl.WarningPopUp;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.manager.WalksyLibScreenManager;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.utils.MainColors;
import main.walksy.lib.core.utils.Renderer;

import java.awt.*;
import java.util.function.UnaryOperator;

public class BooleanWidget extends OptionWidget {

    private final Option<Boolean> option;
    private final WarningPopUp warningPopUp;


    public BooleanWidget(OptionGroup parent, WalksyLibConfigScreen screen, int x, int y, int width, int height, Option<Boolean> option, BooleanOption.Warning warning) {
        super(parent, screen, option, x, y, width, height, option.getName());
        this.option = option;
        if (warning != null) {
            this.warningPopUp = new WarningPopUp(screen, warning.title, warning.message,
                    () ->
                    {
                        option.setValue(!option.getValue());
                        onChange();
                        if (warning.onYes != null) {
                            warning.onYes.run();
                        }
                        this.screen.popUp = null;
                    },
                    () ->
                    {
                        if (warning.onNo != null) {
                            warning.onNo.run();
                        }
                        this.screen.popUp = null;
                    });
        } else {
            this.warningPopUp = null;
        }
    }

    @Override
    public void draw(DrawContext context, int mouseX, int mouseY, float delta) {
        if (WalksyLibScreenManager.Globals.DEBUG)
        {
            renderDebug(context);
        }

        Renderer.fillRoundedRectOutline(context, getWidth() - 16, getY() + 3, 25, getHeight() - 6, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
        int x = option.getValue() ? getWidth() - 6 : getWidth() - 15;
        int color = option.getValue() ? Color.WHITE.getRGB() : MainColors.OUTLINE_WHITE.getRGB();
        Renderer.fillRoundedRect(context, x, getY() + 4, 14, getHeight() - 8, 2, color);
    }


    @Override
    public void onMouseClick(double mouseX, double mouseY, int button) {
        if (isHovered() && button == 0)
        {
            if (this.warningPopUp != null && !this.warningPopUp.visible && !this.option.getValue())
            {
                this.screen.popUp = this.warningPopUp;
                return;
            }
            option.setValue(!option.getValue());
            onChange();
        }
    }

    @Override
    public void onWidgetUpdate() {

    }


    private void renderDebug(DrawContext context)
    {
        context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), new Color(255, 255, 255, 150).getRGB());
    }
}
