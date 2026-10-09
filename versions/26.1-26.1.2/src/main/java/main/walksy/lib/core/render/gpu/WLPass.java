package main.walksy.lib.core.render.gpu;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.Nullable;

public final class WLPass implements AutoCloseable {
    private final RenderPass pass;

    public WLPass(final RenderPass pass) {
        this.pass = pass;
    }

    public WLPass pipeline(final WLPipeline pipeline) {
        this.pass.setPipeline(pipeline.pipeline());
        return this;
    }

    public WLPass uniform(final String name, final WLUniformBuffer buffer) {
        this.pass.setUniform(name, buffer.buffer());
        return this;
    }

    public WLPass transforms(final WLTransform transform) {
        this.pass.setUniform("DynamicTransforms", transform.slice());
        return this;
    }

    public WLPass defaultUniforms() {
        RenderSystem.bindDefaultUniforms(this.pass);
        return this;
    }

    public WLPass texture(final String name, final WLTexture texture, @Nullable final WLFilter filter) {
        final GpuSampler sampler = filter != null ? WLGpu.sampler(filter) : texture.sampler() != null ? texture.sampler() : WLGpu.sampler(WLFilter.LINEAR);
        this.pass.bindTexture(name, texture.view(), sampler);
        return this;
    }

    public WLPass scissor(final int x, final int y, final int width, final int height) {
        this.pass.enableScissor(x, y, width, height);
        return this;
    }

    public WLPass noScissor() {
        this.pass.disableScissor();
        return this;
    }

    public void drawQuads(final WLVertexBuffer vertices, final int byteOffset, final int byteLength, final int vertexCount) {
        if (vertexCount <= 0) {
            return;
        }
        final int stride = byteLength / vertexCount;
        final int indexCount = vertexCount / 4 * 6;
        final RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        this.pass.setVertexBuffer(0, vertices.buffer());
        this.pass.setIndexBuffer(indices.getBuffer(indexCount), indices.type());
        this.pass.drawIndexed(byteOffset / stride, 0, indexCount, 1);
    }

    public void drawFullscreenQuad() {
        final RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        this.pass.setVertexBuffer(0, WLGpu.quadVertices());
        this.pass.setIndexBuffer(indices.getBuffer(6), indices.type());
        this.pass.drawIndexed(0, 0, 6, 1);
    }

    @Override
    public void close() {
        this.pass.close();
    }
}
