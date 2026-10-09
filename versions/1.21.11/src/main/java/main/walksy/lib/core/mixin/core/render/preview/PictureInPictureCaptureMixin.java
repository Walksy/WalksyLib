package main.walksy.lib.core.mixin.core.render.preview;

import main.walksy.lib.core.WalksyLib;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import main.walksy.lib.core.render.gpu.WLTexture;
import main.walksy.lib.core.render.preview.PreviewItems;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PictureInPictureRenderer.class)
public abstract class PictureInPictureCaptureMixin {
    @Shadow
    private GpuTextureView textureView;

    @Inject(method = "blitTexture", at = @At("HEAD"), cancellable = true)
    private void walksylib$capturePreviewEntity(final PictureInPictureRenderState state, final GuiRenderState guiRenderState, final CallbackInfo ci) {
        if (this.textureView == null) {
            return;
        }
        final WLTexture texture = WLTexture.wrap(this.textureView, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST));
        if (WalksyLib.getInstance().getPreviewEntities().capture(state, texture) || PreviewItems.capture(state, texture)) {
            ci.cancel();
        }
    }
}
