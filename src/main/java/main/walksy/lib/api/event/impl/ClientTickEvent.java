package main.walksy.lib.api.event.impl;

import net.minecraft.client.Minecraft;

@FunctionalInterface
public interface ClientTickEvent {
    void tick(Minecraft minecraft);
}
