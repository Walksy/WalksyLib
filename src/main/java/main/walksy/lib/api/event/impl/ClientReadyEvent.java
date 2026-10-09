package main.walksy.lib.api.event.impl;

import net.minecraft.client.Minecraft;

@FunctionalInterface
public interface ClientReadyEvent {
    void ready(Minecraft minecraft);
}
