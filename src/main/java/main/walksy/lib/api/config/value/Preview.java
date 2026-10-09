package main.walksy.lib.api.config.value;

import main.walksy.lib.api.render.PreviewCanvas;

@FunctionalInterface
public interface Preview {
    void render(PreviewCanvas canvas);
}
