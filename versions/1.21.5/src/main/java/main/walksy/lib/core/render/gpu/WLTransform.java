package main.walksy.lib.core.render.gpu;

public final class WLTransform {
    private final WLUniformBuffer buffer;

    public WLTransform(final WLUniformBuffer buffer) {
        this.buffer = buffer;
    }

    WLUniformBuffer buffer() {
        return this.buffer;
    }
}
