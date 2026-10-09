package main.walksy.lib.core.render.gpu;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WLPass implements AutoCloseable {
    private final RenderPass pass;
    private final Map<String, WLUniformBuffer> blocks = new LinkedHashMap<>();
    private final List<Runnable> restores = new ArrayList<>();
    private final boolean vulkan = VulkanBackend.active();
    private final int[] textures = new int[12];
    private int @Nullable [] scissorBox;
    private @Nullable WLPipeline current;
    private static ByteBuffer quadVertices;

    public WLPass(final RenderPass pass) {
        this.pass = pass;
    }

    public WLPass pipeline(final WLPipeline pipeline) {
        this.current = pipeline;
        if (this.vulkan) {
            return this;
        }
        WLGpu.program(pipeline);
        this.pass.setPipeline(pipeline.pipeline());
        return this;
    }

    public WLPass uniform(final String name, final WLUniformBuffer buffer) {
        this.blocks.put(name, buffer);
        return this;
    }

    public WLPass transforms(final WLTransform transform) {
        this.blocks.put("DynamicTransforms", transform.buffer());
        return this;
    }

    public WLPass defaultUniforms() {
        this.blocks.put("Projection", WLGpu.defaultProjection());
        return this;
    }

    public WLPass texture(final String name, final WLTexture texture, @Nullable final WLFilter filter) {
        final WLFilter effective = filter != null ? filter : texture.filter();
        if (effective != null) {
            this.restores.add(WLGpu.applyFilter(texture, effective));
        }
        if (this.vulkan) {
            final int unit = this.current == null ? -1 : this.current.spec().samplers().indexOf(name);
            if (unit >= 0 && unit < this.textures.length && texture.gpuTexture() instanceof final GlTexture gl) {
                gl.flushModeChanges();
                this.textures[unit] = gl.glId();
            }
            return this;
        }
        this.pass.bindSampler(name, texture.gpuTexture());
        return this;
    }

    public WLPass scissor(final int x, final int y, final int width, final int height) {
        if (this.vulkan) {
            this.scissorBox = new int[] { x, y, width, height };
            return this;
        }
        this.pass.enableScissor(x, y, width, height);
        return this;
    }

    public WLPass noScissor() {
        if (this.vulkan) {
            this.scissorBox = null;
            return this;
        }
        this.pass.disableScissor();
        return this;
    }

    public void drawQuads(final WLVertexBuffer vertices, final int byteOffset, final int byteLength, final int vertexCount) {
        if (vertexCount <= 0) {
            return;
        }
        if (this.vulkan) {
            if (this.current != null) {
                VulkanBackend.draw(this.current.vulkan(), this.blocks, this.textures, MemoryUtil.memByteBuffer(MemoryUtil.memAddress0(vertices.staging()) + byteOffset, byteLength), this.current.spec().format(), vertexCount, this.current.spec().blend(), this.scissorBox);
            }
            return;
        }
        final int stride = byteLength / vertexCount;
        final int firstIndex = byteOffset / stride / 4 * 6;
        final int indexCount = vertexCount / 4 * 6;
        final RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        this.pass.setVertexBuffer(0, vertices.buffer());
        this.pass.setIndexBuffer(indices.getBuffer(firstIndex + indexCount), indices.type());
        this.bindBlocks();
        this.pass.drawIndexed(firstIndex, indexCount);
    }

    public void drawFullscreenQuad() {
        if (this.vulkan) {
            if (this.current != null) {
                VulkanBackend.draw(this.current.vulkan(), this.blocks, this.textures, quadVertices(), WLVertexFormat.POSITION, 4, this.current.spec().blend(), this.scissorBox);
            }
            return;
        }
        final RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        this.pass.setVertexBuffer(0, WLGpu.quadVertices());
        this.pass.setIndexBuffer(indices.getBuffer(6), indices.type());
        this.bindBlocks();
        this.pass.drawIndexed(0, 6);
    }

    private static ByteBuffer quadVertices() {
        if (quadVertices == null) {
            quadVertices = MemoryUtil.memAlloc(4 * 3 * 4);
            quadVertices.asFloatBuffer().put(new float[] { -1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f });
        }
        return quadVertices;
    }

    private void bindBlocks() {
        for (final Map.Entry<String, WLUniformBuffer> entry : this.blocks.entrySet()) {
            WLUniformBlocks.bind(entry.getKey(), entry.getValue().glBuffer());
        }
    }

    @Override
    public void close() {
        this.pass.close();
        if (this.vulkan) {
            VulkanBackend.restoreScissor();
        }
        for (int i = this.restores.size() - 1; i >= 0; i--) {
            this.restores.get(i).run();
        }
        this.restores.clear();
    }
}
