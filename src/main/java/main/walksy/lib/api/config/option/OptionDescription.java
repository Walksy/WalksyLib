package main.walksy.lib.api.config.option;

import main.walksy.lib.core.render.WLGraphics2D;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class OptionDescription {
    private final Type type;
    private final BiConsumer<WLGraphics2D, Panel> renderer;
    private final Supplier<String> text;

    private OptionDescription(final Type type, final BiConsumer<WLGraphics2D, Panel> renderer, final Supplier<String> text) {
        this.type = type;
        this.renderer = renderer;
        this.text = text;
    }

    public static OptionDescription ofRender2D(final BiConsumer<WLGraphics2D, Panel> renderer) {
        return new OptionDescription(Type.RENDER, renderer, null);
    }

    public static OptionDescription ofText(final Supplier<String> text) {
        return new OptionDescription(Type.TEXT, null, text);
    }

    public Type getType() {
        return this.type;
    }

    public BiConsumer<WLGraphics2D, Panel> getRenderer() {
        return this.renderer;
    }

    public Supplier<String> getText() {
        return this.text;
    }

    public enum Type {
        RENDER,
        TEXT
    }

    public record Panel(int x, int y, int width, int height) {
        public int endX() {
            return this.x + this.width;
        }

        public int endY() {
            return this.y + this.height;
        }
    }
}
