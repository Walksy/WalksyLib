package main.walksy.lib.api.event.impl;

import main.walksy.lib.api.event.Cancellable;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import org.apache.logging.log4j.util.TriConsumer;

@FunctionalInterface
public interface ReceivePacketEvent {
    void receive(Packet<?> packet, PacketListener listener, TriConsumer<Minecraft, Packet<?>, PacketListener> ensureRunningOnSameThread, Cancellable cancellable);
}
