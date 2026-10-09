package main.walksy.lib.core.gui.widget.options.types;

import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.core.gui.modal.PopoverHost;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.utils.EnumNames;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import main.walksy.lib.core.gui.widget.options.control.PillSelectorWidget;
import main.walksy.lib.core.gui.widget.options.OptionRowWidget;
import main.walksy.lib.core.gui.widget.options.RowContext;

@SuppressWarnings({"rawtypes", "unchecked"})
public class EnumOptionWidget extends OptionRowWidget<Enum> {

    private static final AtomicLong FADE_KEYS = new AtomicLong(0xE4E4E4L);

    private final PillSelectorWidget<Enum> pill;

    public EnumOptionWidget(final Option<Enum> option, final RowContext ctx, final int x, final int y, final int width, final int height) {
        super(option, ctx, x, y, width, height);
        final GlassStyle pillStyle = PillSelectorWidget.defaultStyle().fadeKey(FADE_KEYS.incrementAndGet());
        final Enum[] declared = (Enum[]) option.getValue().getDeclaringClass().getEnumConstants();
        this.pill = new PillSelectorWidget<>(option::getValue, option::setValue, Arrays.asList(declared), EnumNames::pretty, pillStyle, this.uiScale, PopoverHost.PANEL_LAYER, this.ctx == null ? null : this.ctx.onOpenPopoverAt(), this.ctx == null ? null : this.ctx.onClosePopover());
    }

    @Override
    protected void extractControl(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float dt, final float alpha, final boolean available) {
        this.pill.extract(graphics, alpha, this.getX() + this.getWidth() - PADDING * this.uiScale, this.getY(), this.rowLabelHeight(), mouseX, mouseY, dt * 20f);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (!this.isInteractive() || this.pill.isOpen() || this.coveredByOverlay(event.x(), event.y()) || !this.pill.isOverPill(event.x(), event.y()) || this.ctx == null || this.ctx.onOpenPopoverAt() == null) {
            return false;
        }
        this.pill.open();
        return true;
    }
}
