package main.walksy.lib.core.mixin.events;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.api.event.WalksyLibEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkMixin {
    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void walksylib$onJoinedServer(final ClientboundLoginPacket packet, final CallbackInfo ci) {
        WalksyLibEvents.CLIENT_JOIN_SERVER.invoker().join(packet);
        WalksyLib.getInstance().onJoinedServer();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void walksylib$onLeftServer(final CallbackInfo ci) {
        WalksyLibEvents.CLIENT_LEFT_SERVER.invoker().leave();
        Minecraft.getInstance().execute(() -> WalksyLib.getInstance().onLeftServer());
    }
}
