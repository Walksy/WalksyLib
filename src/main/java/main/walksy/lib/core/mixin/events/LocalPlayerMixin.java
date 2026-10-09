package main.walksy.lib.core.mixin.events;

import main.walksy.lib.api.event.WalksyLibEvents;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    public void onStartTick(final CallbackInfo ci) {
        WalksyLibEvents.START_LOCAL_PLAYER_TICK.invoker().tick(LocalPlayer.class.cast(this));
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void onEndTick(final CallbackInfo ci) {
        WalksyLibEvents.END_LOCAL_PLAYER_TICK.invoker().tick(LocalPlayer.class.cast(this));
    }
}
