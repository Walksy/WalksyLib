package walksy.lib.core.utils;

import net.minecraft.client.gui.DrawContext;

public class Renderer {

    public static void renderCircleArrow(DrawContext context, int x, int y, ArrowDirection direction, int color)
    {
    }

    public static void renderMiniArrow(DrawContext context, float x, float y, float scale, ArrowDirection direction, int color) {
        int[][] lines = {
                { -3, 0,  3, 1 },
                { -2, 1,  2, 2 },
                { -1, 2,  1, 3 }
        };

        for (int[] line : lines) {
            float x1 = line[0] * scale;
            float y1 = line[1] * scale;
            float x2 = line[2] * scale;
            float y2 = line[3] * scale;

            switch (direction) {
                case UP -> {
                    context.fill((int)(x + x1), (int)(y - y2), (int)(x + x2), (int)(y - y1), color);
                }
                case DOWN -> {
                    context.fill((int)(x + x1), (int)(y + y1), (int)(x + x2), (int)(y + y2), color);
                }
                case LEFT -> {
                    context.fill((int)(x - y2), (int)(y + x1), (int)(x - y1), (int)(y + x2), color);
                }
                case RIGHT -> {
                    context.fill((int)(x + y1), (int)(y + x1), (int)(x + y2), (int)(y + x2), color);
                }
            }
        }
    }


    public enum ArrowDirection {
        UP, DOWN, LEFT, RIGHT
    }

    private static void fillArrowLine(DrawContext context, int centerX, int topY, int width) {
        int half = width / 2;
        context.fill(centerX - half, topY, centerX + half, topY + 2, -1);
    }

    public static void fillRoundedRect(DrawContext ctx, int x, int y, int width, int height, int radius, int color) {
        int right = x + width;
        int bottom = y + height;

        ctx.fill(x + radius + 1, y, right - radius - 1, y + radius, color); // top
        ctx.fill(x + radius + 1, bottom - radius, right - radius - 1, bottom, color); // bottom

        ctx.fill(x, y + radius + 1, x + radius, bottom - radius - 1, color); // left
        ctx.fill(right - radius, y + radius + 1, right, bottom - radius - 1, color); // right

        ctx.fill(x + radius + 1, y + radius, right - radius - 1, bottom - radius, color);

        //Fills these stupid little gaps
        ctx.fill(x + radius, y + radius + 1, x + radius + 1, bottom - radius - 1, color);
        ctx.fill(right - radius - 1, y + radius + 1, right - radius, bottom - radius - 1, color);

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


    public static void fillPixelCircle(DrawContext ctx, int centerX, int centerY, int radius, int color) {
        int x = radius;
        int y = 0;
        int err = 0;

        while (x >= y) {
            ctx.drawHorizontalLine(centerX - x, centerX + x, centerY + y, color);
            ctx.drawHorizontalLine(centerX - x, centerX + x, centerY - y, color);
            ctx.drawHorizontalLine(centerX - y, centerX + y, centerY + x, color);
            ctx.drawHorizontalLine(centerX - y, centerX + y, centerY - x, color);

            y++;
            if (err <= 0) {
                err += 2 * y + 1;
            } else {
                x--;
                err += 2 * (y - x + 1);
            }
        }
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
