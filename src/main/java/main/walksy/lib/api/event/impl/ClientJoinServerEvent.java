package main.walksy.lib.api.event.impl;

import net.minecraft.network.protocol.game.ClientboundLoginPacket;

@FunctionalInterface
public interface ClientJoinServerEvent {
    void join(final ClientboundLoginPacket packet);
}
