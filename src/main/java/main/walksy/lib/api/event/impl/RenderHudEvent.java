package main.walksy.lib.api.event.impl;

import main.walksy.lib.core.render.WLGraphics2D;
import net.minecraft.client.DeltaTracker;

@FunctionalInterface
public interface RenderHudEvent {
    void render(WLGraphics2D graphics, DeltaTracker deltaTracker);
}
