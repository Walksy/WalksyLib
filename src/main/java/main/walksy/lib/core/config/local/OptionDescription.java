package main.walksy.lib.core.config.local;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class OptionDescription {
    private final OptionType type;
    private final BiConsumer<GuiGraphicsExtractor, OptionPanel> renderConsumer;
    private final Supplier<String> textSupplier;

    private OptionDescription(final OptionType type, final BiConsumer<GuiGraphicsExtractor, OptionPanel> renderConsumer, final Supplier<String> textSupplier) {
        this.type = type;
        this.renderConsumer = renderConsumer;
        this.textSupplier = textSupplier;
    }

    public static OptionDescription ofRender2D(final BiConsumer<GuiGraphicsExtractor, OptionPanel> renderConsumer) {
        return new OptionDescription(OptionType.RENDER, renderConsumer, null);
    }

    public static OptionDescription ofOrderedString(final Supplier<String> textSupplier) {
        return new OptionDescription(OptionType.TEXT, null, textSupplier);
    }

    public OptionType getType() {
        return this.type;
    }

    public BiConsumer<GuiGraphicsExtractor, OptionPanel> getRenderConsumer() {
        return this.renderConsumer;
    }

    public Supplier<String> getStringSupplier() {
        return this.textSupplier;
    }

    public enum OptionType {
        RENDER,
        TEXT
    }

    public record OptionPanel(int x, int y, int width, int height) {
        public int endX() {
            return this.x + this.width;
        }

        public int endY() {
            return this.y + this.height;
        }
    }
}
