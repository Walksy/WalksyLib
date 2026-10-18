package main.walksy.lib.core.mixin.core.render.toast;

import com.mojang.blaze3d.TracyFrameCapture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import main.walksy.lib.core.render.WLRenderSystem;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Window.class, priority = 1100)
public abstract class PresentMixin {
    @Inject(method = "updateDisplay", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;flipFrame(JLcom/mojang/blaze3d/TracyFrameCapture;)V"))
    private void walksylib$compositeTopOverFinishedFrame(final TracyFrameCapture capture, final CallbackInfo ci) {
        if (WLRenderSystem.vulkanMod()) {
            if (!WLRenderSystem.frameEndHookSeen() && WLRenderSystem.takeDeferredTop()) {
                WLRenderSystem.renderGlassTop();
            }
            return;
        }
        if (!WLRenderSystem.takeDeferredTop()) {
            return;
        }
        final RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        final Window window = (Window) (Object) this;
        if (window.getWidth() == target.width && window.getHeight() == target.height) {
            GlStateManager._disableScissorTest();
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
            GlStateManager._glBlitFrameBuffer(0, 0, target.width, target.height, 0, 0, target.width, target.height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        }
        WLRenderSystem.renderGlassTop();
        target.unbindWrite();
        target.blitToScreen(window.getWidth(), window.getHeight());
    }
}
