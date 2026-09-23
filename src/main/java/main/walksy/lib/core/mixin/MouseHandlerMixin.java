package main.walksy.lib.core.mixin;

import main.walksy.lib.core.callback.WalksyLibDropCallback;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "onDrop", at = @At("HEAD"))
    private void onDrop(long handle, List<String> rawPaths, CallbackInfo ci) {
        WalksyLibDropCallback.onFileDrop(rawPaths);
    }
}
