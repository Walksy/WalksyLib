package main.walksy.lib.api.event.impl;

import main.walksy.lib.api.event.Cancellable;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

@FunctionalInterface
public interface MainThreadPacketEvent {
    void receive(Packet<?> packet, PacketListener listener, Cancellable cancellable);
}
