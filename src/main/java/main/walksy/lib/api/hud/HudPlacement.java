package main.walksy.lib.api.hud;

public interface HudPlacement {
    String id();

    boolean visible();

    float screenX();

    float screenY();

    float width();

    float height();
}
