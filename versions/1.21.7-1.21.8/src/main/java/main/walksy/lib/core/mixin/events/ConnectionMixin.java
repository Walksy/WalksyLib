package main.walksy.lib.core.mixin.events;

import main.walksy.lib.api.event.Cancellable;
import main.walksy.lib.api.event.WalksyLibEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {
    @Inject(method = "genericsFtw", at = @At("HEAD"), cancellable = true)
    private static <T extends PacketListener> void onStartGenericsFtw(final Packet<T> packet, final PacketListener listener, final CallbackInfo ci) {
        final Cancellable cancellable = new Cancellable();
        WalksyLibEvents.START_RECEIVE_PACKET.invoker().receive(packet, listener, ConnectionMixin::ensureRunningOnSameThread, cancellable);
        if (cancellable.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "genericsFtw", at = @At("TAIL"))
    private static <T extends PacketListener> void onEndGenericsFtw(final Packet<T> packet, final PacketListener listener, final CallbackInfo ci) {
        WalksyLibEvents.END_RECEIVE_PACKET.invoker().receive(packet, listener, ConnectionMixin::ensureRunningOnSameThread, new Cancellable());
    }

    @Unique
    @SuppressWarnings("unchecked")
    private static <T extends PacketListener> void ensureRunningOnSameThread(final Minecraft minecraft, final Packet<?> packet, final PacketListener listener) {
        PacketUtils.ensureRunningOnSameThread((Packet<T>) packet, (T) listener, minecraft);
    }
}
