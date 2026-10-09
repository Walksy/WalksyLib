package main.walksy.lib.core.render.gpu;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class WLGpu {
    private static final Map<String, WLPipeline> PIPELINES = new HashMap<>();
    private static final List<WLUniformBuffer> TRANSFORMS = new ArrayList<>();
    private static int transformCursor;
    private static WLUniformBuffer defaultProjection;

    private WLGpu() {
    }

    public static synchronized WLPipeline pipeline(final WLPipelineSpec spec) {
        return PIPELINES.computeIfAbsent(spec.name(), name -> new WLPipeline(spec));
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
        final TextureTarget target = new TextureTarget(width, height, false);
        target.setFilterMode(GL11.GL_LINEAR);
        final WLTexture texture = new WLTexture(target.getColorTextureId(), target, null, true);
        clear(texture);
        return texture;
    }

    public static WLTexture texture(final AbstractTexture texture) {
        return WLTexture.wrap(texture.getId());
    }

    public static WLTexture texture(final Identifier id) {
        return texture(Minecraft.getInstance().getTextureManager().getTexture(id));
    }

    public static WLTexture withFilter(final WLTexture texture, final WLFilter filter) {
        return new WLTexture(texture.glId(), texture.target(), filter, false);
    }

    public static boolean renderTargetsBottomUp() {
        return true;
    }

    public static WLTexture mainTarget() {
        final RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        return new WLTexture(target.getColorTextureId(), target, null, false);
    }

    public static int targetWidth() {
        return Minecraft.getInstance().getMainRenderTarget().width;
    }

    public static int targetHeight() {
        return Minecraft.getInstance().getMainRenderTarget().height;
    }

    public static void clear(final WLTexture texture) {
        final RenderTarget target = texture.target();
        if (target == null) {
            return;
        }
        if (VulkanBackend.active()) {
            VulkanBackend.clear(target);
            return;
        }
        final boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        GlStateManager._disableScissorTest();
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, target.frameBufferId);
        GlStateManager._colorMask(true, true, true, true);
        GlStateManager._clearColor(0f, 0f, 0f, 0f);
        GlStateManager._clear(GL11.GL_COLOR_BUFFER_BIT);
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        if (scissor) {
            GlStateManager._enableScissorTest();
        }
    }

    public static void copyMainTo(final WLTexture destination) {
        if (VulkanBackend.active()) {
            return;
        }
        final RenderTarget target = destination.target();
        if (target == null) {
            return;
        }
        final RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        final boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        GlStateManager._disableScissorTest();
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, main.width, main.height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        main.bindWrite(true);
        if (scissor) {
            GlStateManager._enableScissorTest();
        }
    }

    public static void copyToMain(final WLTexture source) {
        if (VulkanBackend.active()) {
            return;
        }
        final RenderTarget target = source.target();
        if (target == null) {
            return;
        }
        final RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        final boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        GlStateManager._disableScissorTest();
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, target.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, main.width, main.height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        main.bindWrite(true);
        if (scissor) {
            GlStateManager._enableScissorTest();
        }
    }

    public static void clearMainDepth() {
        if (VulkanBackend.active()) {
            return;
        }
        final boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        GlStateManager._disableScissorTest();
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        GlStateManager._depthMask(true);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        if (scissor) {
            GlStateManager._enableScissorTest();
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
        return new WLPass(target);
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
}
