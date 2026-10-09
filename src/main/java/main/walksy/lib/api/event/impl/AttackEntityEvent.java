package main.walksy.lib.api.event.impl;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

@FunctionalInterface
public interface AttackEntityEvent {
    void attack(Player local, Entity target);
}
