package main.walksy.lib.core.render.element;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import main.walksy.lib.core.render.GuiContext;
import main.walksy.lib.core.render.gpu.WLTexture;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.jetbrains.annotations.Nullable;

public final class VanillaElement implements WLElement {
    private final GuiElementRenderState state;
    private final WLElementKind kind;
    private final @Nullable WLTexture texture;
    private final @Nullable WLRect scissor;
    private final @Nullable WLRect bounds;

    public VanillaElement(final GuiElementRenderState state) {
        this.state = state;
        final TextureSetup setup = state.textureSetup();
        this.texture = setup.texure0() == null ? null : WLTexture.wrap(setup.texure0(), setup.sampler0());
        final RenderPipeline pipeline = state.pipeline();
        this.kind = this.texture == null ? WLElementKind.FLAT : pipeline == RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA ? WLElementKind.PREMULTIPLIED : pipeline == RenderPipelines.GUI_TEXT_INTENSITY ? WLElementKind.GRAYSCALE : WLElementKind.TEXTURED;
        this.scissor = GuiContext.rect(state.scissorArea());
        this.bounds = GuiContext.rect(state.bounds());
    }

    public GuiElementRenderState state() {
        return this.state;
    }

    @Override
    public WLElementKind kind() {
        return this.kind;
    }

    @Override
    public @Nullable WLTexture texture() {
        return this.texture;
    }

    @Override
    public @Nullable WLRect scissorArea() {
        return this.scissor;
    }

    @Override
    public @Nullable WLRect bounds() {
        return this.bounds;
    }

    @Override
    public void emit(final WLVertexSink sink) {
        this.state.buildVertices(new SinkConsumer(sink));
    }

    private record SinkConsumer(WLVertexSink sink) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(final float x, final float y, final float z) {
            this.sink.vertex(x, y);
            return this;
        }

        @Override
        public VertexConsumer setColor(final int r, final int g, final int b, final int a) {
            this.sink.color((a << 24) | (r << 16) | (g << 8) | b);
            return this;
        }

        @Override
        public VertexConsumer setColor(final int argb) {
            this.sink.color(argb);
            return this;
        }

        @Override
        public VertexConsumer setUv(final float u, final float v) {
            this.sink.uv(u, v);
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

        @Override
        public VertexConsumer setLineWidth(final float width) {
            return this;
        }
    }
}
