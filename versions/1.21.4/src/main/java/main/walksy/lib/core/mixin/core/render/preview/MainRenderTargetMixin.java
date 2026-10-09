package main.walksy.lib.core.mixin.core.render.preview;

import main.walksy.lib.core.WalksyLib;
import com.mojang.blaze3d.pipeline.RenderTarget;
import main.walksy.lib.core.render.preview.PreviewItems;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MainRenderTargetMixin {
    @Inject(method = "getMainRenderTarget", at = @At("HEAD"), cancellable = true)
    private void walksylib$redirectPreviewEntity(final CallbackInfoReturnable<RenderTarget> cir) {
        final RenderTarget entity = WalksyLib.getInstance().getPreviewEntities().redirectedTarget();
        final RenderTarget redirected = entity != null ? entity : PreviewItems.redirectedTarget();
        if (redirected != null) {
            cir.setReturnValue(redirected);
        }
    }
}
