package main.walksy.lib.core.renderer;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import main.walksy.lib.core.config.local.options.type.PixelGrid;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class Renderer2D {

    public static void drawRoundedTexture(
            GuiGraphicsExtractor ctx,
            RenderPipeline pipeline,
            Identifier sprite,
            int x,
            int y,
            int width,
            int height,
            int radius,
            int textureWidth,
            int textureHeight
    ) {
        TextureManager manager = Minecraft.getInstance().getTextureManager();
        manager.getTexture(sprite);
        AbstractTexture texture = manager.getTexture(sprite);
        TextureSetup textureSetup = TextureSetup.singleTexture(texture.getTextureView(), texture.getSampler());
        ScreenRectangle current = ctx.scissorStack.peek();
        RoundedTextureGuiElementRenderState state = new RoundedTextureGuiElementRenderState(
                pipeline,
                textureSetup,
                ctx.pose(),
                sprite,
                x,
                y,
                x + width,
                y + height,
                radius,
                textureWidth,
                textureHeight,
                current
        );
        ctx.guiRenderState.addGuiElement(state);
    }

    public static void renderMiniArrow(GuiGraphicsExtractor context, float x, float y, float scale, ArrowDirection direction, int color) {
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

    public static void verticalLine(GuiGraphicsExtractor context, float x, float y1, float y2, int color) {
        if (y2 < y1) {
            float i = y1;
            y1 = y2;
            y2 = i;
        }

        fill(context, x, y1 + 1, (float) (x + 0.8), y2, color);
    }

    public static void fillRoundedRectGradient(GuiGraphicsExtractor ctx, int x, int y, int width, int height, int radius, int colorTop, int colorBottom) {
        ScreenRectangle current = ctx.scissorStack.peek();
        RoundedGradientRectGuiElementRenderState state = new RoundedGradientRectGuiElementRenderState(
                RenderPipelines.GUI,
                TextureSetup.noTexture(),
                ctx.pose(),
                x,
                y,
                x + width,
                y + height,
                radius,
                colorTop,
                colorBottom,
                current
        );

        ctx.guiRenderState.addGuiElement(state);
    }


    public static void fillRoundedRect(GuiGraphicsExtractor ctx, float x, float y, float width, float height, int radius, int color) {
        float right = x + width;
        float bottom = y + height;
        fill(ctx, x + radius + 1, y, right - radius - 1, y + radius, color); // top
        fill(ctx, x + radius + 1, bottom - radius, right - radius - 1, bottom, color); // bottom

        fill(ctx, x, y + radius + 1, x + radius, bottom - radius - 1, color); // left
        fill(ctx, right - radius, y + radius + 1, right, bottom - radius - 1, color); // right

        fill(ctx, x + radius + 1, y + radius, right - radius - 1, bottom - radius, color);

        //Fills these stupid little gaps
        fill(ctx, x + radius, y + radius + 1, x + radius + 1, bottom - radius - 1, color);
        fill(ctx, right - radius - 1, y + radius + 1, right - radius, bottom - radius - 1, color);

        fillCircleQuarter(ctx, x + radius, y + radius, radius, color, Corner.TOP_LEFT);
        fillCircleQuarter(ctx, right - radius - 1, y + radius, radius, color, Corner.TOP_RIGHT);
        fillCircleQuarter(ctx, x + radius, bottom - radius - 1, radius, color, Corner.BOTTOM_LEFT);
        fillCircleQuarter(ctx, right - radius - 1, bottom - radius - 1, radius, color, Corner.BOTTOM_RIGHT);
    }

    public static void fillRoundedRectOutline(GuiGraphicsExtractor ctx, int x, int y, int width, int height, int radius, int thickness, int color) {
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

    public static void fillRoundedRectOutline_ModWidget(GuiGraphicsExtractor ctx, int x, int y, int width, int height, int radius, int thickness, int color) {
        int right = x + width;
        int bottom = y + height;

        ctx.fill(x + 1, y, right - radius - 1, y + thickness, color);
        ctx.fill(x + 1, bottom - thickness, right - radius - 1, bottom, color);

        ctx.fill(x, y, x + thickness, bottom, color);
        ctx.fill(right - thickness, y + radius + 1, right, bottom - radius - 1, color);

        drawCircleQuarterOutline(ctx, right - radius - 1, y + radius, radius, thickness, color, Corner.TOP_RIGHT);
        drawCircleQuarterOutline(ctx, right - radius - 1, bottom - radius - 1, radius, thickness, color, Corner.BOTTOM_RIGHT);
    }


    public static void drawHueSaturationValueBox(GuiGraphicsExtractor context, int x, int y, int width, int height, int radius, float hue) {
        ScreenRectangle current = context.scissorStack.peek();
        context.guiRenderState.addGuiElement(
                new SaturationBoxGuiElementRenderState(
                        RenderPipelines.GUI,
                        TextureSetup.noTexture(),
                        new Matrix3x2f(context.pose()),
                        x, y, x + width, y + height,
                        radius,
                        hue,
                        current
                )
        );
    }

    public static void drawRoundedHueSlider(GuiGraphicsExtractor ctx, int x, int y, int width, int height, int radius) {
        ScreenRectangle current = ctx.scissorStack.peek();
        HueSliderGuiElementRenderState state = new HueSliderGuiElementRenderState(
                RenderPipelines.GUI,
                TextureSetup.noTexture(),
                new Matrix3x2f(ctx.pose()),
                x,
                y,
                x + width,
                y + height,
                radius,
                current
        );

        ctx.guiRenderState.addGuiElement(state);
    }


    public static void fillCircleQuarter(
            GuiGraphicsExtractor ctx,
            float centerX,
            float centerY,
            int radius,
            int color,
            Corner corner) {
        ScreenRectangle current = ctx.scissorStack.peek();
        CircleQuarterRenderState state = new CircleQuarterRenderState(
                RenderPipelines.GUI,
                TextureSetup.noTexture(),
                ctx.pose(),
                centerX,
                centerY,
                radius,
                color,
                corner,
                current
        );

        ctx.guiRenderState.addGuiElement(state);
    }


    private static void drawCircleQuarterOutline(
            GuiGraphicsExtractor ctx,
            int centerX,
            int centerY,
            int radius,
            int thickness,
            int color,
            Corner corner
    ) {
        ScreenRectangle current = ctx.scissorStack.peek();
        CircleQuarterOutlineRenderState state = new CircleQuarterOutlineRenderState(
                RenderPipelines.GUI,
                TextureSetup.noTexture(),
                ctx.pose(),
                centerX,
                centerY,
                radius,
                thickness,
                color,
                corner,
                current
        );

        ctx.guiRenderState.addGuiElement(state);
    }


    public static void renderGridTexture(GuiGraphicsExtractor context, PixelGrid grid, float x1, float y1, int pixelSize, int gapSize, boolean blend) {
        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                boolean val = grid.getPixel(x, y);

                float px = x1 + x * (pixelSize + gapSize);
                float py = y1 + y * (pixelSize + gapSize);
                float px2 = px + pixelSize;
                float py2 = py + pixelSize;

                if (val) {
                    drawFilledRectangle(context, px, py, px2, py2, Color.WHITE, blend);
                }
            }
        }
    }

    /**
     * private void drawTexturedQuad(Function<Identifier, RenderLayer> renderLayers, Identifier sprite, int x1, int x2, int y1, int y2, float u1, float u2, float v1, float v2, int color) {
     *         RenderLayer renderLayer = (RenderLayer)renderLayers.apply(sprite);
     *         Matrix4f matrix4f = this.matrices.peek().getPositionMatrix();
     *         VertexConsumer vertexConsumer = this.vertexConsumers.getBuffer(renderLayer);
     *         vertexConsumer.vertex(matrix4f, (float)x1, (float)y1, 0.0F).texture(u1, v1).setColor(color);
     *         vertexConsumer.vertex(matrix4f, (float)x1, (float)y2, 0.0F).texture(u1, v2).setColor(color);
     *         vertexConsumer.vertex(matrix4f, (float)x2, (float)y2, 0.0F).texture(u2, v2).setColor(color);
     *         vertexConsumer.vertex(matrix4f, (float)x2, (float)y1, 0.0F).texture(u2, v1).setColor(color);
     *     }
     */

    public static void drawFilledRectangle(GuiGraphicsExtractor context, float x1, float y1, float x2, float y2, Color color, boolean blend) {
        ScreenRectangle current = context.scissorStack.peek();
        context.pose().pushMatrix();
        setGlProperty(2848, false);
        context.guiRenderState.addGuiElement(new ColoredFloatQuadGuiElementRenderState(blend ? RenderPipelines.GUI_INVERT : RenderPipelines.GUI, TextureSetup.noTexture(), new Matrix3x2f(context.pose()), x1, y1, x2, y2, color.getRGB(), color.getRGB(), current));
        context.pose().popMatrix();
    }

    private static void setGlProperty(int property, boolean isEnabled) {
        if (isEnabled) {
            GL11.glEnable((int)property);
        } else {
            GL11.glDisable((int)property);
        }
    }

    public static void renderGridOutline(GuiGraphicsExtractor context, PixelGrid grid, int x1, int y1, int pixelSize, int gapSize) {
        int gridWidthPixels = grid.getWidth() * pixelSize + (grid.getWidth() - 1) * gapSize;
        int gridHeightPixels = grid.getHeight() * pixelSize + (grid.getHeight() - 1) * gapSize;

        int x2 = x1 + gridWidthPixels;
        int y2 = y1 + gridHeightPixels;

        float borderWidth = 0.3f;
        int blue = new Color(0, 100, 255).getRGB();

        //Top
        fill(context, x1 - borderWidth, y1 - borderWidth, x2 + borderWidth, y1, blue);

        //Bottom
        fill(context, x1 - borderWidth, y2, x2 + borderWidth, y2 + borderWidth, blue);

        //Left
        fill(context, x1 - borderWidth, y1, x1, y2, blue);

        //Right
        fill(context, x2, y1, x2 + borderWidth, y2, blue);
    }


    public static void renderGridOutline(GuiGraphicsExtractor context, PixelGrid grid, int x1, int y1, int pixelSize, int gapSize, int outlineColor, boolean markCenter) {
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

    public static void fill(GuiGraphicsExtractor ctx, float x1, float y1, float x2, float y2, int color) {
        ScreenRectangle current = ctx.scissorStack.peek();
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
        ctx.guiRenderState.addGuiElement(new ColoredFloatQuadGuiElementRenderState(RenderPipelines.GUI, TextureSetup.noTexture(), new Matrix3x2f(ctx.pose()), x1, y1, x2, y2, color, color, current));
    }


    @Environment(EnvType.CLIENT)
    public record ColoredFloatQuadGuiElementRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose, float x0, float y0, float x1, float y1, int col1, int col2, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements GuiElementRenderState {
        public ColoredFloatQuadGuiElementRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose, float x0, float y0, float x1, float y1, int col1, int col2, @Nullable ScreenRectangle scissorArea) {
            this(pipeline, textureSetup, pose, x0, y0, x1, y1, col1, col2, scissorArea, createBounds(x0, y0, x1, y1, pose, scissorArea));
        }

        @Override
        public void buildVertices(@NotNull VertexConsumer vertices) {
            vertices.addVertexWith2DPose(this.pose(), (float)this.x0(), (float)this.y0()).setColor(this.col1());
            vertices.addVertexWith2DPose(this.pose(), (float)this.x0(), (float)this.y1()).setColor(this.col2());
            vertices.addVertexWith2DPose(this.pose(), (float)this.x1(), (float)this.y1()).setColor(this.col2());
            vertices.addVertexWith2DPose(this.pose(), (float)this.x1(), (float)this.y0()).setColor(this.col1());
        }

        @Nullable
        private static ScreenRectangle createBounds(float x0, float y0, float x1, float y1, Matrix3x2f pose, @Nullable ScreenRectangle scissorArea) {
            ScreenRectangle ScreenRectangle = (new ScreenRectangle((int) x0, (int) y0, (int) (x1 - x0), (int) (y1 - y0))).transformMaxBounds(pose);
            return scissorArea != null ? scissorArea.intersection(ScreenRectangle) : ScreenRectangle;
        }
    }

    @Environment(EnvType.CLIENT)
    public record SaturationBoxGuiElementRenderState(
            RenderPipeline pipeline,
            TextureSetup textureSetup,
            Matrix3x2f pose,
            int x0, int y0, int x1, int y1,
            int radius,
            float hue,
            @Nullable ScreenRectangle scissorArea,
            @Nullable ScreenRectangle bounds
    ) implements GuiElementRenderState {

        public SaturationBoxGuiElementRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose,
                                                  int x0, int y0, int x1, int y1, int radius, float hue,
                                                  @Nullable ScreenRectangle scissorArea) {
            this(pipeline, textureSetup, pose, x0, y0, x1, y1, radius, hue,
                    scissorArea, createBounds(x0, y0, x1, y1, pose, scissorArea));
        }

        @Nullable
        private static ScreenRectangle createBounds(int x0, int y0, int x1, int y1,
                                               Matrix3x2f pose, @Nullable ScreenRectangle scissorArea) {
            ScreenRectangle ScreenRectangle = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
            return scissorArea != null ? scissorArea.intersection(ScreenRectangle) : ScreenRectangle;
        }

        @Override
        public void buildVertices(@NotNull VertexConsumer vertices) {
            int width = x1 - x0;
            int height = y1 - y0;
            if (width <= 0 || height <= 0) return;

            int steps = Math.max(2, height);
            for (int i = 0; i < steps; i++) {
                float t0 = i / (float) steps;
                float t1 = (i + 1) / (float) steps;

                float value0 = 1.0f - t0;
                float value1 = 1.0f - t1;

                int leftColor0 = Color.HSBtoRGB(hue, 0f, value0) | 0xFF000000;
                int rightColor0 = Color.HSBtoRGB(hue, 1f, value0) | 0xFF000000;
                int leftColor1 = Color.HSBtoRGB(hue, 0f, value1) | 0xFF000000;
                int rightColor1 = Color.HSBtoRGB(hue, 1f, value1) | 0xFF000000;

                float yStart = y0 + t0 * height;
                float yEnd   = y0 + t1 * height;

                int dy0 = (int) (yStart - y0);
                int dy1 = (int) (yEnd - y0);

                int leftX0 = x0;
                int rightX0 = x1 - 1;
                int leftX1 = x0;
                int rightX1 = x1 - 1;

                if (radius > 0) {
                    if (dy0 < radius) {
                        int dd = radius - dy0;
                        int offsetX = (int) Math.sqrt((double) radius * radius - (double) dd * dd);
                        leftX0 = x0 + radius - offsetX;
                        rightX0 = x1 - radius + offsetX - 1;
                    } else if (dy0 >= height - radius) { // bottom curve
                        int dd = dy0 - (height - radius - 1);
                        int offsetX = (int) Math.sqrt((double) radius * radius - (double) dd * dd);
                        leftX0 = x0 + radius - offsetX;
                        rightX0 = x1 - radius + offsetX - 1;
                    }

                    if (dy1 < radius) {
                        int dd = radius - dy1;
                        int offsetX = (int) Math.sqrt((double) radius * radius - (double) dd * dd);
                        leftX1 = x0 + radius - offsetX;
                        rightX1 = x1 - radius + offsetX - 1;
                    } else if (dy1 >= height - radius) {
                        int dd = dy1 - (height - radius - 1);
                        int offsetX = (int) Math.sqrt((double) radius * radius - (double) dd * dd);
                        leftX1 = x0 + radius - offsetX;
                        rightX1 = x1 - radius + offsetX - 1;
                    }
                }

                int leftX = Math.max(leftX0, leftX1);
                int rightX = Math.min(rightX0, rightX1);
                if (rightX <= leftX) continue;

                float px0 = leftX;
                float px1 = rightX + 1;

                vertices.addVertexWith2DPose(pose, px0, yStart).setColor(leftColor0);
                vertices.addVertexWith2DPose(pose, px0, yEnd).setColor(leftColor1);
                vertices.addVertexWith2DPose(pose, px1, yEnd).setColor(rightColor1);
                vertices.addVertexWith2DPose(pose, px1, yStart).setColor(rightColor0);
            }
        }



    }

    @Environment(EnvType.CLIENT)
    public record HueSliderGuiElementRenderState(
            RenderPipeline pipeline,
            TextureSetup textureSetup,
            Matrix3x2f pose,
            int x0, int y0, int x1, int y1,
            int radius,
            @Nullable ScreenRectangle scissorArea,
            @Nullable ScreenRectangle bounds
    ) implements GuiElementRenderState {

        public HueSliderGuiElementRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose,
                                              int x0, int y0, int x1, int y1, int radius,
                                              @Nullable ScreenRectangle scissorArea) {
            this(pipeline, textureSetup, pose, x0, y0, x1, y1, radius,
                    scissorArea, createBounds(x0, y0, x1, y1, pose, scissorArea));
        }

        @Nullable
        private static ScreenRectangle createBounds(int x0, int y0, int x1, int y1,
                                               Matrix3x2f pose, @Nullable ScreenRectangle scissorArea) {
            ScreenRectangle ScreenRectangle = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
            return scissorArea != null ? scissorArea.intersection(ScreenRectangle) : ScreenRectangle;
        }

        @Override
        public void buildVertices(@NotNull VertexConsumer vertices) {
            int width = x1 - x0;
            int height = y1 - y0;
            if (width <= 0 || height <= 0) return;

            int steps = 64; //smoothness, 64 is more than enough
            for (int i = 0; i < steps; i++) {
                float t0 = i / (float) steps;
                float t1 = (i + 1) / (float) steps;

                float hue0 = 1.0f - t0;
                float hue1 = 1.0f - t1;

                int color0 = Color.HSBtoRGB(hue0, 1f, 1f) | 0xFF000000;
                int color1 = Color.HSBtoRGB(hue1, 1f, 1f) | 0xFF000000;

                float yStart = y0 + t0 * height;
                float yEnd   = y0 + t1 * height;

                int dy0 = (int) (yStart - y0);
                int dy1 = (int) (yEnd - y0);

                int leftX0 = x0;
                int rightX0 = x1 - 1;
                int leftX1 = x0;
                int rightX1 = x1 - 1;

                if (dy0 < radius) {
                    int dd = radius - dy0;
                    int offsetX = (int) Math.sqrt((double) radius * radius - (double) dd * dd);
                    leftX0 = x0 + radius - offsetX;
                    rightX0 = x1 - radius + offsetX - 1;
                } else if (dy0 >= height - radius) {
                    int dd = dy0 - (height - radius - 1);
                    int offsetX = (int) Math.sqrt((double) radius * radius - (double) dd * dd);
                    leftX0 = x0 + radius - offsetX;
                    rightX0 = x1 - radius + offsetX - 1;
                }

                if (dy1 < radius) {
                    int dd = radius - dy1;
                    int offsetX = (int) Math.sqrt((double) radius * radius - (double) dd * dd);
                    leftX1 = x0 + radius - offsetX;
                    rightX1 = x1 - radius + offsetX - 1;
                } else if (dy1 >= height - radius) {
                    int dd = dy1 - (height - radius - 1);
                    int offsetX = (int) Math.sqrt((double) radius * radius - (double) dd * dd);
                    leftX1 = x0 + radius - offsetX;
                    rightX1 = x1 - radius + offsetX - 1;
                }

                int leftX = Math.max(leftX0, leftX1);
                int rightX = Math.min(rightX0, rightX1);
                if (rightX <= leftX) continue;

                float px0 = leftX;
                float px1 = rightX + 1;

                vertices.addVertexWith2DPose(pose, px0, yStart).setColor(color0);
                vertices.addVertexWith2DPose(pose, px0, yEnd).setColor(color1);
                vertices.addVertexWith2DPose(pose, px1, yEnd).setColor(color1);
                vertices.addVertexWith2DPose(pose, px1, yStart).setColor(color0);
            }
        }
    }


    @Environment(EnvType.CLIENT)
    public record RoundedGradientRectGuiElementRenderState(
            RenderPipeline pipeline,
            TextureSetup textureSetup,
            Matrix3x2f pose,
            int x0, int y0, int x1, int y1,
            int radius,
            int colorTop,
            int colorBottom,
            @Nullable ScreenRectangle scissorArea,
            @Nullable ScreenRectangle bounds
    ) implements GuiElementRenderState {

        public RoundedGradientRectGuiElementRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose,
                                                        int x0, int y0, int x1, int y1,
                                                        int radius, int colorTop, int colorBottom,
                                                        @Nullable ScreenRectangle scissorArea) {
            this(pipeline, textureSetup, pose, x0, y0, x1, y1, radius, colorTop, colorBottom,
                    scissorArea, createBounds(x0, y0, x1, y1, pose, scissorArea));
        }

        @Nullable
        private static ScreenRectangle createBounds(int x0, int y0, int x1, int y1,
                                               Matrix3x2f pose, @Nullable ScreenRectangle scissorArea) {
            ScreenRectangle ScreenRectangle = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
            return scissorArea != null ? scissorArea.intersection(ScreenRectangle) : ScreenRectangle;
        }

        @Override
        public void buildVertices(@NotNull VertexConsumer vertices) {
            int width = x1 - x0;
            int height = y1 - y0;
            if (width <= 0 || height <= 0) return;

            int steps = Math.max(2, height);
            for (int i = 0; i < steps; i++) {
                float t0 = i / (float) steps;
                float t1 = (i + 1) / (float) steps;

                int color0 = lerpColor(colorTop, colorBottom, t0);
                int color1 = lerpColor(colorTop, colorBottom, t1);

                float yStart = y0 + t0 * height;
                float yEnd   = y0 + t1 * height;

                int dy0 = (int) (yStart - y0);
                int dy1 = (int) (yEnd - y0);

                int leftX0 = x0;
                int rightX0 = x1 - 1;
                int leftX1 = x0;
                int rightX1 = x1 - 1;

                if (dy0 < radius) {
                    int dd = radius - dy0;
                    int offsetX = (int) Math.sqrt((double) radius * radius - (double) dd * dd);
                    leftX0 = x0 + radius - offsetX;
                    rightX0 = x1 - radius + offsetX - 1;
                } else if (dy0 >= height - radius) {
                    int dd = dy0 - (height - radius - 1);
                    int offsetX = (int) Math.sqrt((double) radius * radius - (double) dd * dd);
                    leftX0 = x0 + radius - offsetX;
                    rightX0 = x1 - radius + offsetX - 1;
                }

                if (dy1 < radius) {
                    int dd = radius - dy1;
                    int offsetX = (int) Math.sqrt((double) radius * radius - (double) dd * dd);
                    leftX1 = x0 + radius - offsetX;
                    rightX1 = x1 - radius + offsetX - 1;
                } else if (dy1 >= height - radius) {
                    int dd = dy1 - (height - radius - 1);
                    int offsetX = (int) Math.sqrt((double) radius * radius - (double) dd * dd);
                    leftX1 = x0 + radius - offsetX;
                    rightX1 = x1 - radius + offsetX - 1;
                }

                int leftX = Math.max(leftX0, leftX1);
                int rightX = Math.min(rightX0, rightX1);
                if (rightX <= leftX) continue;

                float px0 = leftX;
                float px1 = rightX + 1;

                vertices.addVertexWith2DPose(pose, px0, yStart).setColor(color0);
                vertices.addVertexWith2DPose(pose, px0, yEnd).setColor(color1);
                vertices.addVertexWith2DPose(pose, px1, yEnd).setColor(color1);
                vertices.addVertexWith2DPose(pose, px1, yStart).setColor(color0);
            }
        }

        private static int lerpColor(int top, int bottom, float t) {
            int aTop = (top >> 24) & 0xFF, rTop = (top >> 16) & 0xFF, gTop = (top >> 8) & 0xFF, bTop = top & 0xFF;
            int aBot = (bottom >> 24) & 0xFF, rBot = (bottom >> 16) & 0xFF, gBot = (bottom >> 8) & 0xFF, bBot = bottom & 0xFF;
            int a = (int) (aTop + t * (aBot - aTop));
            int r = (int) (rTop + t * (rBot - rTop));
            int g = (int) (gTop + t * (gBot - gTop));
            int b = (int) (bTop + t * (bBot - bTop));
            return (a << 24) | (r << 16) | (g << 8) | b;
        }
    }

    @Environment(EnvType.CLIENT)
    public record RoundedTextureGuiElementRenderState(
            RenderPipeline pipeline,
            TextureSetup textureSetup,
            Matrix3x2f pose,
            Identifier texture,
            int x0, int y0, int x1, int y1,
            int radius,
            int textureWidth,
            int textureHeight,
            @Nullable ScreenRectangle scissorArea,
            @Nullable ScreenRectangle bounds
    ) implements GuiElementRenderState {

        public RoundedTextureGuiElementRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose,
                                                   Identifier texture,
                                                   int x0, int y0, int x1, int y1,
                                                   int radius,
                                                   int textureWidth,
                                                   int textureHeight,
                                                   @Nullable ScreenRectangle scissorArea) {
            this(pipeline, textureSetup, pose, texture, x0, y0, x1, y1, radius, textureWidth, textureHeight,
                    scissorArea, createBounds(x0, y0, x1, y1, pose, scissorArea));
        }

        @Nullable
        private static ScreenRectangle createBounds(int x0, int y0, int x1, int y1,
                                               Matrix3x2f pose, @Nullable ScreenRectangle scissorArea) {
            ScreenRectangle ScreenRectangle = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
            return scissorArea != null ? scissorArea.intersection(ScreenRectangle) : ScreenRectangle;
        }

        @Override
        public void buildVertices(@NotNull VertexConsumer vertices) {
            int width = x1 - x0;
            int height = y1 - y0;
            if (width <= 0 || height <= 0) return;

            for (int dy = 0; dy < height; dy++) {
                int currentY = y0 + dy;

                int leftX = x0;
                int rightX = x1 - 1;

                if (dy < radius) {
                    int dd = radius - dy;
                    int offsetX = (int) Math.sqrt((double) radius * radius - (double) dd * dd);
                    leftX = x0 + radius - offsetX;
                    rightX = x1 - radius + offsetX - 1;
                } else if (dy >= height - radius) {
                    int dd = dy - (height - radius - 1);
                    int offsetX = (int) Math.sqrt((double) radius * radius - (double) dd * dd);
                    leftX = x0 + radius - offsetX;
                    rightX = x1 - radius + offsetX - 1;
                }

                if (rightX < leftX) continue;

                float u0 = (leftX - x0) / (float) width;
                float u1 = (rightX + 1 - x0) / (float) width;
                float v0 = dy / (float) height;
                float v1 = (dy + 1) / (float) height;

                float px0 = leftX;
                float py0 = currentY;
                float px1 = rightX + 1;
                float py1 = currentY + 1;

                vertices.addVertexWith2DPose(pose, px0, py0).setUv(u0, v0).setColor(0xFFFFFFFF);
                vertices.addVertexWith2DPose(pose, px0, py1).setUv(u0, v1).setColor(0xFFFFFFFF);
                vertices.addVertexWith2DPose(pose, px1, py1).setUv(u1, v1).setColor(0xFFFFFFFF);
                vertices.addVertexWith2DPose(pose, px1, py0).setUv(u1, v0).setColor(0xFFFFFFFF);
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public record CircleQuarterRenderState(
            RenderPipeline pipeline,
            TextureSetup textureSetup,
            Matrix3x2f pose,
            float centerX,
            float centerY,
            int radius,
            int color,
            Corner corner,
            @Nullable ScreenRectangle scissorArea,
            @Nullable ScreenRectangle bounds
    ) implements GuiElementRenderState {

        public CircleQuarterRenderState(
                RenderPipeline pipeline,
                TextureSetup textureSetup,
                Matrix3x2f pose,
                float centerX,
                float centerY,
                int radius,
                int color,
                Corner corner,
                @Nullable ScreenRectangle scissorArea
        ) {
            this(pipeline, textureSetup, pose, centerX, centerY, radius, color, corner,
                    scissorArea, createBounds(centerX, centerY, radius, pose, scissorArea));
        }

        @Nullable
        private static ScreenRectangle createBounds(float cx, float cy, int radius,
                                               Matrix3x2f pose, @Nullable ScreenRectangle scissorArea) {
            ScreenRectangle rect = new ScreenRectangle((int) (cx - radius), (int) (cy - radius),
                    radius * 2, radius * 2).transformMaxBounds(pose);
            return scissorArea != null ? scissorArea.intersection(rect) : rect;
        }

        @Override
        public void buildVertices(@NotNull VertexConsumer vertices) {
            for (int y = 0; y <= radius; y++) {
                for (int x = 0; x <= radius; x++) {
                    if (x * x + y * y <= radius * radius) {
                        float drawX = centerX;
                        float drawY = centerY;

                        float px0 = drawX, px1 = drawX, py0 = drawY, py1 = drawY;

                        switch (corner) {
                            case TOP_LEFT -> {
                                px0 = drawX - x;
                                px1 = drawX - x + 1;
                                py0 = drawY - y;
                                py1 = drawY - y + 1;
                            }
                            case TOP_RIGHT -> {
                                px0 = drawX + x;
                                px1 = drawX + x + 1;
                                py0 = drawY - y;
                                py1 = drawY - y + 1;
                            }
                            case BOTTOM_LEFT -> {
                                px0 = drawX - x;
                                px1 = drawX - x + 1;
                                py0 = drawY + y;
                                py1 = drawY + y + 1;
                            }
                            case BOTTOM_RIGHT -> {
                                px0 = drawX + x;
                                px1 = drawX + x + 1;
                                py0 = drawY + y;
                                py1 = drawY + y + 1;
                            }
                        }

                        vertices.addVertexWith2DPose(pose, px0, py0).setColor(color);
                        vertices.addVertexWith2DPose(pose, px0, py1).setColor(color);
                        vertices.addVertexWith2DPose(pose, px1, py1).setColor(color);
                        vertices.addVertexWith2DPose(pose, px1, py0).setColor(color);
                    }
                }
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public record CircleQuarterOutlineRenderState(
            RenderPipeline pipeline,
            TextureSetup textureSetup,
            Matrix3x2f pose,
            float centerX,
            float centerY,
            int radius,
            int thickness,
            int color,
            Corner corner,
            @Nullable ScreenRectangle scissorArea,
            @Nullable ScreenRectangle bounds
    ) implements GuiElementRenderState {

        public CircleQuarterOutlineRenderState(
                RenderPipeline pipeline,
                TextureSetup textureSetup,
                Matrix3x2f pose,
                float centerX,
                float centerY,
                int radius,
                int thickness,
                int color,
                Corner corner,
                @Nullable ScreenRectangle scissorArea
        ) {
            this(pipeline, textureSetup, pose, centerX, centerY, radius, thickness, color, corner,
                    scissorArea, createBounds(centerX, centerY, radius, pose, scissorArea));
        }

        @Nullable
        private static ScreenRectangle createBounds(float cx, float cy, int radius,
                                               Matrix3x2f pose, @Nullable ScreenRectangle scissorArea) {
            ScreenRectangle rect = new ScreenRectangle((int) (cx - radius), (int) (cy - radius), radius * 2, radius * 2).transformMaxBounds(pose);
            return scissorArea != null ? scissorArea.intersection(rect) : rect;
        }

        @Override
        public void buildVertices(@NotNull VertexConsumer vertices) {
            int outerRadiusSq = radius * radius;
            int innerRadiusSq = (radius - thickness) * (radius - thickness);

            for (int y = 0; y <= radius; y++) {
                for (int x = 0; x <= radius; x++) {
                    int distSq = x * x + y * y;
                    if (distSq <= outerRadiusSq && distSq >= innerRadiusSq) {
                        float drawX = centerX;
                        float drawY = centerY;

                        float px0 = drawX, px1 = drawX, py0 = drawY, py1 = drawY;

                        switch (corner) {
                            case TOP_LEFT -> {
                                px0 = drawX - x;
                                px1 = drawX - x + 1;
                                py0 = drawY - y;
                                py1 = drawY - y + 1;
                            }
                            case TOP_RIGHT -> {
                                px0 = drawX + x;
                                px1 = drawX + x + 1;
                                py0 = drawY - y;
                                py1 = drawY - y + 1;
                            }
                            case BOTTOM_LEFT -> {
                                px0 = drawX - x;
                                px1 = drawX - x + 1;
                                py0 = drawY + y;
                                py1 = drawY + y + 1;
                            }
                            case BOTTOM_RIGHT -> {
                                px0 = drawX + x;
                                px1 = drawX + x + 1;
                                py0 = drawY + y;
                                py1 = drawY + y + 1;
                            }
                        }

                        vertices.addVertexWith2DPose(pose, px0, py0).setColor(color);
                        vertices.addVertexWith2DPose(pose, px0, py1).setColor(color);
                        vertices.addVertexWith2DPose(pose, px1, py1).setColor(color);
                        vertices.addVertexWith2DPose(pose, px1, py0).setColor(color);
                    }
                }
            }
        }
    }

    public enum ArrowDirection {
        UP, DOWN, LEFT, RIGHT
    }

    public enum Corner {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }
}
