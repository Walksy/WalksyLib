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

    public NumericalWidget(OptionGroup parent, WalksyLibConfigScreen screen, int x, int y, int width, int height, Option<T> option) {
        super(parent, screen, option, x, y, width, height, option.getName());
        this.option = option;

        SliderAdapter<T> adapter = null;
        Number value = option.getValue();
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
    public void draw(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        this.slider.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void onMouseClick(MouseButtonEvent click, boolean doubled) {
        this.slider.onClick(click, doubled);
    }

    @Override
    public void onMouseRelease(MouseButtonEvent click) {
        this.slider.release();
    }

    @Override
    public void onMouseDrag(MouseButtonEvent click, double deltaX, double deltaY) {
        this.slider.onDrag((int) mouseX);
    }

    @Override
    public void onWidgetUpdate() {
        this.slider.setPos(new Point(width - 100, getY() + 6));
    }


    @Override
    public boolean isHovered() {
        return super.isHovered() && this.slider.isHovered;
    }

    @Override
    public <V> void onThirdPartyChange(V value) {
        super.onThirdPartyChange(value);
        this.slider.setValue(this.option.getValue());
    }
}
