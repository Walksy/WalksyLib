package main.walksy.lib.core.render;

import main.walksy.lib.api.config.value.PixelGrid;
import main.walksy.lib.core.render.element.WLElement;
import main.walksy.lib.core.render.element.WLElementKind;
import main.walksy.lib.core.render.element.WLRect;
import main.walksy.lib.core.render.element.WLVertexSink;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

public final class PixelGridRenderer {
    private PixelGridRenderer() {
    }

    public static void render(final WLGraphics2D graphics, final PixelGrid grid, final float x, final float y, final int pixelSize, final int gapSize, final boolean blend) {
        final WLElementKind kind = blend ? WLElementKind.INVERT : WLElementKind.FLAT;
        final Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        final WLRect scissor = graphics.scissor();
        for (int gy = 0; gy < grid.getHeight(); gy++) {
            for (int gx = 0; gx < grid.getWidth(); gx++) {
                final int color = grid.getColor(gx, gy);
                if (color == 0) {
                    continue;
                }
                final float px = x + gx * (pixelSize + gapSize);
                final float py = y + gy * (pixelSize + gapSize);
                add(graphics, new CellQuad(kind, pose, px, py, px + pixelSize, py + pixelSize, color, scissor));
            }
        }
    }

    private static void add(final WLGraphics2D graphics, final WLElement element) {
        if (WLRenderSystem.contentStratum().isTopActive()) {
            WLRenderSystem.contentStratum().addTop(element);
        } else if (WLRenderSystem.contentStratum().isPopoverActive()) {
            WLRenderSystem.contentStratum().addPopover(element);
        } else if (WLRenderSystem.contentStratum().isActive()) {
            WLRenderSystem.contentStratum().add(element);
        } else {
            graphics.context().submit(element);
        }
    }

    private record CellQuad(WLElementKind kind, Matrix3x2f pose, float x0, float y0, float x1, float y1, int col, @Nullable WLRect scissorArea, @Nullable WLRect bounds) implements WLElement {

        CellQuad(final WLElementKind kind, final Matrix3x2f pose, final float x0, final float y0, final float x1, final float y1, final int col, final @Nullable WLRect scissorArea) {
            this(kind, pose, x0, y0, x1, y1, col, scissorArea, boundsOf(x0, y0, x1, y1, pose, scissorArea));
        }

        private static @Nullable WLRect boundsOf(final float x0, final float y0, final float x1, final float y1, final Matrix3x2f pose, final @Nullable WLRect scissor) {
            final WLRect rect = new WLRect((int) x0, (int) y0, (int) (x1 - x0), (int) (y1 - y0)).transformMaxBounds(pose);
            return WLRect.clipped(rect, scissor);
        }

        @Override
        public void emit(final WLVertexSink sink) {
            sink.posed(this.pose, this.x0, this.y0).color(this.col);
            sink.posed(this.pose, this.x0, this.y1).color(this.col);
            sink.posed(this.pose, this.x1, this.y1).color(this.col);
            sink.posed(this.pose, this.x1, this.y0).color(this.col);
        }
    }
}
