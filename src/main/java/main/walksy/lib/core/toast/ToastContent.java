package main.walksy.lib.core.toast;

import main.walksy.lib.core.render.WLGraphics2D;

public interface ToastContent {

    float width(float uiScale);

    float height(float uiScale);

    void extract(WLGraphics2D graphics, float x, float y, float width, float height, float alpha, float uiScale, int layer);
}
