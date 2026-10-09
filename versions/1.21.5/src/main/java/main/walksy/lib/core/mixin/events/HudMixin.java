package main.walksy.lib.core.mixin.events;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.api.event.WalksyLibEvents;
import main.walksy.lib.core.render.GuiContext;
import main.walksy.lib.core.render.WLGraphics2D;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class HudMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void onExtractRenderState(final GuiGraphics graphics, final DeltaTracker deltaTracker, final CallbackInfo ci) {
        GuiContext.beginFrame(graphics);
        try {
            final WLGraphics2D context = GuiContext.graphics(graphics);
            WalksyLibEvents.RENDER_HUD.invoker().render(context, deltaTracker);
            WalksyLib.getInstance().getHudSpaces().renderAll(context);
        } finally {
            GuiContext.endFrame();
        }
    }
}
