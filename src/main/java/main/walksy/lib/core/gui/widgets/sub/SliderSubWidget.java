package main.walksy.lib.core.gui.widgets.sub;

import main.walksy.lib.core.gui.widgets.sub.adaptor.SliderAdapter;
import main.walksy.lib.core.renderer.Renderer2D;
import main.walksy.lib.core.utils.Animation;
import main.walksy.lib.core.utils.MainColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import java.awt.*;
import java.util.function.Consumer;

public class SliderSubWidget<T> extends SubWidget {
    private float sliderPosition = 0.0f;
    private boolean dragging = false;
    public boolean isHovered = false;
    private final SliderAdapter<T> adapter;
    private T value;
    private Consumer<T> onChange;
    private final boolean isRight;

    private final Animation sliderPositionAnimation = new Animation(sliderPosition, 0.5f);

    public SliderSubWidget(int x, int y, int width, int height, SliderAdapter<T> adapter, T initialValue, Consumer<T> onChange, boolean right) {
        super(x, y, width, height);
        this.adapter = adapter;
        this.isRight = right;
        this.onChange = onChange;
        this.setValue(initialValue);
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        sliderPositionAnimation.update(delta);
        sliderPosition = sliderPositionAnimation.getCurrentValue();

        isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        Renderer2D.fillRoundedRect(context, x, y, width, height, 1, new Color(255, 255, 255, 20).getRGB());
        Renderer2D.fillRoundedRectOutline(context, x, y, width, height, 1, 1, MainColors.OUTLINE_BLACK.getRGB());

        int v = isHovered ? 220 : 155;
        Renderer2D.fillRoundedRect(
                context,
                x + (sliderPosition * (width - 10)),
                y + (float) (height - 10) / 2,
                10, 10,
                2,
                new Color(v, v, v, 255).getRGB()
        );

        context.text(
                Minecraft.getInstance().font,
                adapter.format(value),
                isRight ? x + width + 3 : x - Minecraft.getInstance().font.width(adapter.format(adapter.getMax())) - 3,
                y + (height - 8) / 2,
                -1
        );
    }

    @Override
    public void onClick(MouseButtonEvent click, boolean doubled) {
        if (isHovered) {
            dragging = true;
            onChange(click.x());
        }
    }

    @Override
    public void onDrag(int mouseX) {
        if (dragging) {
            onChange(mouseX);
        }
    }

    private void onChange(double mouseX) {
        float targetSliderPosition = Mth.clamp((float) (mouseX - x) / width, 0.0f, 1.0f);
        sliderPositionAnimation.setTargetValue(targetSliderPosition);
        value = adapter.fromSliderPosition(targetSliderPosition);
        this.onChange.accept(value);
    }

    public void setValue(T value) {
        this.value = adapter.clamp(value);
        float targetSliderPosition = Mth.clamp(adapter.toSliderPosition(this.value), 0.0f, 1.0f);
        sliderPositionAnimation.setTargetValue(targetSliderPosition);
    }

    public T getValue() {
        return value;
    }

    public void release() {
        dragging = false;
    }

    public void setOnChange(Consumer<T> onChange) {
        this.onChange = onChange;
    }
}
