package main.walksy.lib.api.config.value;

import main.walksy.lib.core.render.PixelGridRenderer;
import main.walksy.lib.core.render.WLGraphics2D;

import java.awt.Point;
import java.util.Arrays;
import java.util.function.Supplier;

public final class PixelGrid implements CopyableValue<PixelGrid> {
    public static final int DEFAULT_COLOR = 0xFFFFFFFF;
    public static final Resolution DEFAULT_RESOLUTION = Resolution.X15;
    private final Resolution resolution;
    private int[][] colors;

    public enum Resolution {
        X15(15),
        X31(31);

        private final int cells;

        Resolution(final int cells) {
            this.cells = cells;
        }

        public int cells() {
            return this.cells;
        }

        public static Resolution of(final int cells) {
            for (final Resolution resolution : values()) {
                if (resolution.cells == cells) {
                    return resolution;
                }
            }
            return null;
        }

        public static Resolution nearest(final int cells) {
            Resolution best = values()[0];
            int bestDistance = Integer.MAX_VALUE;
            for (final Resolution resolution : values()) {
                final int distance = Math.abs(resolution.cells - cells);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = resolution;
                }
            }
            return best;
        }
    }

    public PixelGrid(final Resolution resolution, final boolean[][] pixels) {
        final int cells = resolution.cells();
        this.resolution = resolution;
        this.colors = new int[cells][cells];
        for (int y = 0; y < cells; y++) {
            for (int x = 0; x < cells; x++) {
                this.colors[y][x] = y < pixels.length && x < pixels[y].length && pixels[y][x] ? DEFAULT_COLOR : 0;
            }
        }
    }

    public PixelGrid(final Resolution resolution, final int[][] colors) {
        final int cells = resolution.cells();
        this.resolution = resolution;
        this.colors = new int[cells][cells];
        for (int y = 0; y < cells; y++) {
            for (int x = 0; x < cells; x++) {
                this.colors[y][x] = y < colors.length && x < colors[y].length ? colors[y][x] : 0;
            }
        }
    }

    public void render(final WLGraphics2D graphics, final Supplier<Point> position, final boolean blend) {
        if (position == null || (position.get().x == -1) && (position.get().y == -1)) {
            return;
        }
        PixelGridRenderer.render(graphics, this, position.get().x, position.get().y, 1, 0, blend);
    }

    public void render(final WLGraphics2D graphics, final float x, final float y, final boolean blend) {
        PixelGridRenderer.render(graphics, this, x, y, 1, 0, blend);
    }

    public void render(final WLGraphics2D graphics, final float x, final float y, final int cellSize, final boolean blend) {
        PixelGridRenderer.render(graphics, this, x, y, Math.max(1, cellSize), 0, blend);
    }

    public boolean getPixel(final int x, final int y) {
        return this.colors[y][x] != 0;
    }

    public void setPixel(final int x, final int y, final boolean val) {
        this.colors[y][x] = val ? DEFAULT_COLOR : 0;
    }

    public int getColor(final int x, final int y) {
        return this.colors[y][x];
    }

    public void setColor(final int x, final int y, final int argb) {
        this.colors[y][x] = argb;
    }

    public int[][] getColors() {
        return this.colors;
    }

    public PixelGrid copy() {
        return new PixelGrid(this.resolution, this.colors);
    }

    public Resolution getResolution() {
        return this.resolution;
    }

    public int getWidth() {
        return this.resolution.cells();
    }

    public int getHeight() {
        return this.resolution.cells();
    }

    public static Builder create(final Resolution resolution) {
        return new Builder(resolution);
    }

    public static Builder create() {
        return create(DEFAULT_RESOLUTION);
    }

    public PixelGrid resizedTo(final Resolution target) {
        return new PixelGrid(target, centeredInto(this.colors, this.getWidth(), this.getHeight(), target.cells()));
    }

    public static PixelGrid migrate(final int width, final int height, final int[][] colors) {
        final Resolution target = Resolution.nearest(Math.max(width, height));
        return new PixelGrid(target, centeredInto(colors, width, height, target.cells()));
    }

    private static int[][] centeredInto(final int[][] src, final int srcW, final int srcH, final int size) {
        final int offsetX = (size - srcW) / 2;
        final int offsetY = (size - srcH) / 2;
        final int[][] out = new int[size][size];
        for (int y = 0; y < srcH && y < src.length; y++) {
            final int ny = y + offsetY;
            if (ny < 0 || ny >= size) {
                continue;
            }
            for (int x = 0; x < srcW && x < src[y].length; x++) {
                final int nx = x + offsetX;
                if (nx < 0 || nx >= size) {
                    continue;
                }
                out[ny][nx] = src[y][x];
            }
        }
        return out;
    }

    public static final class Builder {
        private final Resolution resolution;
        private final int[][] colors;

        public Builder(final Resolution resolution) {
            this.resolution = resolution;
            this.colors = new int[resolution.cells()][resolution.cells()];
        }

        public Builder set(final int x, final int y) {
            return this.set(x, y, DEFAULT_COLOR);
        }

        public Builder set(final int x, final int y, final int argb) {
            final int cells = this.resolution.cells();
            if (x >= 0 && x < cells && y >= 0 && y < cells) {
                this.colors[y][x] = argb;
            }
            return this;
        }

        public PixelGrid build() {
            return new PixelGrid(this.resolution, this.colors);
        }
}

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof final PixelGrid other)) {
            return false;
        }
        if (this.resolution != other.resolution) {
            return false;
        }
        return Arrays.deepEquals(this.colors, other.colors);
    }
}
