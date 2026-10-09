package main.walksy.lib.core.gui.widget.options.types;

import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.api.config.option.Warning;
import main.walksy.lib.core.gui.modal.ConfirmPopover;
import main.walksy.lib.core.gui.modal.PopoverHost;
import main.walksy.lib.core.gui.widget.options.OptionRowWidget;
import main.walksy.lib.core.gui.widget.options.RowContext;
import main.walksy.lib.core.gui.widget.options.control.ToggleControl;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.texture.Icons;
import main.walksy.lib.core.toast.ToastLevel;
import net.minecraft.client.input.MouseButtonEvent;

public class BooleanOptionWidget extends OptionRowWidget<Boolean> {
    private static final long FADE_KEY_BASE = 0x600B00L;
    private static final String CONFIRM_LABEL = "Turn On";
    private final ToggleControl toggle;
    private boolean popoverActive;

    public BooleanOptionWidget(final Option<Boolean> option, final RowContext ctx, final int x, final int y, final int width, final int height) {
        super(option, ctx, x, y, width, height);
        this.toggle = new ToggleControl(() -> Boolean.TRUE.equals(this.option.getValue()), this.uiScale, FADE_KEY_BASE, this::requestToggle, null, option);
    }

    @Override
    protected void extractControl(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float dt, final float alpha, final boolean available) {
        if (this.popoverActive) {
            this.toggle.settle();
            return;
        }
        WLRenderSystem.cursors().requestControl(this.toggle.isOverControl(mouseX, mouseY) && !coveredByOverlay(mouseX, mouseY), available);
        this.toggle.setAvailability(this.availability());
        this.toggle.extract(graphics, dt, this.bandAlpha(), this.getX() + this.getWidth() - PADDING * this.uiScale, this.getY(), this.rowLabelHeight());
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        return !this.popoverActive && this.isInteractive() && this.toggle.mouseClicked(event);
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        if (!this.toggle.isPressed()) {
            return false;
        }
        if (!this.isInteractive()) {
            this.toggle.cancel();
            return false;
        }
        return this.toggle.mouseDragged(event);
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        return this.toggle.mouseReleased(event);
    }

    protected void requestToggle(final boolean next) {
        final Warning warning = this.option.getWarning();
        if (!next || warning == null) {
            this.option.setValue(next);
            return;
        }
        final ConfirmPopover popover = new ConfirmPopover(warning.message(), CONFIRM_LABEL, () -> this.confirm(warning), () -> this.popoverActive = false, this::closePopover, () -> this.toggle.originStyle(PopoverHost.PANEL_LAYER), PopoverHost.PANEL_LAYER)
            .title(warning.title())
            .icon(Icons.WARNING, ToastLevel.WARNING.accent())
            .onCancel(warning.onNo());
        this.popoverActive = this.openPopover(this.toggle.anchorRect(), popover);
    }

    private void confirm(final Warning warning) {
        this.option.setValue(true);
        if (warning.onYes() != null) {
            warning.onYes().run();
        }
    }
}
