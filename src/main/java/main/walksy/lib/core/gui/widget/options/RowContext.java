package main.walksy.lib.core.gui.widget.options;

import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.core.gui.liquid.LiquidRect;
import main.walksy.lib.core.gui.modal.PopoverHost;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public record RowContext(Consumer<Option<?>> onFocusDescription, BiConsumer<LiquidRect, PopoverHost.PopoverContent> onOpenPopover, BiConsumer<LiquidRect, PopoverHost.PopoverContent> onOpenCenteredPopover, OpenAt onOpenPopoverAt, Runnable onClosePopover) {

    public interface OpenAt {
        void accept(LiquidRect anchor, float x, float y, PopoverHost.PopoverContent content);
    }
}
