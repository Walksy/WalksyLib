package main.walksy.lib.core.mixin.core.render.liquidglass.logical;

import main.walksy.lib.core.render.GuiContext;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.perf.OffscreenPanelWarmup;
import main.walksy.lib.core.render.gpu.WLGpu;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class LiquidGlassGameRendererMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void walksylib$beginGuiFrame(final DeltaTracker tickCounter, final boolean tick, final CallbackInfo ci) {
        WLGpu.beginFrame();
        WLRenderSystem.beginFrame();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;flush()V", ordinal = 0, shift = At.Shift.AFTER))
    private void walksylib$endGuiFrame(final DeltaTracker tickCounter, final boolean tick, final CallbackInfo ci) {
        GuiContext.resolveAll();
        WLRenderSystem.flushText();
        if (OffscreenPanelWarmup.pending()) {
            WLRenderSystem.renderGlassTop();
        } else {
            WLRenderSystem.finishGuiFrame();
        }
        WLRenderSystem.flushDev();
        OffscreenPanelWarmup.runIfPending();
    }
}
