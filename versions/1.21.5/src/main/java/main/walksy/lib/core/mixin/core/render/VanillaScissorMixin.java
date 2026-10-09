package main.walksy.lib.core.mixin.core.render;

import com.mojang.blaze3d.systems.RenderSystem;
import main.walksy.lib.core.render.gpu.VanillaScissor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public abstract class VanillaScissorMixin {
    @Inject(method = "enableScissor", at = @At("HEAD"))
    private static void walksylib$trackScissor(final int x, final int y, final int width, final int height, final CallbackInfo ci) {
        VanillaScissor.enable(x, y, width, height);
    }

    @Inject(method = "disableScissor", at = @At("HEAD"))
    private static void walksylib$trackNoScissor(final CallbackInfo ci) {
        VanillaScissor.disable();
    }
}
