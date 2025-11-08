package main.walksy.lib.core.gui.widgets.sub;

import main.walksy.lib.core.gui.widgets.sub.adaptor.SliderAdapter;
import main.walksy.lib.core.renderer.Renderer2D;
import main.walksy.lib.core.utils.Animation;
import main.walksy.lib.core.utils.MainColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

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

    private final Animation sliderPositionAnimation= new Animation(sliderPosition, 0.5f);

    public SliderSubWidget(int x, int y, int width, int height, SliderAdapter<T> adapter, T initialValue, Consumer<T> onChange, boolean right) {
        super(x, y, width, height);
        this.adapter = adapter;
        this.isRight = right;
        this.onChange = onChange;
        this.setValue(initialValue);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        sliderPositionAnimation.update(delta);
        sliderPosition = sliderPositionAnimation.getCurrentValue();

        isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        Renderer2D.fillRoundedRect(context, x, y, width, height, 1, new Color(255, 255, 255, 20).getRGB());
        Renderer2D._fillRoundedRectOutline(context, x, y, width, height, 1, 1, MainColors.OUTLINE_BLACK.getRGB());

        int v = isHovered ? 220 : 155;
        Renderer2D.fillRoundedRect(
                context,
                (int) (x + (sliderPosition * (width - 10))),
                (int) (y + (float) (height - 10) / 2),
                10, 10,
                2,
                new Color(v, v, v, 255).getRGB()
        );

        context.drawText(
                MinecraftClient.getInstance().textRenderer,
                adapter.format(value),
                isRight ? x + width + 3 : x - MinecraftClient.getInstance().textRenderer.getWidth(adapter.format(adapter.getMax())) - 3,
                y + (height - 8) / 2,
                0xFFFFFF,
                false
        );
    }

    @Override
    public void onClick(int mouseX, int mouseY, int button) {
        if (isHovered) {
            dragging = true;
            onChange(mouseX);
        }
    }

    @Override
    public void onDrag(int mouseX) {
        if (dragging) {
            onChange(mouseX);
        }
    }

    private void onChange(int mouseX) {
        float targetSliderPosition = MathHelper.clamp((float) (mouseX - x) / width, 0.0f, 1.0f);
        sliderPositionAnimation.setTargetValue(targetSliderPosition);
        value = adapter.fromSliderPosition(targetSliderPosition);
        this.onChange.accept(value);
    }

    public void setValue(T value) {
        this.value = adapter.clamp(value);
        float targetSliderPosition = MathHelper.clamp(adapter.toSliderPosition(this.value), 0.0f, 1.0f);
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
