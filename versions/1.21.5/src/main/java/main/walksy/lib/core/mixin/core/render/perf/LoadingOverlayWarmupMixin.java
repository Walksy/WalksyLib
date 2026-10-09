package main.walksy.lib.core.mixin.core.render.perf;

import main.walksy.lib.core.WalksyLib;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayWarmupMixin {
    @Shadow
    private long fadeOutStart;

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"))
    private void walksylib$holdForWarmup(final GuiGraphics context, final int mouseX, final int mouseY, final float delta, final CallbackInfo ci) {
        if (this.fadeOutStart != -1L) {
            this.fadeOutStart += WalksyLib.getInstance().getPanelWarmup().takeDeferredDelay();
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("RETURN"))
    private void walksylib$requestWarmup(final GuiGraphics context, final int mouseX, final int mouseY, final float delta, final CallbackInfo ci) {
        if (this.fadeOutStart != -1L) {
            WalksyLib.getInstance().getPanelWarmup().request();
        }
    }
}
