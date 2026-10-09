package main.walksy.lib.api.event.impl;

import main.walksy.lib.api.render.WLGraphics3D;

@FunctionalInterface
public interface Render3DEvent {
    void render(WLGraphics3D graphics);
}
