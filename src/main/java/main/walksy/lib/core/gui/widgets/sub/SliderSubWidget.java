package main.walksy.lib.core.gui.widgets.sub;

import main.walksy.lib.core.gui.widgets.sub.adaptor.SliderAdapter;
import main.walksy.lib.core.gui.Graphics;
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

    private final Animation sliderPositionAnimation = new Animation(this.sliderPosition, 0.5f);

    public SliderSubWidget(final int x, final int y, final int width, final int height, final SliderAdapter<T> adapter, final T initialValue, final Consumer<T> onChange, final boolean right) {
        super(x, y, width, height);
        this.adapter = adapter;
        this.isRight = right;
        this.onChange = onChange;
        this.setValue(initialValue);
    }

    @Override
    public void render(final GuiGraphicsExtractor extractor, final int mouseX, final int mouseY, final float delta) {
        this.sliderPositionAnimation.update(delta);
        this.sliderPosition = this.sliderPositionAnimation.getCurrentValue();

        this.isHovered = mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;

        final Graphics g = new Graphics(extractor);
        g.fillRoundedRect(this.x, this.y, this.width, this.height, 1, new Color(255, 255, 255, 20).getRGB());
        g.fillRoundedRectOutline(this.x, this.y, this.width, this.height, 1, 1, MainColors.OUTLINE_BLACK.getRGB());

        final int v = this.isHovered ? 220 : 155;
        g.fillRoundedRect(
                this.x + (this.sliderPosition * (this.width - 10)),
                this.y + (float) (this.height - 10) / 2,
                10, 10,
                2,
                new Color(v, v, v, 255).getRGB()
        );

        extractor.text(
                Minecraft.getInstance().font,
                this.adapter.format(this.value),
                this.isRight ? this.x + this.width + 3 : this.x - Minecraft.getInstance().font.width(this.adapter.format(this.adapter.getMax())) - 3,
                this.y + (this.height - 8) / 2,
                -1
        );
    }

    @Override
    public void onClick(final MouseButtonEvent click, final boolean doubled) {
        if (this.isHovered) {
            this.dragging = true;
            this.onChange(click.x());
        }
    }

    @Override
    public void onDrag(final int mouseX) {
        if (this.dragging) {
            this.onChange(mouseX);
        }
    }

    private void onChange(final double mouseX) {
        final float targetSliderPosition = Mth.clamp((float) (mouseX - this.x) / this.width, 0.0f, 1.0f);
        this.sliderPositionAnimation.setTargetValue(targetSliderPosition);
        this.value = this.adapter.fromSliderPosition(targetSliderPosition);
        this.onChange.accept(this.value);
    }

    public void setValue(final T value) {
        this.value = this.adapter.clamp(value);
        final float targetSliderPosition = Mth.clamp(this.adapter.toSliderPosition(this.value), 0.0f, 1.0f);
        this.sliderPositionAnimation.setTargetValue(targetSliderPosition);
    }

    public T getValue() {
        return this.value;
    }

    public void release() {
        this.dragging = false;
    }

    public void setOnChange(final Consumer<T> onChange) {
        this.onChange = onChange;
    }
}
