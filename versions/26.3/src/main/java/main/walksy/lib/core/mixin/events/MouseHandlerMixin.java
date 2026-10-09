package main.walksy.lib.core.mixin.events;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.api.event.WalksyLibEvents;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Inject(method = "onButton", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/FramerateLimitTracker;onInputReceived()V"))
    public void onButton(final long handle, final MouseButtonInfo rawButtonInfo, final int action, final CallbackInfo ci) {
        WalksyLib.getInstance().getMouseHandler().onButton(rawButtonInfo.button(), action);
        WalksyLibEvents.MOUSE_BUTTON_INPUT.invoker().button(rawButtonInfo.button(), action, rawButtonInfo.modifiers());
    }

    @Inject(method = "onScroll", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/FramerateLimitTracker;onInputReceived()V"))
    public void onScroll(final long handle, final double xoffset, final double yoffset, final CallbackInfo ci) {
        WalksyLibEvents.MOUSE_SCROLL_INPUT.invoker().scroll(xoffset, yoffset);
    }

    @Inject(method = "onDrop", at = @At("HEAD"))
    public void onDropFile(final long handle, final List<String> files, final CallbackInfo ci) {
        final List<Path> paths = new ArrayList<>(files.size());
        int failedCount = 0;
        for (final String file : files) {
            try {
                paths.add(Path.of(file));
            } catch (final InvalidPathException e) {
                failedCount++;
            }
        }
        WalksyLib.getInstance().onFileDrop(paths);
        WalksyLibEvents.FILE_DROP.invoker().dropFile(paths, failedCount);
    }
}
