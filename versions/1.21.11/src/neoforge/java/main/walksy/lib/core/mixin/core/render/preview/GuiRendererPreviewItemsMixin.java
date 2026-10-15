package main.walksy.lib.core.mixin.core.render.preview;

import main.walksy.lib.core.render.preview.PreviewItems;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererPreviewItemsMixin {
    @Shadow
    @Final
    GuiRenderState renderState;

    @Shadow
    @Final
    private MultiBufferSource.BufferSource bufferSource;

    @Inject(method = "preparePictureInPictureState", at = @At("HEAD"), cancellable = true)
    private void walksylib$preparePreviewItem(final PictureInPictureRenderState state, final int guiScale, final boolean firstPass, final CallbackInfoReturnable<Boolean> cir) {
        if (firstPass && PreviewItems.prepare(state, this.renderState, this.bufferSource, guiScale)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "clearUnusedOversizedItemRenderers", at = @At("TAIL"))
    private void walksylib$clearUnusedPreviewItems(final CallbackInfo ci) {
        PreviewItems.clearUnused();
    }
}
