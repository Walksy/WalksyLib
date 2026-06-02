package main.walksy.lib.core.config.local.options.type;

import main.walksy.lib.core.gui.Graphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.awt.*;
import java.util.Arrays;
import java.util.function.Supplier;

public class PixelGrid {
    private final int width;
    private final int height;
    private boolean[][] pixels;

    public PixelGrid(final int width, final int height, final boolean[][] pixels) {
        this.width = width;
        this.height = height;
        this.pixels = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                this.pixels[y][x] = y < pixels.length && x < pixels[y].length && pixels[y][x];
            }
        }
    }

    public void render(final GuiGraphicsExtractor context, final Supplier<Point> position, final boolean blend) {
        if (position == null || (position.get().x == -1) && (position.get().y == -1)) {
            return;
        }
        new Graphics(context).renderGridTexture(this, position.get().x, position.get().y, 1, 0, blend);
    }

    public void render(final GuiGraphicsExtractor context, final float x, final float y, final boolean blend) {
        new Graphics(context).renderGridTexture(this, x, y, 1, 0, blend);
    }

    public boolean getPixel(final int x, final int y) {
        return this.pixels[y][x];
    }

    public void setPixel(final int x, final int y, final boolean val) {
        this.pixels[y][x] = val;
    }

    public void setPixels(final boolean[][] pixels) {
        this.pixels = pixels;
    }

    public boolean[][] getPixels() {
        return this.pixels;
    }

    public PixelGrid copy() {
        return new PixelGrid(this.width, this.height, this.pixels);
    }

    public int getWidth() { return this.width; }
    public int getHeight() { return this.height; }

    public static Builder create(final int width, final int height) {
        return new Builder(width, height);
    }

    public static Builder create() {
        return create(15, 15);
    }

    public static class Builder {
        private final int width;
        private final int height;
        private final boolean[][] pixels;

        public Builder(final int width, final int height) {
            this.width = width;
            this.height = height;
            this.pixels = new boolean[height][width];
        }

        public Builder set(final int x, final int y) {
            if (x >= 0 && x < this.width && y >= 0 && y < this.height) {
                this.pixels[y][x] = true;
            }
            return this;
        }

        public PixelGrid build() {
            return new PixelGrid(this.width, this.height, this.pixels);
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PixelGrid other)) return false;
        if (this.width != other.width || this.height != other.height) return false;

        return Arrays.deepEquals(this.pixels, other.pixels);
    }

}
