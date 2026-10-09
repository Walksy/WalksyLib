package main.walksy.lib.core.mixin.events;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.api.event.WalksyLibEvents;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "onResourceLoadFinished", at = @At("TAIL"))
    private void walksylib$onClientReady(final CallbackInfo ci) {
        WalksyLib.getInstance().onClientReady();
        WalksyLibEvents.CLIENT_READY.invoker().ready(Minecraft.class.cast(this));
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void onStartTick(final CallbackInfo ci) {
        WalksyLibEvents.START_CLIENT_TICK.invoker().tick(Minecraft.class.cast(this));
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void onEndTick(final CallbackInfo ci) {
        final Minecraft minecraft = Minecraft.class.cast(this);
        WalksyLib.getInstance().onClientTickEnd(minecraft);
        WalksyLibEvents.END_CLIENT_TICK.invoker().tick(minecraft);
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void walksylib$onClientStopping(final CallbackInfo ci) {
        WalksyLib.getInstance().onClientStopping();
    }
}
