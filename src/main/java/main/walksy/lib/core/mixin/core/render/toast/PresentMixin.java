package main.walksy.lib.core.mixin.core.render.toast;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuSurface;
import com.mojang.blaze3d.textures.GpuTextureView;
import main.walksy.lib.core.render.WLRenderSystem;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public abstract class PresentMixin {
    @WrapOperation(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/GpuSurface;blitFromTexture(Lcom/mojang/blaze3d/systems/CommandEncoder;Lcom/mojang/blaze3d/textures/GpuTextureView;)V"))
    private void walksylib$compositeTopBeforePresent(final GpuSurface surface, final CommandEncoder encoder, final GpuTextureView texture, final Operation<Void> original) {
        if (WLRenderSystem.takeDeferredTop()) {
            WLRenderSystem.renderGlassTop();
        }
        original.call(surface, encoder, texture);
    }
}
