package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.utils.Animation;
import main.walksy.lib.core.utils.MainColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public class EnumOptionWidget<E extends Enum<E>> extends OptionWidget {

    private final Option<E> option;
    private int maxWidth;
    private final Animation animation = new Animation(0, 0.5F);
    private boolean open;
    private E hoveredValue = null;

    public EnumOptionWidget(OptionGroup parent, WalksyLibConfigScreen screen, int x, int y, int width, int height, Option<E> option) {
        super(parent, screen, option, x, y, width, height, option.getName());
        this.option = option;
        this.open = false;
        this.recalc();
    }

    @Override
    public void draw(DrawContext context, int mouseX, int mouseY, float delta) {
        this.animation.update(delta);
    }

    @Override
    public void drawOutsideScissor(DrawContext context, int mouseX, int mouseY, float delta) {
        int rectX = getWidth() - 30 - maxWidth;
        int rectY = getY() + 3;
        int rectWidth = maxWidth + 38;
        int rectHeight = (int) (getHeight() - 6 + this.animation.getCurrentValue());

        WalksyLib.get2DRenderer().fillRoundedRect(context, rectX, rectY, rectWidth, rectHeight, 2, new Color(0, 0, 0, 220).getRGB());
        WalksyLib.get2DRenderer().fillRoundedRectOutline(context, rectX - 1, rectY - 1, rectWidth + 2, rectHeight + 2, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
        WalksyLib.get2DRenderer().fillRoundedRectOutline(context, rectX, rectY, rectWidth, rectHeight, 2, 1,
                isHoveringEnum(mouseX, mouseY)
                        ? MainColors.OUTLINE_WHITE_HOVERED.getRGB()
                        : MainColors.OUTLINE_WHITE.getRGB());
        context.enableScissor(rectX - 1, rectY - 1, rectX + rectWidth + 1, rectY + rectHeight - 1);

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int textPos = (int) (rectY + ((getHeight() - 6) / 2f) - textRenderer.fontHeight / 2f) + 1;
        context.drawCenteredTextWithShadow(
                textRenderer,
                option.getValue().name(),
                (int) (rectX + rectWidth / 2f),
                textPos,
                0xFFFFFF
        );


        if (this.animation.getCurrentValue() > 2) {

            context.drawHorizontalLine(rectX + 1, rectX + rectWidth - 2, (int) (rectY + rectHeight - this.animation.getCurrentValue()) - 1, isHoveringEnum(mouseX, mouseY) ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
            this.hoveredValue = null;

            E[] constants = option.getValue().getDeclaringClass().getEnumConstants();
            int index = 1;

            for (E value : constants) {
                if (value == option.getValue()) continue;

                int y = textPos + (textRenderer.fontHeight / 2) + index * 10;

                boolean hovered = (mouseX >= rectX && mouseX <= rectX + rectWidth && mouseY >= y && mouseY <= y + textRenderer.fontHeight) && open;
                if (hovered) this.hoveredValue = value;
                context.drawCenteredTextWithShadow(
                        textRenderer,
                        value.name(),
                        (int) (rectX + rectWidth / 2f),
                        y,
                        hovered ? 0xFFFFAA : Color.GRAY.getRGB()
                );
                index++;
            }
        }
        context.disableScissor();
    }




    @Override
    public void onMouseClick(double mouseX, double mouseY, int button) {
        super.onMouseClick(mouseX, mouseY, button);
        if (isHoveringEnum(mouseX, mouseY)) {
            this.open = !this.open;
            this.animation.setTargetValue(open ? option.getValue().getDeclaringClass().getEnumConstants().length * 10 : 0);
        } else if (hoveredValue != null) {
            this.option.setValue(this.hoveredValue);
            this.animation.setTargetValue(0);
            this.open = false;
            this.hoveredValue = null;
        }
    }

    @Override
    public void onWidgetUpdate() {

    }

    @Override
    public boolean isHovered() {
        return false;
    }

    void recalc() {
        int longestWidth = 0;
        for (E constant : option.getValue().getDeclaringClass().getEnumConstants()) {
            int width = screen.getTextRenderer().getWidth(constant.name());
            if (width > longestWidth) longestWidth = width;
        }

        maxWidth = longestWidth;
    }


    private boolean isHoveringEnum(double mouseX, double mouseY) {
        return (mouseX >= getWidth() - 30 - maxWidth &&
                mouseX <= getWidth() - 30 - maxWidth + (maxWidth + 38) &&
                mouseY >= getY() + 3 &&
                mouseY <= getY() + 3 + (getHeight() - 6 + this.animation.getCurrentValue())) && this.hoveredValue == null;
    }
}
