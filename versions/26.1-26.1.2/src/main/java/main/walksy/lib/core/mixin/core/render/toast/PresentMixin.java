package main.walksy.lib.core.mixin.core.render.toast;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import main.walksy.lib.core.render.WLRenderSystem;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public abstract class PresentMixin {
    @WrapOperation(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen()V"))
    private void walksylib$compositeTopBeforePresent(final RenderTarget target, final Operation<Void> original) {
        if (!WLRenderSystem.frameEndHookSeen() && WLRenderSystem.takeDeferredTop()) {
            WLRenderSystem.renderGlassTop();
        }
        original.call(target);
    }
}
