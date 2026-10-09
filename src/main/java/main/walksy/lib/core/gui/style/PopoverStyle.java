package main.walksy.lib.core.gui.style;

import main.walksy.lib.core.utils.ColorUtils;

public interface PopoverStyle {
    PopoverStyle DEFAULT = new PopoverStyle() { };

    static PopoverStyle getDefault() {
        return Holder.current;
    }

    static void setDefault(final PopoverStyle style) {
        Holder.current = style == null ? DEFAULT : style;
    }

    default int title() {
        return Theme.get().label();
    }

    default int label() {
        return Theme.get().label();
    }

    default int secondaryLabel() {
        return Theme.get().secondaryLabel();
    }

    default int tertiaryLabel() {
        return Theme.get().tertiaryLabel();
    }

    default int accent() {
        return Theme.get().accent();
    }

    default int selectedLabel() {
        return this.accent();
    }

    default int destructive() {
        return Theme.get().destructive();
    }

    default int emptyMessage() {
        return this.tertiaryLabel();
    }

    default int selectedRowFill() {
        return ColorUtils.withAlpha(this.accent(), 0.22f);
    }

    default int rowHoverFill() {
        return ColorUtils.withAlpha(Theme.get().shadowColor(), 0.14f);
    }

    default int separator() {
        return Theme.get().separator();
    }

    default int scrollbar(final boolean dragging) {
        return ColorUtils.withAlpha(Theme.get().label(), dragging ? 0.55f : 0.3f);
    }

    final class Holder {
        private static PopoverStyle current = DEFAULT;

        private Holder() {
        }
    }
}
