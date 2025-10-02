package main.walksy.lib.core.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;

public class MarqueeUtil {

    public static int tickCount = 0;

    public static String get(String full, int maxWidth, int interval) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        if (textRenderer.getWidth(full) <= maxWidth) return full;

        String ellipsis = "...";
        int ellipsisWidth = textRenderer.getWidth(ellipsis);
        int visibleWidth = maxWidth - ellipsisWidth;

        int[] widths = new int[full.length() + 1];
        for (int i = 0; i < full.length(); i++) {
            widths[i + 1] = widths[i] + textRenderer.getWidth(full.substring(i, i + 1));
        }

        int mChars = 0;
        for (int i = 1; i <= full.length(); i++) {
            if (widths[i] <= visibleWidth) mChars = i;
            else break;
        }

        int steps = full.length() - mChars + 1;
        if (steps <= 1) return full;

        int cycle = steps * 2 - 2;
        int pos = (tickCount / interval) % cycle;
        if (pos >= steps) pos = cycle - pos;

        String visiblePart = full.substring(pos, pos + mChars);
        return (pos == steps - 1) ? visiblePart : visiblePart + ellipsis;
    }
}
