package main.walksy.lib.core.render.gpu;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.ByteBuffer;
import java.util.Map;

final class VulkanBackend {
    private static final boolean ACTIVE = FabricLoader.getInstance().isModLoaded("vulkanmod");

    private VulkanBackend() {
    }

    static boolean active() {
        return ACTIVE;
    }

    static Program compile(final WLPipelineSpec spec) {
        return new Program(VulkanModRenderer.compile(spec));
    }

    static void draw(final Program program, final Map<String, WLUniformBuffer> blocks, final int[] textures, final ByteBuffer vertices, final WLVertexFormat format, final int vertexCount, final WLBlend blend, final int[] scissor) {
        VulkanModRenderer.draw((VulkanModRenderer.Compiled) program.compiled, blocks, textures, vertices, format, vertexCount, blend, scissor);
    }

    static void clear(final RenderTarget target) {
        target.setClearColor(0f, 0f, 0f, 0f);
        target.clear();
    }

    static void restoreScissor() {
        VulkanModRenderer.restoreScissor();
    }

    static int textureWidth(final int id) {
        return VulkanModRenderer.textureWidth(id);
    }

    static int textureHeight(final int id) {
        return VulkanModRenderer.textureHeight(id);
    }

    static final class Program {
        private final Object compiled;

        private Program(final Object compiled) {
            this.compiled = compiled;
        }
    }
}
