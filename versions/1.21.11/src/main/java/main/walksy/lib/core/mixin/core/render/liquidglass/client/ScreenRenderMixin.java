package main.walksy.lib.core.mixin.core.render.liquidglass.client;

import main.walksy.lib.core.render.GuiContext;
import main.walksy.lib.core.render.WLRenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenRenderMixin {
    @Inject(method = "renderBlurredBackground(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), cancellable = true)
    private void walksylib$takeOverBlur(final GuiGraphics context, final CallbackInfo ci) {
        WLRenderSystem.glassUniforms().requestScreenBlur(GuiContext.graphics(context));
        ci.cancel();
    }

    @Inject(method = "renderTransparentBackground(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), cancellable = true)
    private void walksylib$onRenderDarkening(final GuiGraphics context, final CallbackInfo ci) {
        ci.cancel();
    }
}
