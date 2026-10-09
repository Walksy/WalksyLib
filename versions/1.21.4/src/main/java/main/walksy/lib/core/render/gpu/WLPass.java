package main.walksy.lib.core.render.gpu;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferUploader;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WLPass implements AutoCloseable {
    private static final int POSITION_STRIDE = 12;
    private static int vertexArray;
    private static int indexBuffer;
    private static int indexCapacity;
    private static int quadBuffer;
    private static ByteBuffer quadVertices;
    private final Map<String, WLUniformBuffer> blocks = new LinkedHashMap<>();
    private final List<Runnable> restores = new ArrayList<>();
    private final boolean restoreScissor;
    private final int[] savedScissor = new int[4];
    private final boolean vulkan;
    private final int[] textures = new int[12];
    private int @Nullable [] scissorBox;
    private @Nullable WLPipeline pipeline;

    public WLPass(final WLTexture target) {
        this.vulkan = VulkanBackend.active();
        if (this.vulkan) {
            this.restoreScissor = false;
            final RenderTarget renderTarget = target.target();
            if (renderTarget == null) {
                throw new IllegalStateException("walksylib render pass target has no framebuffer");
            }
            renderTarget.bindWrite(true);
            GlStateManager._disableScissorTest();
            GlStateManager._disableDepthTest();
            GlStateManager._depthMask(false);
            GlStateManager._disableCull();
            GlStateManager._colorMask(true, true, true, true);
            return;
        }
        this.restoreScissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        if (this.restoreScissor) {
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, this.savedScissor);
        }
        final RenderTarget renderTarget = target.target();
        if (renderTarget == null) {
            throw new IllegalStateException("walksylib render pass target has no framebuffer");
        }
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, renderTarget.frameBufferId);
        GlStateManager._viewport(0, 0, renderTarget.width, renderTarget.height);
        GlStateManager._disableScissorTest();
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);
        GlStateManager._disableCull();
        GlStateManager._colorMask(true, true, true, true);
    }

    public WLPass pipeline(final WLPipeline pipeline) {
        this.pipeline = pipeline;
        if (!this.vulkan) {
            GlStateManager._glUseProgram(pipeline.program());
        }
        switch (pipeline.spec().blend()) {
            case OPAQUE -> GlStateManager._disableBlend();
            case TRANSLUCENT -> {
                GlStateManager._enableBlend();
                GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            }
            case PREMULTIPLIED -> {
                GlStateManager._enableBlend();
                GlStateManager._blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            }
        }
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
        final int unit = this.pipeline == null ? -1 : this.pipeline.samplerUnit(name);
        if (unit < 0) {
            return this;
        }
        if (this.vulkan) {
            if (unit < this.textures.length) {
                this.textures[unit] = texture.glId();
            }
            return this;
        }
        GlStateManager._activeTexture(GL13.GL_TEXTURE0 + unit);
        GlStateManager._bindTexture(texture.glId());
        final WLFilter effective = filter != null ? filter : texture.filter();
        if (effective != null) {
            final int textureId = texture.glId();
            final int minFilter = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER);
            final int magFilter = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER);
            final int wrapS = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S);
            final int wrapT = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T);
            this.restores.add(() -> {
                GlStateManager._bindTexture(textureId);
                GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, minFilter);
                GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, magFilter);
                GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrapS);
                GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrapT);
            });
            final int mode = effective == WLFilter.LINEAR ? GL11.GL_LINEAR : GL11.GL_NEAREST;
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, mode);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, mode);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        }
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        return this;
    }

    public WLPass scissor(final int x, final int y, final int width, final int height) {
        if (this.vulkan) {
            this.scissorBox = new int[] { x, y, width, height };
            return this;
        }
        GlStateManager._enableScissorTest();
        GlStateManager._scissorBox(x, y, width, height);
        return this;
    }

    public WLPass noScissor() {
        if (this.vulkan) {
            this.scissorBox = null;
            return this;
        }
        GlStateManager._disableScissorTest();
        return this;
    }

    public void drawQuads(final WLVertexBuffer vertices, final int byteOffset, final int byteLength, final int vertexCount) {
        if (vertexCount <= 0 || this.pipeline == null) {
            return;
        }
        if (this.vulkan) {
            VulkanBackend.draw(this.pipeline.vulkan(), this.blocks, this.textures, MemoryUtil.memByteBuffer(MemoryUtil.memAddress0(vertices.staging()) + byteOffset, byteLength), this.pipeline.spec().format(), vertexCount, this.pipeline.spec().blend(), this.scissorBox);
            return;
        }
        final int stride = byteLength / vertexCount;
        final int indexCount = vertexCount / 4 * 6;
        this.bindBlocks();
        GlStateManager._glBindVertexArray(vertexArray());
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, vertices.glBuffer());
        attributes(this.pipeline.spec().format(), stride, byteOffset);
        bindIndices(indexCount);
        GL11.glDrawElements(GL11.GL_TRIANGLES, indexCount, GL11.GL_UNSIGNED_INT, 0L);
    }

    public void drawFullscreenQuad() {
        if (this.vulkan) {
            if (this.pipeline != null) {
                VulkanBackend.draw(this.pipeline.vulkan(), this.blocks, this.textures, quadVertices(), WLVertexFormat.POSITION, 4, this.pipeline.spec().blend(), this.scissorBox);
            }
            return;
        }
        this.bindBlocks();
        GlStateManager._glBindVertexArray(vertexArray());
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, quadBuffer());
        attributes(WLVertexFormat.POSITION, POSITION_STRIDE, 0L);
        bindIndices(6);
        GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0L);
    }

    private void bindBlocks() {
        for (final Map.Entry<String, WLUniformBuffer> entry : this.blocks.entrySet()) {
            WLUniformBlocks.bind(entry.getKey(), entry.getValue().glBuffer());
        }
    }

    private static void attributes(final WLVertexFormat format, final int stride, final long offset) {
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, offset);
        long cursor = offset + POSITION_STRIDE;
        if (format.hasUv()) {
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, cursor);
            cursor += 8;
        } else {
            GL20.glDisableVertexAttribArray(1);
        }
        if (format.hasColor()) {
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 4, GL11.GL_UNSIGNED_BYTE, true, stride, cursor);
        } else {
            GL20.glDisableVertexAttribArray(2);
        }
    }

    private static int vertexArray() {
        if (vertexArray == 0) {
            vertexArray = GlStateManager._glGenVertexArrays();
        }
        return vertexArray;
    }

    private static void bindIndices(final int indexCount) {
        if (indexBuffer == 0) {
            indexBuffer = GlStateManager._glGenBuffers();
        }
        GlStateManager._glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
        if (indexCapacity >= indexCount) {
            return;
        }
        final int quads = Math.max(indexCount, indexCapacity * 2) / 6 + 1;
        final IntBuffer indices = MemoryUtil.memAllocInt(quads * 6);
        try {
            for (int quad = 0; quad < quads; quad++) {
                final int base = quad * 4;
                indices.put(base).put(base + 1).put(base + 2).put(base + 2).put(base + 3).put(base);
            }
            indices.flip();
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);
        } finally {
            MemoryUtil.memFree(indices);
        }
        indexCapacity = quads * 6;
    }

    private static ByteBuffer quadVertices() {
        if (quadVertices == null) {
            quadVertices = MemoryUtil.memAlloc(4 * 3 * 4);
            quadVertices.asFloatBuffer().put(new float[] { -1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f });
        }
        return quadVertices;
    }

    private static int quadBuffer() {
        if (quadBuffer == 0) {
            final ByteBuffer bytes = MemoryUtil.memAlloc(4 * 3 * 4);
            try {
                final FloatBuffer floats = bytes.asFloatBuffer();
                floats.put(new float[] { -1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f });
                bytes.rewind();
                quadBuffer = GlStateManager._glGenBuffers();
                GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, quadBuffer);
                GlStateManager._glBufferData(GL15.GL_ARRAY_BUFFER, bytes, GL15.GL_STATIC_DRAW);
            } finally {
                MemoryUtil.memFree(bytes);
            }
        }
        return quadBuffer;
    }

    @Override
    public void close() {
        if (this.vulkan) {
            GlStateManager._disableBlend();
            GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            GlStateManager._depthMask(true);
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
            VulkanBackend.restoreScissor();
            return;
        }
        GlStateManager._glBindVertexArray(0);
        BufferUploader.invalidate();
        GlStateManager._glUseProgram(0);
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        for (int i = this.restores.size() - 1; i >= 0; i--) {
            this.restores.get(i).run();
        }
        this.restores.clear();
        GlStateManager._disableBlend();
        GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager._depthMask(true);
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        if (this.restoreScissor) {
            GlStateManager._enableScissorTest();
            GlStateManager._scissorBox(this.savedScissor[0], this.savedScissor[1], this.savedScissor[2], this.savedScissor[3]);
        } else {
            GlStateManager._disableScissorTest();
        }
    }
}
