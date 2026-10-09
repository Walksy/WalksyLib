package main.walksy.lib.core.gui.widget.options.types;
import com.mojang.blaze3d.platform.InputConstants;

import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.core.gui.widget.SearchBar;
import main.walksy.lib.core.render.WLGraphics2D;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.concurrent.atomic.AtomicLong;
import main.walksy.lib.core.gui.widget.options.control.TextFieldControl;
import main.walksy.lib.core.gui.widget.options.OptionRowWidget;
import main.walksy.lib.core.gui.widget.options.RowContext;

public class StringOptionWidget extends OptionRowWidget<String> {
    private static final AtomicLong FADE_KEYS = new AtomicLong(0x57A3B0L);
    private final TextFieldControl field;

    public StringOptionWidget(final Option<String> option, final RowContext ctx, final int x, final int y, final int width, final int height) {
        super(option, ctx, x, y, width, height);
        this.field = new TextFieldControl(option.getValue(), option::setValue, (String) null, SearchBar.defaultStyle().fadeKey(FADE_KEYS.incrementAndGet()), this.uiScale);
    }

    @Override
    protected void extractControl(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float dt, final float alpha, final boolean available) {
        this.field.syncText(this.option.getValue());
        final float h = this.rowLabelHeight() * TextFieldControl.ROW_HEIGHT_RATIO;
        final float y = this.getY() + (this.rowLabelHeight() - h) / 2f;
        final float x0 = this.controlAreaX();
        final float x1 = this.getX() + this.getWidth() - PADDING * this.uiScale;
        this.field.extract(graphics, x0, y, x1 - x0, h, mouseX, mouseY, dt, alpha, available, available && !this.coveredByOverlay(mouseX, mouseY));
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !this.isInteractive()) {
            return false;
        }
        if (!this.field.isOver(event.x(), event.y())) {
            this.field.setFocused(false);
            return false;
        }
        this.field.setFocused(true);
        this.field.clicked(event, doubleClick);
        return true;
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        return this.field.isFocused() && this.field.dragged(event, dragX, dragY);
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        return this.field.isFocused() && this.field.keyPressed(event);
    }

    @Override
    public boolean charTyped(final CharacterEvent event) {
        return this.field.isFocused() && this.field.charTyped(event);
    }

    @Override
    public void setFocused(final boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            this.field.setFocused(false);
        }
    }
}
