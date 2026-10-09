package main.walksy.lib.core.render.gpu;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.descriptor.ImageDescriptor;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import net.vulkanmod.vulkan.shader.layout.AlignedStruct;
import net.vulkanmod.vulkan.shader.layout.Uniform;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.lwjgl.system.MemoryUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class VulkanModRenderer {
    private static final String DEFINE = "#define WALKSYLIB_VULKANMOD";
    private static final int GL_ONE = 1;
    private static final int GL_TRIANGLES = 4;
    private static final int GL_SRC_ALPHA = 770;
    private static final int GL_ONE_MINUS_SRC_ALPHA = 771;

    private VulkanModRenderer() {
    }

    static Compiled compile(final WLPipelineSpec spec) {
        final VertexFormat format = vertexFormat(spec.format());
        final VulkanShaderSource source = VulkanShaderSource.convert(source(spec.vertexShader(), "vsh"), source(spec.fragmentShader(), "fsh"), format.getElementAttributeNames(), DEFINE);
        final List<UBO> ubos = new ArrayList<>();
        final Map<String, ByteBuffer> staging = new HashMap<>();
        for (final Map.Entry<String, VulkanShaderSource.Block> entry : source.blocks().entrySet()) {
            final AlignedStruct.Builder struct = new AlignedStruct.Builder();
            for (final String[] field : entry.getValue().fields()) {
                struct.addUniformInfo(field[0], field[1]);
            }
            final UBO ubo = struct.buildUBO(entry.getValue().binding(), Integer.MAX_VALUE);
            final ByteBuffer block = MemoryUtil.memCalloc(Math.max(16, ubo.getSize()));
            for (final Uniform field : ubo.getUniforms()) {
                final MappedBuffer slice = MappedBuffer.createFromBuffer(MemoryUtil.memByteBuffer(MemoryUtil.memAddress0(block) + field.getOffset() * 4L, field.getSize() * 4));
                field.setSupplier(() -> slice);
            }
            ubos.add(ubo);
            staging.put(entry.getKey(), block);
        }
        final List<ImageDescriptor> samplers = new ArrayList<>();
        for (final Map.Entry<String, Integer> entry : source.samplers().entrySet()) {
            samplers.add(new ImageDescriptor(entry.getValue(), "sampler2D", entry.getKey(), Math.max(0, spec.samplers().indexOf(entry.getKey()))));
        }
        final Pipeline.Builder builder = new Pipeline.Builder(format, "walksylib/" + spec.name());
        builder.setUniforms(ubos, samplers);
        builder.compileShaders("walksylib/" + spec.name(), source.vertex(), source.fragment());
        return new Compiled(builder.createGraphicsPipeline(), staging);
    }

    static void draw(final Compiled program, final Map<String, WLUniformBuffer> blocks, final int[] textures, final ByteBuffer vertices, final WLVertexFormat format, final int vertexCount, final WLBlend blend, final int[] scissor) {
        for (final Map.Entry<String, WLUniformBuffer> entry : blocks.entrySet()) {
            final ByteBuffer block = program.staging.get(entry.getKey());
            if (block != null) {
                final ByteBuffer data = entry.getValue().data();
                MemoryUtil.memCopy(MemoryUtil.memAddress0(data), MemoryUtil.memAddress0(block), Math.min(data.capacity(), block.capacity()));
            }
        }
        applyState(blend, scissor);
        final Renderer renderer = Renderer.getInstance();
        VRenderSystem.setPrimitiveTopologyGL(GL_TRIANGLES);
        renderer.bindGraphicsPipeline(program.pipeline);
        for (int slot = 0; slot < textures.length; slot++) {
            if (textures[slot] <= 0) {
                continue;
            }
            final VulkanImage image = VulkanModTextures.image(textures[slot]);
            if (image != null) {
                VTextureSelector.bindTexture(slot, image);
            }
        }
        renderer.uploadAndBindUBOs(program.pipeline);
        Renderer.getDrawer().draw(vertices, VertexFormat.Mode.QUADS, vertexFormat(format), vertexCount);
        restoreScissor();
    }

    static void restoreScissor() {
        final int[] box = VanillaScissor.box();
        if (box == null) {
            Renderer.resetScissor();
        } else {
            Renderer.setScissor(box[0], box[1], box[2], box[3]);
        }
    }

    static void makeReadable(final int id) {
        final VulkanImage image = VulkanModTextures.image(id);
        if (image == null) {
            return;
        }
        Renderer.getInstance().endRenderPass();
        image.readOnlyLayout();
    }

    static int textureWidth(final int id) {
        final VulkanImage image = VulkanModTextures.image(id);
        return image == null ? 0 : image.width;
    }

    static int textureHeight(final int id) {
        final VulkanImage image = VulkanModTextures.image(id);
        return image == null ? 0 : image.height;
    }

    private static void applyState(final WLBlend blend, final int[] scissor) {
        VRenderSystem.disableDepthTest();
        VRenderSystem.depthMask(false);
        VRenderSystem.disableCull();
        VRenderSystem.colorMask(true, true, true, true);
        switch (blend) {
            case OPAQUE -> VRenderSystem.disableBlend();
            case TRANSLUCENT -> {
                VRenderSystem.enableBlend();
                VRenderSystem.blendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
            }
            case PREMULTIPLIED -> {
                VRenderSystem.enableBlend();
                VRenderSystem.blendFuncSeparate(GL_ONE, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
            }
        }
        if (scissor == null) {
            Renderer.resetScissor();
        } else {
            Renderer.setScissor(scissor[0], scissor[1], scissor[2], scissor[3]);
        }
    }

    private static VertexFormat vertexFormat(final WLVertexFormat format) {
        return switch (format) {
            case POSITION -> DefaultVertexFormat.POSITION;
            case POSITION_COLOR -> DefaultVertexFormat.POSITION_COLOR;
            case POSITION_TEX_COLOR -> DefaultVertexFormat.POSITION_TEX_COLOR;
        };
    }

    private static String source(final String path, final String extension) {
        final Identifier location = Identifier.fromNamespaceAndPath("walksylib", "shaders/" + path + "." + extension);
        try (final BufferedReader reader = Minecraft.getInstance().getResourceManager().getResourceOrThrow(location).openAsReader()) {
            return reader.lines().collect(Collectors.joining("\n", "", "\n"));
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    static final class Compiled {
        private final GraphicsPipeline pipeline;
        private final Map<String, ByteBuffer> staging;

        private Compiled(final GraphicsPipeline pipeline, final Map<String, ByteBuffer> staging) {
            this.pipeline = pipeline;
            this.staging = staging;
        }
    }
}
