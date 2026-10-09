package main.walksy.lib.core.mixin.events;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.api.event.WalksyLibEvents;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/FramerateLimitTracker;onInputReceived()V"))
    public void onKey(final long handle, final int action, final KeyEvent event, final CallbackInfo ci) {
        WalksyLib.getInstance().onKeyInput(event.key(), action, event.scancode());
        WalksyLibEvents.KEY_INPUT.invoker().key(event.key(), action, event.scancode(), event.modifiers());
    }
}
