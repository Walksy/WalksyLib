package main.walksy.lib.core.mixin.events;

import main.walksy.lib.api.event.Cancellable;
import main.walksy.lib.api.event.WalksyLibEvents;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PacketUtils.class)
public class PacketHandleMixin {
    @Inject(method = "method_11072", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/Packet;handle(Lnet/minecraft/network/PacketListener;)V"), cancellable = true)
    private static void walksylib$onStartHandle(final PacketListener listener, final Packet<?> packet, final CallbackInfo ci) {
        if (listener.flow() != PacketFlow.CLIENTBOUND) {
            return;
        }
        final Cancellable cancellable = new Cancellable();
        WalksyLibEvents.START_RECEIVE_PACKET_MAIN.invoker().receive(packet, listener, cancellable);
        if (cancellable.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "method_11072", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/Packet;handle(Lnet/minecraft/network/PacketListener;)V", shift = At.Shift.AFTER))
    private static void walksylib$onEndHandle(final PacketListener listener, final Packet<?> packet, final CallbackInfo ci) {
        if (listener.flow() != PacketFlow.CLIENTBOUND) {
            return;
        }
        WalksyLibEvents.END_RECEIVE_PACKET_MAIN.invoker().receive(packet, listener, new Cancellable());
    }
}
