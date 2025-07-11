package main.walksy.lib.core.gui.widgets;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import main.walksy.lib.core.config.impl.Option;
import main.walksy.lib.core.config.impl.options.groups.OptionGroup;
import main.walksy.lib.core.gui.WalksyLibScreenManager;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.utils.MainColors;
import main.walksy.lib.core.utils.Renderer;

import java.awt.*;

public class BooleanWidget extends OptionWidget {

    private final Option<Boolean> option;

    public BooleanWidget(OptionGroup parent, WalksyLibConfigScreen screen, int x, int y, int width, int height, Option<Boolean> option) {
        super(parent, screen, option, x, y, width, height, option.getName());
        this.option = option;
    }

    @Override
    public void draw(DrawContext context, int mouseX, int mouseY, float delta) {
        if (WalksyLibScreenManager.Globals.DEBUG)
        {
            renderDebug(context);
        }

        Renderer.fillRoundedRectOutline(context, getWidth() - 38, getY() + 3, 25, getHeight() - 6, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
        int x = option.getValue() ? getWidth() - 28 : getWidth() - 37;
        int color = option.getValue() ? Color.WHITE.getRGB() : MainColors.OUTLINE_WHITE.getRGB();
        Renderer.fillRoundedRect(context, x, getY() + 4, 14, getHeight() - 8, 2, color);
    }


    @Override
    public void onMouseClick(double mouseX, double mouseY, int button) {
        if (isHovered() && button == 0)
        {
            option.setValue(!option.getValue());
        }
    }

    private void renderDebug(DrawContext context)
    {
        context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), new Color(255, 255, 255, 150).getRGB());
    }


    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {

    }
}
