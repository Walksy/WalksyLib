package main.walksy.lib.core.mixin.core.render.msdf;

import main.walksy.lib.core.render.WLRenderSystem;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiRenderer.class)
public class MsdfGuiRendererMixin {
    @Shadow @Final private List<?> draws;

    @Inject(method = "draw", at = @At("RETURN"))
    private void walksylib$flushMsdfText(final CallbackInfo ci) {
        if (!this.draws.isEmpty()) {
            WLRenderSystem.flushText();
        }
        WLRenderSystem.finishGuiFrame();
        WLRenderSystem.flushDev();
    }
}
