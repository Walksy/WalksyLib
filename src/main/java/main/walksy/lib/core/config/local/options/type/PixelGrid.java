package main.walksy.lib.core.config.local.options.type;

import java.util.Arrays;

public class PixelGrid {
    private final int width;
    private final int height;
    private boolean[][] pixels;

    public PixelGrid(int width, int height, boolean[][] pixels) {
        this.width = width;
        this.height = height;
        this.pixels = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                this.pixels[y][x] = y < pixels.length && x < pixels[y].length && pixels[y][x];
            }
        }
    }

    public boolean getPixel(int x, int y) {
        return pixels[y][x];
    }

    public void setPixel(int x, int y, boolean val) {
        pixels[y][x] = val;
    }

    public void setPixels(boolean[][] pixels) {
        this.pixels = pixels;
    }

    public boolean[][] getPixels() {
        return this.pixels;
    }

    public PixelGrid copy() {
        return new PixelGrid(width, height, pixels);
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public static Builder create(int width, int height) {
        return new Builder(width, height);
    }

    public static class Builder {
        private final int width;
        private final int height;
        private final boolean[][] pixels;

        public Builder(int width, int height) {
            this.width = width;
            this.height = height;
            this.pixels = new boolean[height][width];
        }

        public Builder set(int x, int y) {
            if (x >= 0 && x < width && y >= 0 && y < height) {
                pixels[y][x] = true;
            }
            return this;
        }

        public PixelGrid build() {
            return new PixelGrid(width, height, pixels);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PixelGrid other)) return false;
        if (this.width != other.width || this.height != other.height) return false;

        return Arrays.deepEquals(this.pixels, other.pixels);
    }

}
