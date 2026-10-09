package main.walksy.lib.core.render.gpu;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class WLGpu {
    private static final Map<String, WLPipeline> PIPELINES = new HashMap<>();
    private static final Vector4f TRANSPARENT = new Vector4f(0f, 0f, 0f, 0f);
    private static GpuBuffer quadVertices;

    private WLGpu() {
    }

    public static synchronized WLPipeline pipeline(final WLPipelineSpec spec) {
        final WLPipeline cached = PIPELINES.get(spec.name());
        if (cached != null) {
            return cached;
        }
        final BindGroupLayout.Builder layout = BindGroupLayout.builder();
        for (final String uniform : spec.uniforms()) {
            layout.withUniform(uniform, UniformType.UNIFORM_BUFFER);
        }
        for (final String sampler : spec.samplers()) {
            layout.withUniform(sampler, UniformType.COMBINED_IMAGE_SAMPLER);
        }
        final RenderPipeline built = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("walksylib", "pipeline/" + spec.name()))
            .withVertexShader(Identifier.fromNamespaceAndPath("walksylib", spec.vertexShader()))
            .withFragmentShader(Identifier.fromNamespaceAndPath("walksylib", spec.fragmentShader()))
            .withBindGroupLayout(layout.build())
            .withVertexBinding(0, vertexFormat(spec.format()))
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withColorTargetState(colorTarget(spec.blend()))
            .build();
        RenderSystem.getCompiledPipeline(built);
        final WLPipeline pipeline = new WLPipeline(built);
        PIPELINES.put(spec.name(), pipeline);
        return pipeline;
    }

    public static WLUniformBuffer uniformBuffer(final String label, final int size) {
        return new WLUniformBuffer(label, size);
    }

    public static WLVertexBuffer vertexBuffer(final String label) {
        return new WLVertexBuffer(label);
    }

    public static WLTexture colorTexture(final String label, final int width, final int height) {
        final GpuTexture texture = RenderSystem.getDevice().createTexture(label, GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT, GpuFormat.RGBA8_UNORM, width, height, 1, 1);
        return new WLTexture(texture, RenderSystem.getDevice().createTextureView(texture), null, true);
    }

    public static WLTexture texture(final AbstractTexture texture) {
        return WLTexture.wrap(texture.getTextureView(), texture.getSampler());
    }

    public static WLTexture texture(final Identifier id) {
        return texture(Minecraft.getInstance().getTextureManager().getTexture(id));
    }

    public static WLTexture withFilter(final WLTexture texture, final WLFilter filter) {
        return WLTexture.wrap(texture.view(), sampler(filter));
    }

    public static boolean renderTargetsBottomUp() {
        return true;
    }

    public static WLTexture mainTarget() {
        final RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        return new WLTexture(target.getColorTexture(), target.getColorTextureView(), null, false);
    }

    public static int targetWidth() {
        return Minecraft.getInstance().gameRenderer.mainRenderTarget().width;
    }

    public static int targetHeight() {
        return Minecraft.getInstance().gameRenderer.mainRenderTarget().height;
    }

    public static void clear(final WLTexture texture) {
        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(texture.gpuTexture(), TRANSPARENT);
    }

    public static void copyMainTo(final WLTexture destination) {
        final RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        RenderSystem.getDevice()
            .createCommandEncoder()
            .copyTextureToTexture(target.getColorTexture(), destination.gpuTexture(), 0, 0, 0, 0, 0, target.width, target.height);
    }

    public static WLTransform transform(final Matrix4f modelView) {
        return new WLTransform(RenderSystem.getDynamicUniforms().writeTransform(modelView));
    }

    public static WLPass pass(final Supplier<String> label, final WLTexture target) {
        reserveQuadIndices(4);
        quadVertices();
        return new WLPass(RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(label, target.view(), Optional.empty()));
    }

    static GpuSampler sampler(final WLFilter filter) {
        return RenderSystem.getSamplerCache().getClampToEdge(filter == WLFilter.LINEAR ? FilterMode.LINEAR : FilterMode.NEAREST);
    }

    static void reserveQuadIndices(final int vertexCount) {
        RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS).getBuffer(Math.max(6, vertexCount / 4 * 6));
    }

    static synchronized GpuBuffer quadVertices() {
        if (quadVertices == null) {
            try (final MemoryStack stack = MemoryStack.stackPush()) {
                final ByteBuffer bytes = stack.malloc(4 * 3 * 4);
                final FloatBuffer floats = bytes.asFloatBuffer();
                floats.put(new float[] { -1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f });
                bytes.rewind();
                quadVertices = RenderSystem.getDevice().createBuffer(() -> "walksylib_quad_vbo", GpuBuffer.USAGE_VERTEX, bytes);
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

    private static ColorTargetState colorTarget(final WLBlend blend) {
        return switch (blend) {
            case OPAQUE -> ColorTargetState.DEFAULT;
            case TRANSLUCENT -> new ColorTargetState(BlendFunction.TRANSLUCENT);
            case PREMULTIPLIED -> new ColorTargetState(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA);
        };
    }
}
