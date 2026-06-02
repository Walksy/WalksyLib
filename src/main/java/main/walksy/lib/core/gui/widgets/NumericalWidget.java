package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.gui.widgets.sub.SliderSubWidget;
import main.walksy.lib.core.gui.widgets.sub.adaptor.DoubleSliderAdapter;
import main.walksy.lib.core.gui.widgets.sub.adaptor.FloatSliderAdapter;
import main.walksy.lib.core.gui.widgets.sub.adaptor.IntSliderAdapter;
import main.walksy.lib.core.gui.widgets.sub.adaptor.SliderAdapter;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.awt.*;

public class NumericalWidget<T extends Number> extends OptionWidget {

    private final SliderSubWidget<T> slider;
    private final Option<T> option;

    public NumericalWidget(final OptionGroup parent, final WalksyLibConfigScreen screen, final int x, final int y, final int width, final int height, final Option<T> option) {
        super(parent, screen, option, x, y, width, height, option.getName());
        this.option = option;

        SliderAdapter<T> adapter = null;
        final Number value = option.getValue();
        if (value instanceof Integer) {
            adapter = (SliderAdapter<T>) new IntSliderAdapter(option.getMin().intValue(), option.getMax().intValue(), option.getValue().intValue());
        } else if (value instanceof Float) {
            adapter = (SliderAdapter<T>) new FloatSliderAdapter(option.getMin().floatValue(), option.getMax().floatValue(), option.getValue().floatValue());
        } else if (value instanceof Double) {
            adapter = (SliderAdapter<T>) new DoubleSliderAdapter(option.getMin().doubleValue(), option.getMax().doubleValue(), option.getValue().doubleValue());
        }

        this.slider = new SliderSubWidget<>(width - 100, y + 6, 109, height - 12, adapter, option.getValue(), option::setValue, false);
    }

    @Override
    public void draw(final GuiGraphicsExtractor context, final int mouseX, final int mouseY, final float delta) {
        this.slider.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void onMouseClick(final MouseButtonEvent click, final boolean doubled) {
        this.slider.onClick(click, doubled);
    }

    @Override
    public void onMouseRelease(final MouseButtonEvent click) {
        this.slider.release();
    }

    @Override
    public void onMouseDrag(final MouseButtonEvent click, final double deltaX, final double deltaY) {
        this.slider.onDrag((int) this.mouseX);
    }

    @Override
    public void onWidgetUpdate() {
        this.slider.setPos(new Point(this.width - 100, this.getY() + 6));
    }


    @Override
    public boolean isHovered() {
        return super.isHovered() && this.slider.isHovered;
    }

    @Override
    public <V> void onThirdPartyChange(final V value) {
        super.onThirdPartyChange(value);
        this.slider.setValue(this.option.getValue());
    }
}
