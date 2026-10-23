package main.walksy.lib.api.hud;

import main.walksy.lib.core.render.WLGraphics2D;

import java.util.List;

public interface HudElement {

    void render(WLGraphics2D graphics, float x, float y, float scale, boolean editing);

    float width(float scale);

    float height(float scale);

    default float scaleStep() {
        return 0.1f;
    }

    default boolean snapScale() {
        return false;
    }

    default List<String> hudOptions() {
        return List.of();
    }

    default boolean visible() {
        return true;
    }

    default float defaultAnchorX() {
        return 0.5f;
    }

    default float defaultAnchorY() {
        return 0.5f;
    }

    default float defaultOffsetX() {
        return 0f;
    }

    default float defaultOffsetY() {
        return 0f;
    }
}
