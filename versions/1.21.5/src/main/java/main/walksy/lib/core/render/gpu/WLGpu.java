package main.walksy.lib.core.render.gpu;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlRenderPipeline;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Supplier;

public final class WLGpu {
    private static final boolean VULKANMOD = FabricLoader.getInstance().isModLoaded("vulkanmod");
    private static final Map<String, WLPipeline> PIPELINES = new HashMap<>();
    private static final BlendFunction PREMULTIPLIED = new BlendFunction(SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
    private static final List<WLUniformBuffer> TRANSFORMS = new ArrayList<>();
    private static int transformCursor;
    private static WLUniformBuffer defaultProjection;
    private static GpuBuffer quadVertices;

    private WLGpu() {
    }

    public static synchronized WLPipeline pipeline(final WLPipelineSpec spec) {
        final WLPipeline cached = PIPELINES.get(spec.name());
        if (cached != null) {
            return cached;
        }
        final RenderPipeline.Builder builder = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("walksylib", "pipeline/" + spec.name()))
            .withVertexShader(Identifier.fromNamespaceAndPath("walksylib", spec.vertexShader()))
            .withFragmentShader(Identifier.fromNamespaceAndPath("walksylib", spec.fragmentShader()));
        for (final String sampler : spec.samplers()) {
            builder.withSampler(sampler);
        }
        if (VULKANMOD) {
            builder.withShaderDefine("WALKSYLIB_VULKANMOD");
        }
        builder.withVertexFormat(vertexFormat(spec.format()), VertexFormat.Mode.QUADS);
        builder.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST);
        builder.withDepthWrite(false);
        switch (spec.blend()) {
            case OPAQUE -> builder.withoutBlend();
            case TRANSLUCENT -> builder.withBlend(BlendFunction.TRANSLUCENT);
            case PREMULTIPLIED -> builder.withBlend(PREMULTIPLIED);
        }
        final WLPipeline pipeline = new WLPipeline(builder.build(), spec);
        program(pipeline);
        PIPELINES.put(spec.name(), pipeline);
        return pipeline;
    }

    public static void beginFrame() {
        transformCursor = 0;
    }

    public static WLUniformBuffer uniformBuffer(final String label, final int size) {
        return new WLUniformBuffer(label, size);
    }

    public static WLVertexBuffer vertexBuffer(final String label) {
        return new WLVertexBuffer(label);
    }

    public static WLTexture colorTexture(final String label, final int width, final int height) {
        final GpuTexture texture = RenderSystem.getDevice().createTexture(label, TextureFormat.RGBA8, width, height, 1);
        return new WLTexture(texture, null, true);
    }

    public static WLTexture texture(final AbstractTexture texture) {
        return WLTexture.wrap(texture.getTexture());
    }

    public static WLTexture texture(final Identifier id) {
        return texture(Minecraft.getInstance().getTextureManager().getTexture(id));
    }

    public static WLTexture withFilter(final WLTexture texture, final WLFilter filter) {
        return new WLTexture(texture.gpuTexture(), filter, false);
    }

    public static void finishOffscreen(final GpuTexture texture) {
        if (VulkanBackend.active() && texture instanceof final GlTexture gl) {
            VulkanBackend.makeReadable(gl.glId());
        }
    }

    public static boolean renderTargetsBottomUp() {
        return !VulkanBackend.active();
    }

    public static WLTexture mainTarget() {
        return new WLTexture(Minecraft.getInstance().getMainRenderTarget().getColorTexture(), null, false);
    }

    public static int targetWidth() {
        return Minecraft.getInstance().getMainRenderTarget().width;
    }

    public static int targetHeight() {
        return Minecraft.getInstance().getMainRenderTarget().height;
    }

    public static void clear(final WLTexture texture) {
        if (VulkanBackend.active()) {
            RenderSystem.getDevice().createCommandEncoder().createRenderPass(texture.gpuTexture(), OptionalInt.of(0)).close();
            return;
        }
        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(texture.gpuTexture(), 0);
    }

    public static void copyMainTo(final WLTexture destination) {
        final RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        RenderSystem.getDevice()
            .createCommandEncoder()
            .copyTextureToTexture(target.getColorTexture(), destination.gpuTexture(), 0, 0, 0, 0, 0, target.width, target.height);
    }

    public static void copyToMain(final WLTexture source) {
        final RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        RenderSystem.getDevice()
            .createCommandEncoder()
            .copyTextureToTexture(source.gpuTexture(), target.getColorTexture(), 0, 0, 0, 0, 0, target.width, target.height);
    }

    public static void clearMainDepth() {
        final RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        if (target.getDepthTexture() != null) {
            RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(target.getDepthTexture(), 1.0D);
        }
    }

    public static WLTransform transform(final Matrix4f modelView) {
        if (transformCursor == TRANSFORMS.size()) {
            TRANSFORMS.add(new WLUniformBuffer("walksylib DynamicTransforms " + transformCursor, 64));
        }
        final WLUniformBuffer buffer = TRANSFORMS.get(transformCursor);
        transformCursor++;
        try (final WLMapping map = buffer.map()) {
            map.writer().putMat4f(modelView);
        }
        return new WLTransform(buffer);
    }

    public static WLPass pass(final Supplier<String> label, final WLTexture target) {
        RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS).getBuffer(6);
        quadVertices();
        return new WLPass(RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(target.gpuTexture(), OptionalInt.empty()));
    }

    static void program(final WLPipeline pipeline) {
        if (RenderSystem.getDevice() instanceof final GlDevice device) {
            final GlRenderPipeline compiled = device.getOrCompilePipeline(pipeline.pipeline());
            if (compiled.isValid()) {
                WLUniformBlocks.configure(compiled.program(), pipeline.uniformBlocks());
            }
        }
    }

    static WLUniformBuffer defaultProjection() {
        if (defaultProjection == null) {
            defaultProjection = new WLUniformBuffer("walksylib default projection", 64);
        }
        try (final WLMapping map = defaultProjection.map()) {
            map.writer().putMat4f(RenderSystem.getProjectionMatrix());
        }
        return defaultProjection;
    }

    static Runnable applyFilter(final WLTexture texture, final WLFilter filter) {
        final GpuTexture gpuTexture = texture.gpuTexture();
        final FilterMode minFilter = gpuTexture.minFilter;
        final FilterMode magFilter = gpuTexture.magFilter;
        final boolean useMipmaps = gpuTexture.useMipmaps;
        final AddressMode addressModeU = gpuTexture.addressModeU;
        final AddressMode addressModeV = gpuTexture.addressModeV;
        gpuTexture.setTextureFilter(filter == WLFilter.LINEAR ? FilterMode.LINEAR : FilterMode.NEAREST, false);
        gpuTexture.setAddressMode(AddressMode.CLAMP_TO_EDGE);
        return () -> {
            if (!gpuTexture.isClosed()) {
                gpuTexture.setTextureFilter(minFilter, magFilter, useMipmaps);
                gpuTexture.setAddressMode(addressModeU, addressModeV);
            }
        };
    }

    static synchronized GpuBuffer quadVertices() {
        if (quadVertices == null) {
            try (final MemoryStack stack = MemoryStack.stackPush()) {
                final ByteBuffer bytes = stack.malloc(4 * 3 * 4);
                final FloatBuffer floats = bytes.asFloatBuffer();
                floats.put(new float[] { -1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f });
                bytes.rewind();
                quadVertices = RenderSystem.getDevice().createBuffer(() -> "walksylib_quad_vbo", BufferType.VERTICES, BufferUsage.STATIC_WRITE, bytes);
            }
        }
        return quadVertices;
    }

    private static VertexFormat vertexFormat(final WLVertexFormat format) {
        return switch (format) {
            case POSITION -> DefaultVertexFormat.POSITION;
            case POSITION_COLOR -> DefaultVertexFormat.POSITION_COLOR;
            case POSITION_TEX_COLOR -> DefaultVertexFormat.POSITION_TEX_COLOR;
        };
    }
}
