package main.walksy.lib.core.toast;

import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.render.texture.Icons;
import org.jetbrains.annotations.Nullable;

public enum ToastLevel {

    PLAIN(-1, null),

    INFO(-1, Icons.INFO),

    SUCCESS(3, Icons.CHECK),

    WARNING(1, Icons.WARNING),

    ERROR(0, Icons.ERROR);

    private final int paletteIndex;
    private final Icons.@Nullable Icon icon;

    ToastLevel(final int paletteIndex, final Icons.@Nullable Icon icon) {
        this.paletteIndex = paletteIndex;
        this.icon = icon;
    }

    public int accent() {
        final int[] palette = Theme.get().systemPalette();
        if (this.paletteIndex < 0 || this.paletteIndex >= palette.length) {
            return Theme.get().accent();
        }
        return palette[this.paletteIndex];
    }

    public @Nullable ToastIcon icon() {
        return this.icon == null ? null : ToastIcon.glyph(this.icon);
    }
}
