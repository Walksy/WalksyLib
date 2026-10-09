package main.walksy.lib.core.mixin.core.render.liquidglass.logical;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import main.walksy.lib.core.render.WLRenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class LiquidGlassGameRendererMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void walksylib$beginGuiFrame(final DeltaTracker tickCounter, final boolean tick, final CallbackInfo ci) {
        WLRenderSystem.beginFrame();
    }

    @Redirect(method = "processBlurEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PostChain;process(Lcom/mojang/blaze3d/pipeline/RenderTarget;Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;)V"))
    private void walksylib$blurOnlyForScreens(final PostChain chain, final RenderTarget target, final GraphicsResourceAllocator pool) {
        if (WLRenderSystem.glassUniforms().wantsVanillaBlur()) {
            chain.process(target, pool);
        }
    }

    @Inject(method = "processBlurEffect", at = @At("RETURN"))
    private void walksylib$renderLiquidGlass(final CallbackInfo ci) {
        WLRenderSystem.renderGlass();
    }
}
