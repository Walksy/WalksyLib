package main.walksy.lib.core.mixin.events;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.api.event.WalksyLibEvents;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"))
    public void onKey(final long handle, final int key, final int scancode, final int action, final int modifiers, final CallbackInfo ci) {
        if (handle != Minecraft.getInstance().getWindow().getWindow()) {
            return;
        }
        WalksyLib.getInstance().onKeyInput(key, action, scancode);
        WalksyLibEvents.KEY_INPUT.invoker().key(key, action, scancode, modifiers);
    }
}
