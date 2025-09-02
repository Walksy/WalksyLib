package main.walksy.lib.core.utils;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.util.function.Function;

public class Renderer {

    public static void drawRoundedTexture(
            DrawContext ctx,
            Function<Identifier, RenderLayer> renderLayers,
            Identifier sprite,
            int x,
            int y,
            int width,
            int height,
            int radius,
            int textureWidth,
            int textureHeight
    ) {
        for (int dy = 0; dy < height; dy++) {
            for (int dx = 0; dx < width; dx++) {
                boolean skip = false;

                //corner masking
                if (dx < radius && dy < radius && Math.pow(radius - dx, 2) + Math.pow(radius - dy, 2) > radius * radius) skip = true;
                if (dx >= width - radius && dy < radius && Math.pow(dx - (width - radius - 1), 2) + Math.pow(radius - dy, 2) > radius * radius) skip = true;
                if (dx < radius && dy >= height - radius && Math.pow(radius - dx, 2) + Math.pow(dy - (height - radius - 1), 2) > radius * radius) skip = true;
                if (dx >= width - radius && dy >= height - radius && Math.pow(dx - (width - radius - 1), 2) + Math.pow(dy - (height - radius - 1), 2) > radius * radius) skip = true;

                if (!skip) {
                    int u = dx % textureWidth;
                    int v = dy % textureHeight;

                    ctx.drawTexture(renderLayers, sprite, x + dx, y + dy, u, v, 1, 1, textureWidth, textureHeight);
                }
            }
        }
    }








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

    public static void fillRoundedRectGradient(DrawContext ctx, int x, int y, int width, int height, int radius, int colorTop, int colorBottom) {
        int right = x + width;
        int bottom = y + height;

        //interpolate ARGB colors linearly by t in [0,1]
        Function<Float, Integer> lerpColor = t -> {
            int aTop = (colorTop >> 24) & 0xFF;
            int rTop = (colorTop >> 16) & 0xFF;
            int gTop = (colorTop >> 8) & 0xFF;
            int bTop = (colorTop) & 0xFF;

            int aBot = (colorBottom >> 24) & 0xFF;
            int rBot = (colorBottom >> 16) & 0xFF;
            int gBot = (colorBottom >> 8) & 0xFF;
            int bBot = (colorBottom) & 0xFF;

            int a = (int) (aTop + t * (aBot - aTop));
            int r = (int) (rTop + t * (rBot - rTop));
            int g = (int) (gTop + t * (gBot - gTop));
            int b = (int) (bTop + t * (bBot - bTop));

            return (a << 24) | (r << 16) | (g << 8) | b;
        };

        // Draw from y to bottom line by line, filling the horizontal span with gradient color
        for (int currentY = y; currentY < bottom; currentY++) {
            float t = (float)(currentY - y) / (height - 1);
            int color = lerpColor.apply(t);

            // Calculate horizontal start and end based on rounded corners

            int leftX = x;
            int rightX = right - 1;

            // top-left corner
            if (currentY < y + radius) {
                int dy = y + radius - currentY;
                int offsetX = (int) Math.sqrt(radius * radius - dy * dy);
                leftX = x + radius - offsetX;
            }
            // bottom-left corner
            else if (currentY >= bottom - radius) {
                int dy = currentY - (bottom - radius - 1);
                int offsetX = (int) Math.sqrt(radius * radius - dy * dy);
                leftX = x + radius - offsetX;
            }

            // top-right corner
            if (currentY < y + radius) {
                int dy = y + radius - currentY;
                int offsetX = (int) Math.sqrt(radius * radius - dy * dy);
                rightX = right - radius + offsetX - 1;
            }
            // bottom-right corner
            else if (currentY >= bottom - radius) {
                int dy = currentY - (bottom - radius - 1);
                int offsetX = (int) Math.sqrt(radius * radius - dy * dy);
                rightX = right - radius + offsetX - 1;
            }

            if (rightX >= leftX) {
                ctx.fill(leftX, currentY, rightX + 1, currentY + 1, color);
            }
        }
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

    public static void drawHueSaturationValueBox(DrawContext ctx, int x, int y, int width, int height, int radius, float hue) {
        int right = x + width;
        int bottom = y + height;

        //Draw centre
        for (int iy = 0; iy < height; iy++) {
            for (int ix = 0; ix < width; ix++) {
                boolean inCorner = false;
                if (ix < radius && iy < radius) {
                    inCorner = (ix - radius) * (ix - radius) + (iy - radius) * (iy - radius) > radius * radius;
                } else if (ix >= width - radius && iy < radius) {
                    inCorner = (ix - (width - radius - 1)) * (ix - (width - radius - 1)) +
                            (iy - radius) * (iy - radius) > radius * radius;
                } else if (ix < radius && iy >= height - radius) {
                    inCorner = (ix - radius) * (ix - radius) +
                            (iy - (height - radius - 1)) * (iy - (height - radius - 1)) > radius * radius;
                } else if (ix >= width - radius && iy >= height - radius) {
                    inCorner = (ix - (width - radius - 1)) * (ix - (width - radius - 1)) +
                            (iy - (height - radius - 1)) * (iy - (height - radius - 1)) > radius * radius;
                }
                if (inCorner) continue;

                float saturation = ix / (float)(width - 1);
                float value = 1.0f - (iy / (float)(height - 1));

                int color = Color.HSBtoRGB(hue, saturation, value);
                ctx.fill(x + ix, y + iy, x + ix + 1, y + iy + 1, color);
            }
        }
        //Draw edges
        int baseColor = Color.HSBtoRGB(hue, 1.0f, 1.0f);
        fillCircleQuarter(ctx, x + radius, y + radius, radius, Color.WHITE.getRGB(), Corner.TOP_LEFT);
        fillCircleQuarter(ctx, right - radius - 1, y + radius, radius, baseColor, Corner.TOP_RIGHT);
        fillCircleQuarter(ctx, x + radius, bottom - radius - 1, radius, Color.BLACK.getRGB(), Corner.BOTTOM_LEFT);
        fillCircleQuarter(ctx, right - radius - 1, bottom - radius - 1, radius, Color.BLACK.getRGB(), Corner.BOTTOM_RIGHT);
    }

    public static void drawRoundedHueSlider(DrawContext ctx, int x, int y, int width, int height, int radius) {
        int right = x + width;
        int bottom = y + height;

        for (int dy = radius; dy < height - radius; dy++) {
            float hue = 1.0f - (dy / (float) (height - 1));
            int color = Color.HSBtoRGB(hue, 1.0f, 1.0f);
            ctx.fill(x, y + dy, right, y + dy + 1, color);
        }

        for (int dy = 0; dy < radius; dy++) {
            float hueTop = 1.0f - (dy / (float) (height - 1));
            float hueBottom = 1.0f - ((height - 1 - dy) / (float) (height - 1));
            int colorTop = Color.HSBtoRGB(hueTop, 1.0f, 1.0f);
            int colorBottom = Color.HSBtoRGB(hueBottom, 1.0f, 1.0f);

            ctx.fill(x + radius, y + dy, right - radius, y + dy + 1, colorTop);

            ctx.fill(x + radius, bottom - radius + dy, right - radius, bottom - radius + dy + 1, colorBottom);
        }

        int red = Color.HSBtoRGB(0.0f, 1.0f, 1.0f);
        fillCircleQuarter(ctx, x + radius, y + radius, radius, red, Corner.TOP_LEFT);
        fillCircleQuarter(ctx, right - radius - 1, y + radius, radius, red, Corner.TOP_RIGHT);
        fillCircleQuarter(ctx, x + radius, bottom - radius - 1, radius, red, Corner.BOTTOM_LEFT);
        fillCircleQuarter(ctx, right - radius - 1, bottom - radius - 1, radius, red, Corner.BOTTOM_RIGHT);
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
