package main.walksy.lib.core.render.gpu;

import org.jetbrains.annotations.Nullable;

public final class VanillaScissor {
    private static final int[] BOX = new int[4];
    private static boolean enabled;

    private VanillaScissor() {
    }

    public static void enable(final int x, final int y, final int width, final int height) {
        BOX[0] = x;
        BOX[1] = y;
        BOX[2] = width;
        BOX[3] = height;
        enabled = true;
    }

    public static void disable() {
        enabled = false;
    }

    static int @Nullable [] box() {
        return enabled ? BOX : null;
    }
}
