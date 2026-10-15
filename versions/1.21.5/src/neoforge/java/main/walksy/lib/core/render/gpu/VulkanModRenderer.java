package main.walksy.lib.core.render.gpu;

import java.nio.ByteBuffer;
import java.util.Map;

final class VulkanModRenderer {
    private VulkanModRenderer() {
    }

    static Compiled compile(final WLPipelineSpec spec) {
        throw new UnsupportedOperationException("VulkanMod is not available on NeoForge");
    }

    static void draw(final Compiled program, final Map<String, WLUniformBuffer> blocks, final int[] textures, final ByteBuffer vertices, final WLVertexFormat format, final int vertexCount, final WLBlend blend, final int[] scissor) {
        throw new UnsupportedOperationException("VulkanMod is not available on NeoForge");
    }

    static void restoreScissor() {
    }

    static void makeReadable(final int id) {
    }

    static int textureWidth(final int id) {
        return 0;
    }

    static int textureHeight(final int id) {
        return 0;
    }

    static final class Compiled {
        private Compiled() {
        }
    }
}
