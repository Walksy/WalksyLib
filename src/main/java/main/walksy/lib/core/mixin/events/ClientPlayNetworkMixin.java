package main.walksy.lib.core.mixin.events;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import main.walksy.lib.api.event.Cancellable;
import main.walksy.lib.api.event.WalksyLibEvents;
import main.walksy.lib.core.WalksyLib;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
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

    @WrapOperation(method = "handleBundlePacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/Packet;handle(Lnet/minecraft/network/PacketListener;)V"))
    private void walksylib$onBundledPacket(final Packet<?> packet, final PacketListener listener, final Operation<Void> original) {
        final Cancellable cancellable = new Cancellable();
        WalksyLibEvents.START_RECEIVE_PACKET_MAIN.invoker().receive(packet, listener, cancellable);
        if (cancellable.isCancelled()) {
            return;
        }
        original.call(packet, listener);
        WalksyLibEvents.END_RECEIVE_PACKET_MAIN.invoker().receive(packet, listener, new Cancellable());
    }
}
