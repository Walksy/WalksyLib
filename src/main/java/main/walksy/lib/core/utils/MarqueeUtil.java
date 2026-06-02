package main.walksy.lib.core.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

public class MarqueeUtil {

    public static int tickCount = 0;

    public static String get(final String full, final int maxWidth, final int interval) {
        final Font textRenderer = Minecraft.getInstance().font;
        if (textRenderer.width(full) <= maxWidth) return full;

        final String ellipsis = "...";
        final int ellipsisWidth = textRenderer.width(ellipsis);
        final int visibleWidth = maxWidth - ellipsisWidth;

        final int[] widths = new int[full.length() + 1];
        for (int i = 0; i < full.length(); i++) {
            widths[i + 1] = widths[i] + textRenderer.width(full.substring(i, i + 1));
        }

        int mChars = 0;
        for (int i = 1; i <= full.length(); i++) {
            if (widths[i] <= visibleWidth) mChars = i;
            else break;
        }

        final int steps = full.length() - mChars + 1;
        if (steps <= 1) return full;

        final int cycle = steps * 2 - 2;
        int pos = (tickCount / interval) % cycle;
        if (pos >= steps) pos = cycle - pos;

        final String visiblePart = full.substring(pos, pos + mChars);
        return (pos == steps - 1) ? visiblePart : visiblePart + ellipsis;
    }
}
