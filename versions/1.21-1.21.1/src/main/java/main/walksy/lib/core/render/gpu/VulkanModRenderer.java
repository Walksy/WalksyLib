package main.walksy.lib.core.render.gpu;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.vulkanmod.gl.VkGlTexture;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.util.MappedBuffer;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.SPIRVUtils;
import net.vulkanmod.vulkan.shader.converter.GLSLParser;
import net.vulkanmod.vulkan.shader.converter.Lexer;
import net.vulkanmod.vulkan.shader.descriptor.ImageDescriptor;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import net.vulkanmod.vulkan.shader.layout.Uniform;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class VulkanModRenderer {
    private static final String DEFINE = "#define WALKSYLIB_VULKANMOD";
    private static final int GL_ONE = 1;
    private static final int GL_SRC_ALPHA = 770;
    private static final int GL_ONE_MINUS_SRC_ALPHA = 771;

    private VulkanModRenderer() {
    }

    static Compiled compile(final WLPipelineSpec spec) {
        final VertexFormat format = vertexFormat(spec.format());
        final GLSLParser parser = new GLSLParser();
        parser.setVertexFormat(format);
        parser.parse(new Lexer(source(spec.vertexShader(), "vsh")), GLSLParser.Stage.VERTEX);
        parser.parse(new Lexer(source(spec.fragmentShader(), "fsh")), GLSLParser.Stage.FRAGMENT);
        final UBO[] ubos = parser.createUBOs();
        final List<ImageDescriptor> samplers = parser.getSamplerList();
        for (final ImageDescriptor sampler : samplers) {
            sampler.setImageIdx(Math.max(0, spec.samplers().indexOf(sampler.name)));
        }
        final Pipeline.Builder builder = new Pipeline.Builder(format, "walksylib/" + spec.name());
        builder.setUniforms(List.of(ubos), samplers);
        builder.setShaderSrc(SPIRVUtils.ShaderKind.VERTEX_SHADER, parser.getOutput(GLSLParser.Stage.VERTEX));
        builder.setShaderSrc(SPIRVUtils.ShaderKind.FRAGMENT_SHADER, parser.getOutput(GLSLParser.Stage.FRAGMENT));
        final GraphicsPipeline pipeline = builder.createGraphicsPipeline();
        final Map<String, ByteBuffer> staging = new HashMap<>();
        for (final UBO ubo : ubos) {
            final ByteBuffer block = MemoryUtil.memCalloc(ubo.getSize());
            for (final Uniform field : ubo.getUniforms()) {
                final MappedBuffer slice = MappedBuffer.createFromBuffer(MemoryUtil.memByteBuffer(MemoryUtil.memAddress0(block) + field.getOffset() * 4L, field.getSize() * 4));
                field.setSupplier(() -> slice);
            }
            staging.put(ubo.name, block);
        }
        return new Compiled(pipeline, staging);
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
        VRenderSystem.setPrimitiveTopologyGL(VertexFormat.Mode.QUADS.asGLMode);
        renderer.bindGraphicsPipeline(program.pipeline);
        for (int slot = 0; slot < textures.length; slot++) {
            if (textures[slot] <= 0) {
                continue;
            }
            final VkGlTexture texture = VkGlTexture.getTexture(textures[slot]);
            final VulkanImage image = texture == null ? null : texture.getVulkanImage();
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

    static int textureWidth(final int id) {
        final VkGlTexture texture = VkGlTexture.getTexture(id);
        return texture == null || texture.getVulkanImage() == null ? 0 : texture.getVulkanImage().width;
    }

    static int textureHeight(final int id) {
        final VkGlTexture texture = VkGlTexture.getTexture(id);
        return texture == null || texture.getVulkanImage() == null ? 0 : texture.getVulkanImage().height;
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
        final String text;
        try (final BufferedReader reader = Minecraft.getInstance().getResourceManager().getResourceOrThrow(location).openAsReader()) {
            text = reader.lines().collect(Collectors.joining("\n", "", "\n"));
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
        final int versionEnd = text.indexOf('\n', text.indexOf("#version")) + 1;
        return text.substring(0, versionEnd) + DEFINE + "\n" + text.substring(versionEnd);
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
