package main.walksy.lib.core.mixin.core.render.toast;

import main.walksy.lib.core.render.GuiContext;

import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.toast.ToastRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.BitSet;

@Mixin(ToastManager.class)
public abstract class ToastManagerMixin {
    @Shadow @Final private BitSet occupiedSlots;

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void walksylib$extractToasts(final GuiGraphicsExtractor graphics, final CallbackInfo ci) {
        ToastRenderer.render(GuiContext.graphics(graphics), this.occupiedSlots.length() * (float) Toast.SLOT_HEIGHT);
    }
}
