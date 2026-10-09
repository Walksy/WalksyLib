package main.walksy.lib.core.render.texture;

import com.mojang.blaze3d.vertex.VertexConsumer;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.element.WLElement;
import main.walksy.lib.core.render.element.WLElementKind;
import main.walksy.lib.core.render.element.WLRect;
import main.walksy.lib.core.render.element.WLVertexSink;
import main.walksy.lib.core.render.gpu.WLTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class VanillaFont {
    private VanillaFont() {
    }

    public static int width(final String text) {
        final Font font = font();
        return font == null || text == null ? 0 : font.width(text);
    }

    private static Font font() {
        final Minecraft client = Minecraft.getInstance();
        return client == null ? null : client.font;
    }

    public static void extract(final WLGraphics2D graphics, final String text, final float x, final float y, final int color, final boolean shadow) {
        final Font font = font();
        if (font == null || text == null || text.isEmpty() || (color >>> 24) == 0) {
            return;
        }
        final Matrix3x2f pose = graphics.pose();
        final Matrix4f matrix = new Matrix4f(pose.m00(), pose.m01(), 0f, 0f, pose.m10(), pose.m11(), 0f, 0f, 0f, 0f, 1f, 0f, pose.m20(), pose.m21(), 0f, 1f);
        final Capture capture = new Capture();
        font.drawInBatch(text, x, y, color, shadow, matrix, capture, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        capture.submit(graphics, graphics.scissor());
    }

    private static @Nullable Identifier location(final RenderType type) {
        if (type instanceof final RenderType.CompositeRenderType composite) {
            return composite.state.textureState.cutoutTexture().orElse(null);
        }
        return null;
    }

    private static final class Vertex {
        private float x;
        private float y;
        private float u;
        private float v;
        private int color = -1;
    }

    private static final class Capture implements MultiBufferSource {
        private final List<Collector> collectors = new ArrayList<>();

        @Override
        public VertexConsumer getBuffer(final RenderType type) {
            final Identifier location = location(type);
            final WLElementKind kind = type.getRenderPipeline() == RenderPipelines.TEXT_INTENSITY ? WLElementKind.GRAYSCALE : WLElementKind.TEXTURED;
            final Collector collector = new Collector(kind, location == null ? null : WLRenderSystem.texture(location));
            this.collectors.add(collector);
            return collector;
        }

        private void submit(final WLGraphics2D graphics, @Nullable final WLRect scissor) {
            for (final Collector collector : this.collectors) {
                if (collector.texture == null) {
                    continue;
                }
                final List<Vertex> vertices = collector.vertices;
                for (int i = 0; i + 3 < vertices.size(); i += 4) {
                    final Vertex[] quad = {vertices.get(i), vertices.get(i + 1), vertices.get(i + 2), vertices.get(i + 3)};
                    graphics.submit(new Glyph(collector.kind, collector.texture, quad, scissor, bounds(quad)));
                }
            }
        }

        private static WLRect bounds(final Vertex[] quad) {
            float minX = Float.POSITIVE_INFINITY;
            float minY = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY;
            float maxY = Float.NEGATIVE_INFINITY;
            for (final Vertex vertex : quad) {
                minX = Math.min(minX, vertex.x);
                minY = Math.min(minY, vertex.y);
                maxX = Math.max(maxX, vertex.x);
                maxY = Math.max(maxY, vertex.y);
            }
            final int left = (int) Math.floor(minX);
            final int top = (int) Math.floor(minY);
            return new WLRect(left, top, (int) Math.ceil(maxX) - left, (int) Math.ceil(maxY) - top);
        }
    }

    private static final class Collector implements VertexConsumer {
        private final WLElementKind kind;
        private final @Nullable WLTexture texture;
        private final List<Vertex> vertices = new ArrayList<>();
        private Vertex current = new Vertex();

        private Collector(final WLElementKind kind, @Nullable final WLTexture texture) {
            this.kind = kind;
            this.texture = texture;
        }

        @Override
        public VertexConsumer addVertex(final float x, final float y, final float z) {
            this.current = new Vertex();
            this.current.x = x;
            this.current.y = y;
            this.vertices.add(this.current);
            return this;
        }

        @Override
        public VertexConsumer setColor(final int red, final int green, final int blue, final int alpha) {
            this.current.color = (alpha << 24) | (red << 16) | (green << 8) | blue;
            return this;
        }

        @Override
        public VertexConsumer setUv(final float u, final float v) {
            this.current.u = u;
            this.current.v = v;
            return this;
        }

        @Override
        public VertexConsumer setUv1(final int u, final int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(final int u, final int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(final float x, final float y, final float z) {
            return this;
        }
    }

    private record Glyph(WLElementKind kind, WLTexture texture, Vertex[] vertices, @Nullable WLRect scissorArea, WLRect bounds) implements WLElement {
        @Override
        public void emit(final WLVertexSink sink) {
            for (final Vertex vertex : this.vertices) {
                sink.vertex(vertex.x, vertex.y).uv(vertex.u, vertex.v).color(vertex.color);
            }
        }
    }
}
