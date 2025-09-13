package main.walksy.lib.core.renderer;

import main.walksy.lib.core.config.local.options.type.PixelGrid;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import java.awt.*;

public class Render2D {

    public void renderGridTexture(DrawContext context, PixelGrid grid, int x1, int y1, int pixelSize, int gapSize, float scale) {
        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                boolean val = grid.getPixel(x, y);

                int px = x1 + x * (pixelSize + gapSize);
                int py = y1 + y * (pixelSize + gapSize);

                if (val) {
                    context.fill(
                            px,
                            py,
                            px + pixelSize,
                            py + pixelSize,
                            Color.WHITE.getRGB()
                    );
                }
            }
        }
    }

    public void renderGridOutline(DrawContext context, PixelGrid grid, int x1, int y1, int pixelSize, int gapSize) {
        int gridWidthPixels = grid.getWidth() * pixelSize + (grid.getWidth() - 1) * gapSize;
        int gridHeightPixels = grid.getHeight() * pixelSize + (grid.getHeight() - 1) * gapSize;

        int x2 = x1 + gridWidthPixels;
        int y2 = y1 + gridHeightPixels;

        float borderWidth = 0.3f; // Thinner border
        int blue = new Color(0, 100, 255).getRGB();

        // Top
        this.fill(RenderLayer.getGui(), context.getMatrices(), x1 - borderWidth, y1 - borderWidth, x2 + borderWidth, y1, 0, blue);

        // Bottom
        this.fill(RenderLayer.getGui(), context.getMatrices(), x1 - borderWidth, y2, x2 + borderWidth, y2 + borderWidth, 0, blue);

        // Left
        this.fill(RenderLayer.getGui(), context.getMatrices(), x1 - borderWidth, y1, x1, y2, 0, blue);

        // Right
        this.fill(RenderLayer.getGui(), context.getMatrices(), x2, y1, x2 + borderWidth, y2, 0, blue);
    }



    public void renderGridOutline(DrawContext context, PixelGrid grid, int x1, int y1, int pixelSize, int gapSize, int outlineColor, boolean markCenter) {
        if (markCenter) {
            int centerX = grid.getWidth() / 2;
            int centerY = grid.getHeight() / 2;

            int px = x1 + centerX * (pixelSize + gapSize);
            int py = y1 + centerY * (pixelSize + gapSize);

            int centerColor = new Color(255, 100, 100, 100).getRGB();

            context.fill(
                    px + 1,
                    py + 1,
                    px + pixelSize - 1,
                    py + pixelSize - 1,
                    centerColor
            );
        }
        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                int px = x1 + x * (pixelSize + gapSize);
                int py = y1 + y * (pixelSize + gapSize);

                boolean val = grid.getPixel(x, y);
                if (val) {
                    context.fill(
                            px + 1,
                            py + 1,
                            px + pixelSize - 1,
                            py + pixelSize - 1,
                            Color.WHITE.getRGB()
                    );
                }
                context.fill(px + 1, py, px + pixelSize - 1, py + 1, outlineColor);
                context.fill(px + 1, py + pixelSize - 1, px + pixelSize - 1, py + pixelSize, outlineColor);
                context.fill(px, py, px + 1, py + pixelSize, outlineColor);
                context.fill(px + pixelSize - 1, py, px + pixelSize, py + pixelSize, outlineColor);
            }
        }
    }



    public void startPopUpRender(DrawContext context, int z, int width, int height) {
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, z); //push it above everything else
        context.fill(0, 0, width, height, new Color(0, 0, 0, 100).getRGB());
    }

    public void endPopUpRender(DrawContext context) {
        context.getMatrices().pop();
    }

    public void fill(RenderLayer layer, MatrixStack stack, float x1, float y1, float x2, float y2, float z, int color) {
        Matrix4f matrix4f = stack.peek().getPositionMatrix();
        if (x1 < x2) {
            float i = x1;
            x1 = x2;
            x2 = i;
        }

        if (y1 < y2) {
            float i = y1;
            y1 = y2;
            y2 = i;
        }

        VertexConsumer vertexConsumer = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers().getBuffer(layer);
        vertexConsumer.vertex(matrix4f, (float)x1, (float)y1, (float)z).color(color);
        vertexConsumer.vertex(matrix4f, (float)x1, (float)y2, (float)z).color(color);
        vertexConsumer.vertex(matrix4f, (float)x2, (float)y2, (float)z).color(color);
        vertexConsumer.vertex(matrix4f, (float)x2, (float)y1, (float)z).color(color);
    }
}
