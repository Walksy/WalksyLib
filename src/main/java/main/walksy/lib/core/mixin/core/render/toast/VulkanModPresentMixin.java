package main.walksy.lib.core.mixin.core.render.toast;

import main.walksy.lib.core.render.WLRenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.vulkanmod.vulkan.Renderer", remap = false)
public abstract class VulkanModPresentMixin {
    @Inject(method = "endFrame", at = @At(value = "INVOKE", target = "Lnet/vulkanmod/vulkan/pass/MainPass;end(Lorg/lwjgl/vulkan/VkCommandBuffer;)V"), require = 0, remap = false)
    private void walksylib$compositeTopBeforeMainPassEnds(final CallbackInfo ci) {
        if (WLRenderSystem.takeDeferredTopAtFrameEnd()) {
            WLRenderSystem.renderGlassTop();
        }
    }
}
