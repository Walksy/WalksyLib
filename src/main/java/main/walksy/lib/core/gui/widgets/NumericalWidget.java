package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.manager.WalksyLibScreenManager;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.utils.MainColors;
import main.walksy.lib.core.utils.Renderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;

import java.awt.*;

public class NumericalWidget<T extends Number> extends OptionWidget {

    private static final int SLIDER_WIDTH = 150;
    private static final int SLIDER_HEIGHT_PAD = 6;
    private static final int KNOB_WIDTH = 12;
    private static final int VALUE_BOX_WIDTH = 38;
    private static final int SLIDER_GAP = 5;

    private final Option<T> option;
    private boolean dragging = false;
    private double animatedKnobX = -1;

    public NumericalWidget(OptionGroup parent, WalksyLibConfigScreen screen, int x, int y, int width, int height, Option<T> option) {
        super(parent, screen, option, x, y, width, height, option.getName());
        this.option = option;
    }

    @Override
    public void draw(DrawContext context, int mouseX, int mouseY, float delta) {
        if (WalksyLibScreenManager.Globals.DEBUG) {
            context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), new Color(255, 255, 255, 150).getRGB());
        }

        double min = option.getMin().doubleValue();
        double max = option.getMax().doubleValue();
        double val = option.getValue().doubleValue();
        double percent = Math.max(0.0, Math.min(1.0, (val - min) / (max - min)));

        int sliderX = getX() + getWidth() - VALUE_BOX_WIDTH - SLIDER_WIDTH - SLIDER_GAP;
        int sliderY = getY() + SLIDER_HEIGHT_PAD;
        int sliderHeight = getHeight() - (SLIDER_HEIGHT_PAD * 2);

        double targetKnobX = sliderX + percent * (SLIDER_WIDTH - KNOB_WIDTH);
        if (animatedKnobX < 0) animatedKnobX = targetKnobX;
        animatedKnobX += (targetKnobX - animatedKnobX) * 0.2;
        animatedKnobX = Math.max(sliderX, Math.min(animatedKnobX, sliderX + SLIDER_WIDTH - KNOB_WIDTH));

        int knobX = (int) animatedKnobX;

        int dividerColor = isHoveringSlider(mouseX, mouseY) ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB();
        context.drawVerticalLine(getX() + getWidth() - VALUE_BOX_WIDTH, getY(), getY() + getHeight() - 1, isHovered() ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());

        Renderer.fillRoundedRect(context, knobX, sliderY, KNOB_WIDTH, sliderHeight, 2, dividerColor);
        Renderer.fillRoundedRectOutline(context, sliderX, sliderY, SLIDER_WIDTH, sliderHeight, 2, 1, new Color(MainColors.OUTLINE_BLACK.getRed(), MainColors.OUTLINE_BLACK.getGreen(), MainColors.OUTLINE_BLACK.getBlue(), 255).getRGB());

        String display = formatValue(val);
        int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(display);
        int textX = getX() + getWidth() - (VALUE_BOX_WIDTH / 2) - (textWidth / 2);
        int textY = getY() + (getHeight() / 2 - 4);

        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, display, textX, textY, Color.LIGHT_GRAY.getRGB());
    }

    @Override
    public void onMouseClick(double mouseX, double mouseY, int button) {
        if (isHoveringSlider(mouseX, mouseY) && button == 0) {
            dragging = true;
            updateValue(mouseX);
        }
    }

    @Override
    public void onMouseRelease(double mouseX, double mouseY, int button) {
        dragging = false;
    }

    @Override
    public void onMouseDrag(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            updateValue(mouseX);
        }
    }

    @Override
    public void onWidgetUpdate() {

    }

    private void updateValue(double mouseX) {
        double min = option.getMin().doubleValue();
        double max = option.getMax().doubleValue();
        int sliderX = getX() + getWidth() - VALUE_BOX_WIDTH - SLIDER_WIDTH - SLIDER_GAP;

        double percent = (mouseX - sliderX - (KNOB_WIDTH / 2.0)) / (SLIDER_WIDTH - KNOB_WIDTH);
        percent = Math.max(0.0, Math.min(1.0, percent));

        double snapped = snap(min + percent * (max - min), option.getIncrement().doubleValue());

        if (option.getType() == Integer.class) {
            option.setValue((T) Integer.valueOf((int) snapped));
        } else {
            option.setValue((T) Double.valueOf(snapped));
        }
        onChange();
    }

    private double snap(double value, double increment) {
        return (increment <= 0) ? value : Math.round(value / increment) * increment;
    }

    private String formatValue(double val) {
        return option.getType() == Integer.class ? String.valueOf((int) val) : String.format("%.2f", val); //TODO: format this so the max num of decimal places is set from the .values in the builder
    }

    private boolean isHoveringSlider(double mouseX, double mouseY) {
        int sliderX = getX() + getWidth() - VALUE_BOX_WIDTH - SLIDER_WIDTH - SLIDER_GAP;
        int sliderY = getY() + SLIDER_HEIGHT_PAD;
        int sliderHeight = getHeight() - (SLIDER_HEIGHT_PAD * 2);

        return mouseX >= sliderX && mouseX <= sliderX + SLIDER_WIDTH &&
                mouseY >= sliderY && mouseY <= sliderY + sliderHeight;
    }
}
