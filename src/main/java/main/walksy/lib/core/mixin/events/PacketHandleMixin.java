package main.walksy.lib.core.mixin.events;

import main.walksy.lib.api.event.Cancellable;
import main.walksy.lib.api.event.WalksyLibEvents;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.network.PacketProcessor$ListenerAndPacket")
public class PacketHandleMixin {
    @Shadow
    @Final
    private PacketListener listener;
    @Shadow
    @Final
    private Packet<?> packet;

    @Inject(method = "handle", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/Packet;handle(Lnet/minecraft/network/PacketListener;)V"), cancellable = true)
    private void walksylib$onStartHandle(final CallbackInfo ci) {
        if (this.listener.flow() != PacketFlow.CLIENTBOUND) {
            return;
        }
        final Cancellable cancellable = new Cancellable();
        WalksyLibEvents.START_RECEIVE_PACKET_MAIN.invoker().receive(this.packet, this.listener, cancellable);
        if (cancellable.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "handle", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/Packet;handle(Lnet/minecraft/network/PacketListener;)V", shift = At.Shift.AFTER))
    private void walksylib$onEndHandle(final CallbackInfo ci) {
        if (this.listener.flow() != PacketFlow.CLIENTBOUND) {
            return;
        }
        WalksyLibEvents.END_RECEIVE_PACKET_MAIN.invoker().receive(this.packet, this.listener, new Cancellable());
    }
}
