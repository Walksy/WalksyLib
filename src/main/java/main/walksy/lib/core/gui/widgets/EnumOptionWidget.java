package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.renderer.Renderer2D;
import main.walksy.lib.core.utils.MainColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class EnumOptionWidget<E extends Enum<E>> extends OptionWidget {

    private final Option<E> option;
    private int maxWidth;

    public EnumOptionWidget(OptionGroup parent, WalksyLibConfigScreen screen, int x, int y, int width, int height, Option<E> option) {
        super(parent, screen, option, x, y, width, height, option.getName());
        this.option = option;
        this.recalc();
    }

    @Override
    public void draw(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int rectX = getWidth() - 30 - maxWidth;
        int rectY = getY() + 3;
        int rectWidth = maxWidth + 38;
        int rectHeight = getHeight() - 6;

        boolean hovered = isHoveringEnum(mouseX, mouseY);

        Renderer2D.fillRoundedRectOutline(context, rectX - 1, rectY - 1, rectWidth + 2, rectHeight + 2, 2, 1,
                hovered ? MainColors.OUTLINE_BLACK.getRGB() : new Color(0, 0, 0, 100).getRGB());

        Renderer2D.fillRoundedRectOutline(context, rectX, rectY, rectWidth, rectHeight, 2, 1,
                hovered ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());

        Font textRenderer = Minecraft.getInstance().font;
        int textPos = (int) (rectY + (rectHeight / 2f) - textRenderer.lineHeight / 2f) + 1;

        context.centeredText(
                textRenderer,
                option.getValue().name(),
                (int) (rectX + rectWidth / 2f),
                textPos,
                hovered ? -1 : Color.LIGHT_GRAY.getRGB()
        );
    }

    @Override
    public void onMouseClick(MouseButtonEvent click, boolean doubled) {
        super.onMouseClick(click, doubled);

        if (isHoveringEnum(click.x(), click.y())) {
            E[] constants = option.getValue().getDeclaringClass().getEnumConstants();
            int currentIndex = option.getValue().ordinal();
            int nextIndex;

            if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                nextIndex = (currentIndex - 1 + constants.length) % constants.length;
            } else {
                nextIndex = (currentIndex + 1) % constants.length;
            }

            this.option.setValue(constants[nextIndex]);
        }
    }

    @Override
    public void onWidgetUpdate() {}

    @Override
    public boolean isHovered() {
        return false;
    }

    void recalc() {
        int longestWidth = 0;
        for (E constant : option.getValue().getDeclaringClass().getEnumConstants()) {
            int width = screen.getFont().width(constant.name());
            if (width > longestWidth) longestWidth = width;
        }
        maxWidth = longestWidth;
    }

    private boolean isHoveringEnum(double mouseX, double mouseY) {
        int rectX = getWidth() - 30 - maxWidth;
        int rectY = getY() + 3;
        int rectWidth = maxWidth + 38;
        int rectHeight = getHeight() - 6;

        return (mouseX >= rectX && mouseX <= rectX + rectWidth &&
                mouseY >= rectY && mouseY <= rectY + rectHeight);
    }
}