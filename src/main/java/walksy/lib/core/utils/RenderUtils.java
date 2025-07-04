package walksy.lib.core.utils;

import net.minecraft.client.gui.DrawContext;

public class RenderUtils {

    public static void fillRoundedRect(DrawContext ctx, int x, int y, int width, int height, int radius, int color) {
        int right = x + width;
        int bottom = y + height;

        ctx.fill(x + radius, y, right - radius, bottom, color);
        ctx.fill(x, y + radius, right, bottom - radius, color);

        fillCircleQuarter(ctx, x + radius, y + radius, radius, color, Corner.TOP_LEFT);
        fillCircleQuarter(ctx, right - radius - 1, y + radius, radius, color, Corner.TOP_RIGHT);
        fillCircleQuarter(ctx, x + radius, bottom - radius - 1, radius, color, Corner.BOTTOM_LEFT);
        fillCircleQuarter(ctx, right - radius - 1, bottom - radius - 1, radius, color, Corner.BOTTOM_RIGHT);
    }

    public static void fillRoundedRectOutline(DrawContext ctx, int x, int y, int width, int height, int radius, int thickness, int color) {
        int right = x + width;
        int bottom = y + height;

        ctx.fill(x + radius + 1, y, right - radius - 1, y + thickness, color);
        ctx.fill(x + radius + 1, bottom - thickness, right - radius - 1, bottom, color);

        ctx.fill(x, y + radius + 1, x + thickness, bottom - radius - 1, color);
        ctx.fill(right - thickness, y + radius + 1, right, bottom - radius - 1, color);

        drawCircleQuarterOutline(ctx, x + radius, y + radius, radius, thickness, color, Corner.TOP_LEFT);
        drawCircleQuarterOutline(ctx, right - radius - 1, y + radius, radius, thickness, color, Corner.TOP_RIGHT);
        drawCircleQuarterOutline(ctx, x + radius, bottom - radius - 1, radius, thickness, color, Corner.BOTTOM_LEFT);
        drawCircleQuarterOutline(ctx, right - radius - 1, bottom - radius - 1, radius, thickness, color, Corner.BOTTOM_RIGHT);
    }

    private enum Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT; }

    private static void fillCircleQuarter(DrawContext ctx, int centerX, int centerY, int radius, int color, Corner corner) {
        for (int y = 0; y <= radius; y++) {
            for (int x = 0; x <= radius; x++) {
                if (x * x + y * y <= radius * radius) {
                    int drawX = centerX;
                    int drawY = centerY;
                    switch (corner) {
                        case TOP_LEFT -> ctx.fill(drawX - x, drawY - y, drawX - x + 1, drawY - y + 1, color);
                        case TOP_RIGHT -> ctx.fill(drawX + x, drawY - y, drawX + x + 1, drawY - y + 1, color);
                        case BOTTOM_LEFT -> ctx.fill(drawX - x, drawY + y, drawX - x + 1, drawY + y + 1, color);
                        case BOTTOM_RIGHT -> ctx.fill(drawX + x, drawY + y, drawX + x + 1, drawY + y + 1, color);
                    }
                }
            }
        }
    }

    private static void drawCircleQuarterOutline(DrawContext ctx, int centerX, int centerY, int radius, int thickness, int color, Corner corner) {
        int outerRadiusSq = radius * radius;
        int innerRadiusSq = (radius - thickness) * (radius - thickness);

        for (int y = 0; y <= radius; y++) {
            for (int x = 0; x <= radius; x++) {
                int distSq = x * x + y * y;
                if (distSq <= outerRadiusSq && distSq >= innerRadiusSq) {
                    int drawX = centerX;
                    int drawY = centerY;
                    switch (corner) {
                        case TOP_LEFT -> ctx.fill(drawX - x, drawY - y, drawX - x + 1, drawY - y + 1, color);
                        case TOP_RIGHT -> ctx.fill(drawX + x, drawY - y, drawX + x + 1, drawY - y + 1, color);
                        case BOTTOM_LEFT -> ctx.fill(drawX - x, drawY + y, drawX - x + 1, drawY + y + 1, color);
                        case BOTTOM_RIGHT -> ctx.fill(drawX + x, drawY + y, drawX + x + 1, drawY + y + 1, color);
                    }
                }
            }
        }
    }
}
