package main.walksy.lib.api.event.impl;

import net.minecraft.world.entity.player.Player;

@FunctionalInterface
public interface ShieldDisableEvent {
    void disable(Player player);
}
