package main.walksy.lib.core.mixin.core.render.liquidglass.client;

import main.walksy.lib.core.render.GuiContext;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.WLRenderSystem;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenRenderMixin {
    @Inject(method = "renderBlurredBackground(F)V", at = @At("HEAD"))
    private void walksylib$takeOverBlur(final float partialTick, final CallbackInfo ci) {
        final WLGraphics2D graphics = GuiContext.current();
        if (graphics != null) {
            WLRenderSystem.glassUniforms().requestScreenBlur(graphics);
        }
    }
}
