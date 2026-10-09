package main.walksy.lib.api.event.impl;

import net.minecraft.client.player.LocalPlayer;

@FunctionalInterface
public interface LocalPlayerTickEvent {
    void tick(LocalPlayer player);
}
