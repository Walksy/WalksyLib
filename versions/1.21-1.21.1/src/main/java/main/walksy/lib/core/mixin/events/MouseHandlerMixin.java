package main.walksy.lib.core.mixin.events;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.api.event.WalksyLibEvents;
import main.walksy.lib.core.platform.WLInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.List;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Inject(method = "onPress", at = @At("HEAD"))
    public void onButton(final long handle, final int button, final int action, final int modifiers, final CallbackInfo ci) {
        if (handle != Minecraft.getInstance().getWindow().getWindow()) {
            return;
        }
        WLInput.mouseButton(button, action, modifiers);
        WalksyLib.getInstance().getMouseHandler().onButton(button, action);
        WalksyLibEvents.MOUSE_BUTTON_INPUT.invoker().button(button, action, modifiers);
    }

    @Inject(method = "onScroll", at = @At("HEAD"))
    public void onScroll(final long handle, final double xoffset, final double yoffset, final CallbackInfo ci) {
        if (handle != Minecraft.getInstance().getWindow().getWindow()) {
            return;
        }
        WalksyLibEvents.MOUSE_SCROLL_INPUT.invoker().scroll(xoffset, yoffset);
    }

    @Inject(method = "onDrop", at = @At("HEAD"))
    public void onDropFile(final long handle, final List<Path> files, final int failedCount, final CallbackInfo ci) {
        WalksyLib.getInstance().onFileDrop(files);
        WalksyLibEvents.FILE_DROP.invoker().dropFile(files, failedCount);
    }
}
