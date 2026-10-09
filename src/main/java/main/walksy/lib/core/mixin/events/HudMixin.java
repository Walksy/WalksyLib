package main.walksy.lib.core.mixin.events;

import main.walksy.lib.core.render.GuiContext;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.api.event.WalksyLibEvents;
import main.walksy.lib.core.render.WLGraphics2D;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void onExtractRenderState(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker, final CallbackInfo ci) {
        WalksyLibEvents.RENDER_HUD.invoker().render(GuiContext.graphics(graphics), deltaTracker);
        WalksyLib.getInstance().getHudSpaces().renderAll(GuiContext.graphics(graphics));
    }
}
