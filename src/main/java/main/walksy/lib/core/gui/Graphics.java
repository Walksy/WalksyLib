package main.walksy.lib.core.gui;

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

public record Graphics(GuiGraphicsExtractor extractor) {

    public void drawRoundedTexture(
            final RenderPipeline pipeline,
            final Identifier sprite,
            final int x,
            final int y,
            final int width,
            final int height,
            final int radius,
            final int textureWidth,
            final int textureHeight
    ) {
        final TextureManager manager = Minecraft.getInstance().getTextureManager();
        manager.getTexture(sprite);
        final AbstractTexture texture = manager.getTexture(sprite);
        final TextureSetup textureSetup = TextureSetup.singleTexture(texture.getTextureView(), texture.getSampler());
        final ScreenRectangle current = this.extractor.scissorStack.peek();
        this.extractor.guiRenderState.addGuiElement(new RoundedTextureGuiElementRenderState(
                pipeline,
                textureSetup,
                this.extractor.pose(),
                sprite,
                x,
                y,
                x + width,
                y + height,
                radius,
                textureWidth,
                textureHeight,
                current
        ));
    }

    public void renderMiniArrow(final float x, final float y, final float scale, final ArrowDirection direction, final int color) {
        final int[][] lines = {
                { -3, 0,  3, 1 },
                { -2, 1,  2, 2 },
                { -1, 2,  1, 3 }
        };

        for (final int[] line : lines) {
            final float x1 = line[0] * scale;
            final float y1 = line[1] * scale;
            final float x2 = line[2] * scale;
            final float y2 = line[3] * scale;

            switch (direction) {
                case UP -> this.extractor.fill((int)(x + x1), (int)(y - y2), (int)(x + x2), (int)(y - y1), color);
                case DOWN -> this.extractor.fill((int)(x + x1), (int)(y + y1), (int)(x + x2), (int)(y + y2), color);
                case LEFT -> this.extractor.fill((int)(x - y2), (int)(y + x1), (int)(x - y1), (int)(y + x2), color);
                case RIGHT -> this.extractor.fill((int)(x + y1), (int)(y + x1), (int)(x + y2), (int)(y + x2), color);
            }
        }
    }

    public void verticalLine(float x, float y1, float y2, final int color) {
        if (y2 < y1) {
            final float i = y1;
            y1 = y2;
            y2 = i;
        }
        this.fill(x, y1 + 1, (float) (x + 0.8), y2, color);
    }

    public void fillRoundedRectGradient(final int x, final int y, final int width, final int height, final int radius, final int colorTop, final int colorBottom) {
        this.extractor.guiRenderState.addGuiElement(new RoundedGradientRectGuiElementRenderState(
                RenderPipelines.GUI,
                TextureSetup.noTexture(),
                this.extractor.pose(),
                x,
                y,
                x + width,
                y + height,
                radius,
                colorTop,
                colorBottom,
                this.extractor.scissorStack.peek()
        ));
    }

    public void fillRoundedRect(final float x, final float y, final float width, final float height, final int radius, final int color) {
        final float right = x + width;
        final float bottom = y + height;
        this.fill(x + radius + 1, y, right - radius - 1, y + radius, color);
        this.fill(x + radius + 1, bottom - radius, right - radius - 1, bottom, color);

        this.fill(x, y + radius + 1, x + radius, bottom - radius - 1, color);
        this.fill(right - radius, y + radius + 1, right, bottom - radius - 1, color);

        this.fill(x + radius + 1, y + radius, right - radius - 1, bottom - radius, color);

        this.fill(x + radius, y + radius + 1, x + radius + 1, bottom - radius - 1, color);
        this.fill(right - radius - 1, y + radius + 1, right - radius, bottom - radius - 1, color);

        this.fillCircleQuarter(x + radius, y + radius, radius, color, Corner.TOP_LEFT);
        this.fillCircleQuarter(right - radius - 1, y + radius, radius, color, Corner.TOP_RIGHT);
        this.fillCircleQuarter(x + radius, bottom - radius - 1, radius, color, Corner.BOTTOM_LEFT);
        this.fillCircleQuarter(right - radius - 1, bottom - radius - 1, radius, color, Corner.BOTTOM_RIGHT);
    }

    public void fillRoundedRectOutline(final int x, final int y, final int width, final int height, final int radius, final int thickness, final int color) {
        final int right = x + width;
        final int bottom = y + height;

        this.extractor.fill(x + radius + 1, y, right - radius - 1, y + thickness, color);
        this.extractor.fill(x + radius + 1, bottom - thickness, right - radius - 1, bottom, color);

        this.extractor.fill(x, y + radius + 1, x + thickness, bottom - radius - 1, color);
        this.extractor.fill(right - thickness, y + radius + 1, right, bottom - radius - 1, color);

        this.drawCircleQuarterOutline(x + radius, y + radius, radius, thickness, color, Corner.TOP_LEFT);
        this.drawCircleQuarterOutline(right - radius - 1, y + radius, radius, thickness, color, Corner.TOP_RIGHT);
        this.drawCircleQuarterOutline(x + radius, bottom - radius - 1, radius, thickness, color, Corner.BOTTOM_LEFT);
        this.drawCircleQuarterOutline(right - radius - 1, bottom - radius - 1, radius, thickness, color, Corner.BOTTOM_RIGHT);
    }

    public void fillRoundedRectOutline_ModWidget(final int x, final int y, final int width, final int height, final int radius, final int thickness, final int color) {
        final int right = x + width;
        final int bottom = y + height;

        this.extractor.fill(x + 1, y, right - radius - 1, y + thickness, color);
        this.extractor.fill(x + 1, bottom - thickness, right - radius - 1, bottom, color);

        this.extractor.fill(x, y, x + thickness, bottom, color);
        this.extractor.fill(right - thickness, y + radius + 1, right, bottom - radius - 1, color);

        this.drawCircleQuarterOutline(right - radius - 1, y + radius, radius, thickness, color, Corner.TOP_RIGHT);
        this.drawCircleQuarterOutline(right - radius - 1, bottom - radius - 1, radius, thickness, color, Corner.BOTTOM_RIGHT);
    }

    public void drawHueSaturationValueBox(final int x, final int y, final int width, final int height, final int radius, final float hue) {
        this.extractor.guiRenderState.addGuiElement(
                new SaturationBoxGuiElementRenderState(
                        RenderPipelines.GUI,
                        TextureSetup.noTexture(),
                        new Matrix3x2f(this.extractor.pose()),
                        x, y, x + width, y + height,
                        radius,
                        hue,
                        this.extractor.scissorStack.peek()
                )
        );
    }

    public void drawRoundedHueSlider(final int x, final int y, final int width, final int height, final int radius) {
        this.extractor.guiRenderState.addGuiElement(new HueSliderGuiElementRenderState(
                RenderPipelines.GUI,
                TextureSetup.noTexture(),
                new Matrix3x2f(this.extractor.pose()),
                x,
                y,
                x + width,
                y + height,
                radius,
                this.extractor.scissorStack.peek()
        ));
    }

    public void fillCircleQuarter(final float centerX, final float centerY, final int radius, final int color, final Corner corner) {
        this.extractor.guiRenderState.addGuiElement(new CircleQuarterRenderState(
                RenderPipelines.GUI,
                TextureSetup.noTexture(),
                this.extractor.pose(),
                centerX,
                centerY,
                radius,
                color,
                corner,
                this.extractor.scissorStack.peek()
        ));
    }

    private void drawCircleQuarterOutline(final int centerX, final int centerY, final int radius, final int thickness, final int color, final Corner corner) {
        this.extractor.guiRenderState.addGuiElement(new CircleQuarterOutlineRenderState(
                RenderPipelines.GUI,
                TextureSetup.noTexture(),
                this.extractor.pose(),
                centerX,
                centerY,
                radius,
                thickness,
                color,
                corner,
                this.extractor.scissorStack.peek()
        ));
    }

    public void renderGridTexture(final PixelGrid grid, final float x1, final float y1, final int pixelSize, final int gapSize, final boolean blend) {
        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                if (grid.getPixel(x, y)) {
                    final float px = x1 + x * (pixelSize + gapSize);
                    final float py = y1 + y * (pixelSize + gapSize);
                    this.drawFilledRectangle(px, py, px + pixelSize, py + pixelSize, Color.WHITE, blend);
                }
            }
        }
    }

    public void drawFilledRectangle(final float x1, final float y1, final float x2, final float y2, final Color color, final boolean blend) {
        final ScreenRectangle current = this.extractor.scissorStack.peek();
        this.extractor.pose().pushMatrix();
        this.setGlProperty(2848, false);
        this.extractor.guiRenderState.addGuiElement(new ColoredFloatQuadGuiElementRenderState(
                blend ? RenderPipelines.GUI_INVERT : RenderPipelines.GUI,
                TextureSetup.noTexture(),
                new Matrix3x2f(this.extractor.pose()),
                x1, y1, x2, y2,
                color.getRGB(), color.getRGB(),
                current
        ));
        this.extractor.pose().popMatrix();
    }

    private void setGlProperty(final int property, final boolean isEnabled) {
        if (isEnabled) {
            GL11.glEnable(property);
        } else {
            GL11.glDisable(property);
        }
    }

    public void renderGridOutline(final PixelGrid grid, final int x1, final int y1, final int pixelSize, final int gapSize) {
        final int gridWidthPixels  = grid.getWidth()  * pixelSize + (grid.getWidth()  - 1) * gapSize;
        final int gridHeightPixels = grid.getHeight() * pixelSize + (grid.getHeight() - 1) * gapSize;

        final int x2 = x1 + gridWidthPixels;
        final int y2 = y1 + gridHeightPixels;

        final float borderWidth = 0.3f;
        final int blue = new Color(0, 100, 255).getRGB();

        this.fill(x1 - borderWidth, y1 - borderWidth, x2 + borderWidth, y1, blue);
        this.fill(x1 - borderWidth, y2, x2 + borderWidth, y2 + borderWidth, blue);
        this.fill(x1 - borderWidth, y1, x1, y2, blue);
        this.fill(x2, y1, x2 + borderWidth, y2, blue);
    }

    public void renderGridOutline(final PixelGrid grid, final int x1, final int y1, final int pixelSize, final int gapSize, final int outlineColor, final boolean markCenter) {
        if (markCenter) {
            final int px = x1 + (grid.getWidth()  / 2) * (pixelSize + gapSize);
            final int py = y1 + (grid.getHeight() / 2) * (pixelSize + gapSize);
            this.extractor.fill(px + 1, py + 1, px + pixelSize - 1, py + pixelSize - 1, new Color(255, 100, 100, 100).getRGB());
        }

        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                final int px = x1 + x * (pixelSize + gapSize);
                final int py = y1 + y * (pixelSize + gapSize);

                if (grid.getPixel(x, y)) {
                    this.extractor.fill(px + 1, py + 1, px + pixelSize - 1, py + pixelSize - 1, Color.WHITE.getRGB());
                }
                this.extractor.fill(px + 1, py, px + pixelSize - 1, py + 1, outlineColor);
                this.extractor.fill(px + 1, py + pixelSize - 1, px + pixelSize - 1, py + pixelSize, outlineColor);
                this.extractor.fill(px, py, px + 1, py + pixelSize, outlineColor);
                this.extractor.fill(px + pixelSize - 1, py, px + pixelSize, py + pixelSize, outlineColor);
            }
        }
    }

    public void fill(float x1, float y1, float x2, float y2, final int color) {
        final ScreenRectangle current = this.extractor.scissorStack.peek();
        if (x1 < x2) { final float i = x1; x1 = x2; x2 = i; }
        if (y1 < y2) { final float i = y1; y1 = y2; y2 = i; }
        this.extractor.guiRenderState.addGuiElement(new ColoredFloatQuadGuiElementRenderState(
                RenderPipelines.GUI, TextureSetup.noTexture(), new Matrix3x2f(this.extractor.pose()), x1, y1, x2, y2, color, color, current
        ));
    }


    @Environment(EnvType.CLIENT)
    public record ColoredFloatQuadGuiElementRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose, float x0, float y0, float x1, float y1, int col1, int col2, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements GuiElementRenderState {
        public ColoredFloatQuadGuiElementRenderState(final RenderPipeline pipeline, final TextureSetup textureSetup, final Matrix3x2f pose, final float x0, final float y0, final float x1, final float y1, final int col1, final int col2, @Nullable final ScreenRectangle scissorArea) {
            this(pipeline, textureSetup, pose, x0, y0, x1, y1, col1, col2, scissorArea, createBounds(x0, y0, x1, y1, pose, scissorArea));
        }

        @Override
        public void buildVertices(@NotNull final VertexConsumer vertices) {
            vertices.addVertexWith2DPose(this.pose(), (float) this.x0(), (float) this.y0()).setColor(this.col1());
            vertices.addVertexWith2DPose(this.pose(), (float) this.x0(), (float) this.y1()).setColor(this.col2());
            vertices.addVertexWith2DPose(this.pose(), (float) this.x1(), (float) this.y1()).setColor(this.col2());
            vertices.addVertexWith2DPose(this.pose(), (float) this.x1(), (float) this.y0()).setColor(this.col1());
        }

        @Nullable
        private static ScreenRectangle createBounds(final float x0, final float y0, final float x1, final float y1, final Matrix3x2f pose, @Nullable final ScreenRectangle scissorArea) {
            final ScreenRectangle bounds = (new ScreenRectangle((int) x0, (int) y0, (int) (x1 - x0), (int) (y1 - y0))).transformMaxBounds(pose);
            return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
        }
    }

    @Environment(EnvType.CLIENT)
    public record SaturationBoxGuiElementRenderState(
            RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose,
            int x0, int y0, int x1, int y1, int radius, float hue,
            @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds
    ) implements GuiElementRenderState {

        public SaturationBoxGuiElementRenderState(final RenderPipeline pipeline, final TextureSetup textureSetup, final Matrix3x2f pose,
                                                  final int x0, final int y0, final int x1, final int y1, final int radius, final float hue,
                                                  @Nullable final ScreenRectangle scissorArea) {
            this(pipeline, textureSetup, pose, x0, y0, x1, y1, radius, hue, scissorArea, createBounds(x0, y0, x1, y1, pose, scissorArea));
        }

        @Nullable
        private static ScreenRectangle createBounds(final int x0, final int y0, final int x1, final int y1, final Matrix3x2f pose, @Nullable final ScreenRectangle scissorArea) {
            final ScreenRectangle bounds = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
            return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
        }

        @Override
        public void buildVertices(@NotNull final VertexConsumer vertices) {
            final int width = this.x1 - this.x0;
            final int height = this.y1 - this.y0;
            if (width <= 0 || height <= 0) return;

            final int steps = Math.max(2, height);
            for (int i = 0; i < steps; i++) {
                final float t0 = i / (float) steps;
                final float t1 = (i + 1) / (float) steps;

                final float value0 = 1.0f - t0;
                final float value1 = 1.0f - t1;

                final int leftColor0  = Color.HSBtoRGB(this.hue, 0f, value0) | 0xFF000000;
                final int rightColor0 = Color.HSBtoRGB(this.hue, 1f, value0) | 0xFF000000;
                final int leftColor1  = Color.HSBtoRGB(this.hue, 0f, value1) | 0xFF000000;
                final int rightColor1 = Color.HSBtoRGB(this.hue, 1f, value1) | 0xFF000000;

                final float yStart = this.y0 + t0 * height;
                final float yEnd   = this.y0 + t1 * height;
                final int dy0 = (int) (yStart - this.y0);
                final int dy1 = (int) (yEnd   - this.y0);

                int leftX0 = this.x0, rightX0 = this.x1 - 1;
                int leftX1 = this.x0, rightX1 = this.x1 - 1;

                if (this.radius > 0) {
                    if (dy0 < this.radius) {
                        final int dd = this.radius - dy0;
                        final int offsetX = (int) Math.sqrt((double) this.radius * this.radius - (double) dd * dd);
                        leftX0 = this.x0 + this.radius - offsetX;
                        rightX0 = this.x1 - this.radius + offsetX - 1;
                    } else if (dy0 >= height - this.radius) {
                        final int dd = dy0 - (height - this.radius - 1);
                        final int offsetX = (int) Math.sqrt((double) this.radius * this.radius - (double) dd * dd);
                        leftX0 = this.x0 + this.radius - offsetX;
                        rightX0 = this.x1 - this.radius + offsetX - 1;
                    }

                    if (dy1 < this.radius) {
                        final int dd = this.radius - dy1;
                        final int offsetX = (int) Math.sqrt((double) this.radius * this.radius - (double) dd * dd);
                        leftX1 = this.x0 + this.radius - offsetX;
                        rightX1 = this.x1 - this.radius + offsetX - 1;
                    } else if (dy1 >= height - this.radius) {
                        final int dd = dy1 - (height - this.radius - 1);
                        final int offsetX = (int) Math.sqrt((double) this.radius * this.radius - (double) dd * dd);
                        leftX1 = this.x0 + this.radius - offsetX;
                        rightX1 = this.x1 - this.radius + offsetX - 1;
                    }
                }

                final int leftX  = Math.max(leftX0, leftX1);
                final int rightX = Math.min(rightX0, rightX1);
                if (rightX <= leftX) continue;

                vertices.addVertexWith2DPose(this.pose, leftX,      yStart).setColor(leftColor0);
                vertices.addVertexWith2DPose(this.pose, leftX,      yEnd).setColor(leftColor1);
                vertices.addVertexWith2DPose(this.pose, rightX + 1, yEnd).setColor(rightColor1);
                vertices.addVertexWith2DPose(this.pose, rightX + 1, yStart).setColor(rightColor0);
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public record HueSliderGuiElementRenderState(
            RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose,
            int x0, int y0, int x1, int y1, int radius,
            @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds
    ) implements GuiElementRenderState {

        public HueSliderGuiElementRenderState(final RenderPipeline pipeline, final TextureSetup textureSetup, final Matrix3x2f pose,
                                              final int x0, final int y0, final int x1, final int y1, final int radius,
                                              @Nullable final ScreenRectangle scissorArea) {
            this(pipeline, textureSetup, pose, x0, y0, x1, y1, radius, scissorArea, createBounds(x0, y0, x1, y1, pose, scissorArea));
        }

        @Nullable
        private static ScreenRectangle createBounds(final int x0, final int y0, final int x1, final int y1, final Matrix3x2f pose, @Nullable final ScreenRectangle scissorArea) {
            final ScreenRectangle bounds = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
            return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
        }

        @Override
        public void buildVertices(@NotNull final VertexConsumer vertices) {
            final int width  = this.x1 - this.x0;
            final int height = this.y1 - this.y0;
            if (width <= 0 || height <= 0) return;

            final int steps = 64;
            for (int i = 0; i < steps; i++) {
                final float t0 = i / (float) steps;
                final float t1 = (i + 1) / (float) steps;

                final int color0 = Color.HSBtoRGB(1.0f - t0, 1f, 1f) | 0xFF000000;
                final int color1 = Color.HSBtoRGB(1.0f - t1, 1f, 1f) | 0xFF000000;

                final float yStart = this.y0 + t0 * height;
                final float yEnd   = this.y0 + t1 * height;
                final int dy0 = (int) (yStart - this.y0);
                final int dy1 = (int) (yEnd   - this.y0);

                int leftX0 = this.x0, rightX0 = this.x1 - 1;
                int leftX1 = this.x0, rightX1 = this.x1 - 1;

                if (dy0 < this.radius) {
                    final int dd = this.radius - dy0;
                    final int offsetX = (int) Math.sqrt((double) this.radius * this.radius - (double) dd * dd);
                    leftX0 = this.x0 + this.radius - offsetX;
                    rightX0 = this.x1 - this.radius + offsetX - 1;
                } else if (dy0 >= height - this.radius) {
                    final int dd = dy0 - (height - this.radius - 1);
                    final int offsetX = (int) Math.sqrt((double) this.radius * this.radius - (double) dd * dd);
                    leftX0 = this.x0 + this.radius - offsetX;
                    rightX0 = this.x1 - this.radius + offsetX - 1;
                }

                if (dy1 < this.radius) {
                    final int dd = this.radius - dy1;
                    final int offsetX = (int) Math.sqrt((double) this.radius * this.radius - (double) dd * dd);
                    leftX1 = this.x0 + this.radius - offsetX;
                    rightX1 = this.x1 - this.radius + offsetX - 1;
                } else if (dy1 >= height - this.radius) {
                    final int dd = dy1 - (height - this.radius - 1);
                    final int offsetX = (int) Math.sqrt((double) this.radius * this.radius - (double) dd * dd);
                    leftX1 = this.x0 + this.radius - offsetX;
                    rightX1 = this.x1 - this.radius + offsetX - 1;
                }

                final int leftX  = Math.max(leftX0, leftX1);
                final int rightX = Math.min(rightX0, rightX1);
                if (rightX <= leftX) continue;

                vertices.addVertexWith2DPose(this.pose, leftX,      yStart).setColor(color0);
                vertices.addVertexWith2DPose(this.pose, leftX,      yEnd).setColor(color1);
                vertices.addVertexWith2DPose(this.pose, rightX + 1, yEnd).setColor(color1);
                vertices.addVertexWith2DPose(this.pose, rightX + 1, yStart).setColor(color0);
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public record RoundedGradientRectGuiElementRenderState(
            RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose,
            int x0, int y0, int x1, int y1, int radius, int colorTop, int colorBottom,
            @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds
    ) implements GuiElementRenderState {

        public RoundedGradientRectGuiElementRenderState(final RenderPipeline pipeline, final TextureSetup textureSetup, final Matrix3x2f pose,
                                                        final int x0, final int y0, final int x1, final int y1,
                                                        final int radius, final int colorTop, final int colorBottom,
                                                        @Nullable final ScreenRectangle scissorArea) {
            this(pipeline, textureSetup, pose, x0, y0, x1, y1, radius, colorTop, colorBottom, scissorArea, createBounds(x0, y0, x1, y1, pose, scissorArea));
        }

        @Nullable
        private static ScreenRectangle createBounds(final int x0, final int y0, final int x1, final int y1, final Matrix3x2f pose, @Nullable final ScreenRectangle scissorArea) {
            final ScreenRectangle bounds = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
            return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
        }

        @Override
        public void buildVertices(@NotNull final VertexConsumer vertices) {
            final int width  = this.x1 - this.x0;
            final int height = this.y1 - this.y0;
            if (width <= 0 || height <= 0) return;

            final int steps = Math.max(2, height);
            for (int i = 0; i < steps; i++) {
                final float t0 = i / (float) steps;
                final float t1 = (i + 1) / (float) steps;

                final int color0 = lerpColor(this.colorTop, this.colorBottom, t0);
                final int color1 = lerpColor(this.colorTop, this.colorBottom, t1);

                final float yStart = this.y0 + t0 * height;
                final float yEnd   = this.y0 + t1 * height;
                final int dy0 = (int) (yStart - this.y0);
                final int dy1 = (int) (yEnd   - this.y0);

                int leftX0 = this.x0, rightX0 = this.x1 - 1;
                int leftX1 = this.x0, rightX1 = this.x1 - 1;

                if (dy0 < this.radius) {
                    final int dd = this.radius - dy0;
                    final int offsetX = (int) Math.sqrt((double) this.radius * this.radius - (double) dd * dd);
                    leftX0 = this.x0 + this.radius - offsetX;
                    rightX0 = this.x1 - this.radius + offsetX - 1;
                } else if (dy0 >= height - this.radius) {
                    final int dd = dy0 - (height - this.radius - 1);
                    final int offsetX = (int) Math.sqrt((double) this.radius * this.radius - (double) dd * dd);
                    leftX0 = this.x0 + this.radius - offsetX;
                    rightX0 = this.x1 - this.radius + offsetX - 1;
                }

                if (dy1 < this.radius) {
                    final int dd = this.radius - dy1;
                    final int offsetX = (int) Math.sqrt((double) this.radius * this.radius - (double) dd * dd);
                    leftX1 = this.x0 + this.radius - offsetX;
                    rightX1 = this.x1 - this.radius + offsetX - 1;
                } else if (dy1 >= height - this.radius) {
                    final int dd = dy1 - (height - this.radius - 1);
                    final int offsetX = (int) Math.sqrt((double) this.radius * this.radius - (double) dd * dd);
                    leftX1 = this.x0 + this.radius - offsetX;
                    rightX1 = this.x1 - this.radius + offsetX - 1;
                }

                final int leftX  = Math.max(leftX0, leftX1);
                final int rightX = Math.min(rightX0, rightX1);
                if (rightX <= leftX) continue;

                vertices.addVertexWith2DPose(this.pose, leftX,      yStart).setColor(color0);
                vertices.addVertexWith2DPose(this.pose, leftX,      yEnd).setColor(color1);
                vertices.addVertexWith2DPose(this.pose, rightX + 1, yEnd).setColor(color1);
                vertices.addVertexWith2DPose(this.pose, rightX + 1, yStart).setColor(color0);
            }
        }

        private static int lerpColor(final int top, final int bottom, final float t) {
            final int aTop = (top >> 24) & 0xFF,    rTop = (top >> 16) & 0xFF,    gTop = (top >> 8) & 0xFF,    bTop = top & 0xFF;
            final int aBot = (bottom >> 24) & 0xFF, rBot = (bottom >> 16) & 0xFF, gBot = (bottom >> 8) & 0xFF, bBot = bottom & 0xFF;
            return ((int)(aTop + t * (aBot - aTop)) << 24)
                 | ((int)(rTop + t * (rBot - rTop)) << 16)
                 | ((int)(gTop + t * (gBot - gTop)) <<  8)
                 |  (int)(bTop + t * (bBot - bTop));
        }
    }

    @Environment(EnvType.CLIENT)
    public record RoundedTextureGuiElementRenderState(
            RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose,
            Identifier texture, int x0, int y0, int x1, int y1,
            int radius, int textureWidth, int textureHeight,
            @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds
    ) implements GuiElementRenderState {

        public RoundedTextureGuiElementRenderState(final RenderPipeline pipeline, final TextureSetup textureSetup, final Matrix3x2f pose,
                                                   final Identifier texture,
                                                   final int x0, final int y0, final int x1, final int y1,
                                                   final int radius, final int textureWidth, final int textureHeight,
                                                   @Nullable final ScreenRectangle scissorArea) {
            this(pipeline, textureSetup, pose, texture, x0, y0, x1, y1, radius, textureWidth, textureHeight, scissorArea, createBounds(x0, y0, x1, y1, pose, scissorArea));
        }

        @Nullable
        private static ScreenRectangle createBounds(final int x0, final int y0, final int x1, final int y1, final Matrix3x2f pose, @Nullable final ScreenRectangle scissorArea) {
            final ScreenRectangle bounds = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
            return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
        }

        @Override
        public void buildVertices(@NotNull final VertexConsumer vertices) {
            final int width  = this.x1 - this.x0;
            final int height = this.y1 - this.y0;
            if (width <= 0 || height <= 0) return;

            for (int dy = 0; dy < height; dy++) {
                final int currentY = this.y0 + dy;

                int leftX = this.x0;
                int rightX = this.x1 - 1;

                if (dy < this.radius) {
                    final int dd = this.radius - dy;
                    final int offsetX = (int) Math.sqrt((double) this.radius * this.radius - (double) dd * dd);
                    leftX  = this.x0 + this.radius - offsetX;
                    rightX = this.x1 - this.radius + offsetX - 1;
                } else if (dy >= height - this.radius) {
                    final int dd = dy - (height - this.radius - 1);
                    final int offsetX = (int) Math.sqrt((double) this.radius * this.radius - (double) dd * dd);
                    leftX  = this.x0 + this.radius - offsetX;
                    rightX = this.x1 - this.radius + offsetX - 1;
                }

                if (rightX < leftX) continue;

                final float u0 = (leftX - this.x0) / (float) width;
                final float u1 = (rightX + 1 - this.x0) / (float) width;
                final float v0 = dy / (float) height;
                final float v1 = (dy + 1) / (float) height;

                vertices.addVertexWith2DPose(this.pose, leftX,      currentY).setUv(u0, v0).setColor(0xFFFFFFFF);
                vertices.addVertexWith2DPose(this.pose, leftX,      currentY + 1).setUv(u0, v1).setColor(0xFFFFFFFF);
                vertices.addVertexWith2DPose(this.pose, rightX + 1, currentY + 1).setUv(u1, v1).setColor(0xFFFFFFFF);
                vertices.addVertexWith2DPose(this.pose, rightX + 1, currentY).setUv(u1, v0).setColor(0xFFFFFFFF);
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public record CircleQuarterRenderState(
            RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose,
            float centerX, float centerY, int radius, int color, Corner corner,
            @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds
    ) implements GuiElementRenderState {

        public CircleQuarterRenderState(final RenderPipeline pipeline, final TextureSetup textureSetup, final Matrix3x2f pose,
                                        final float centerX, final float centerY, final int radius, final int color,
                                        final Corner corner, @Nullable final ScreenRectangle scissorArea) {
            this(pipeline, textureSetup, pose, centerX, centerY, radius, color, corner, scissorArea, createBounds(centerX, centerY, radius, pose, scissorArea));
        }

        @Nullable
        private static ScreenRectangle createBounds(final float cx, final float cy, final int radius, final Matrix3x2f pose, @Nullable final ScreenRectangle scissorArea) {
            final ScreenRectangle rect = new ScreenRectangle((int) (cx - radius), (int) (cy - radius), radius * 2, radius * 2).transformMaxBounds(pose);
            return scissorArea != null ? scissorArea.intersection(rect) : rect;
        }

        @Override
        public void buildVertices(@NotNull final VertexConsumer vertices) {
            for (int y = 0; y <= this.radius; y++) {
                for (int x = 0; x <= this.radius; x++) {
                    if (x * x + y * y <= this.radius * this.radius) {
                        float px0, px1, py0, py1;
                        switch (this.corner) {
                            case TOP_LEFT    -> { px0 = this.centerX - x; px1 = this.centerX - x + 1; py0 = this.centerY - y; py1 = this.centerY - y + 1; }
                            case TOP_RIGHT   -> { px0 = this.centerX + x; px1 = this.centerX + x + 1; py0 = this.centerY - y; py1 = this.centerY - y + 1; }
                            case BOTTOM_LEFT -> { px0 = this.centerX - x; px1 = this.centerX - x + 1; py0 = this.centerY + y; py1 = this.centerY + y + 1; }
                            default          -> { px0 = this.centerX + x; px1 = this.centerX + x + 1; py0 = this.centerY + y; py1 = this.centerY + y + 1; }
                        }
                        vertices.addVertexWith2DPose(this.pose, px0, py0).setColor(this.color);
                        vertices.addVertexWith2DPose(this.pose, px0, py1).setColor(this.color);
                        vertices.addVertexWith2DPose(this.pose, px1, py1).setColor(this.color);
                        vertices.addVertexWith2DPose(this.pose, px1, py0).setColor(this.color);
                    }
                }
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public record CircleQuarterOutlineRenderState(
            RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose,
            float centerX, float centerY, int radius, int thickness, int color, Corner corner,
            @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds
    ) implements GuiElementRenderState {

        public CircleQuarterOutlineRenderState(final RenderPipeline pipeline, final TextureSetup textureSetup, final Matrix3x2f pose,
                                               final float centerX, final float centerY, final int radius, final int thickness,
                                               final int color, final Corner corner, @Nullable final ScreenRectangle scissorArea) {
            this(pipeline, textureSetup, pose, centerX, centerY, radius, thickness, color, corner, scissorArea, createBounds(centerX, centerY, radius, pose, scissorArea));
        }

        @Nullable
        private static ScreenRectangle createBounds(final float cx, final float cy, final int radius, final Matrix3x2f pose, @Nullable final ScreenRectangle scissorArea) {
            final ScreenRectangle rect = new ScreenRectangle((int) (cx - radius), (int) (cy - radius), radius * 2, radius * 2).transformMaxBounds(pose);
            return scissorArea != null ? scissorArea.intersection(rect) : rect;
        }

        @Override
        public void buildVertices(@NotNull final VertexConsumer vertices) {
            final int outerRadiusSq = this.radius * this.radius;
            final int innerRadiusSq = (this.radius - this.thickness) * (this.radius - this.thickness);

            for (int y = 0; y <= this.radius; y++) {
                for (int x = 0; x <= this.radius; x++) {
                    final int distSq = x * x + y * y;
                    if (distSq <= outerRadiusSq && distSq >= innerRadiusSq) {
                        float px0, px1, py0, py1;
                        switch (this.corner) {
                            case TOP_LEFT    -> { px0 = this.centerX - x; px1 = this.centerX - x + 1; py0 = this.centerY - y; py1 = this.centerY - y + 1; }
                            case TOP_RIGHT   -> { px0 = this.centerX + x; px1 = this.centerX + x + 1; py0 = this.centerY - y; py1 = this.centerY - y + 1; }
                            case BOTTOM_LEFT -> { px0 = this.centerX - x; px1 = this.centerX - x + 1; py0 = this.centerY + y; py1 = this.centerY + y + 1; }
                            default          -> { px0 = this.centerX + x; px1 = this.centerX + x + 1; py0 = this.centerY + y; py1 = this.centerY + y + 1; }
                        }
                        vertices.addVertexWith2DPose(this.pose, px0, py0).setColor(this.color);
                        vertices.addVertexWith2DPose(this.pose, px0, py1).setColor(this.color);
                        vertices.addVertexWith2DPose(this.pose, px1, py1).setColor(this.color);
                        vertices.addVertexWith2DPose(this.pose, px1, py0).setColor(this.color);
                    }
                }
            }
        }
    }

    public enum ArrowDirection { UP, DOWN, LEFT, RIGHT }

    public enum Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }
}
