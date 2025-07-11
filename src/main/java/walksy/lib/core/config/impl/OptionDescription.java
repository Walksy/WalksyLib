package walksy.lib.core.config.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class OptionDescription {
    private final OptionType type;
    private final BiConsumer<DrawContext, OptionPanel> renderConsumer;
    private final Supplier<String> textSupplier;

    private OptionDescription(OptionType type, BiConsumer<DrawContext, OptionPanel> renderConsumer, Supplier<String> textSupplier) {
        this.type = type;
        this.renderConsumer = renderConsumer;
        this.textSupplier = textSupplier;
    }

    public static OptionDescription ofRender2D(BiConsumer<DrawContext, OptionPanel> renderConsumer) {
        return new OptionDescription(OptionType.RENDER, renderConsumer, null);
    }

    public static OptionDescription ofOrderedString(Supplier<String> textSupplier) {
        return new OptionDescription(OptionType.TEXT, null, textSupplier);
    }

    public OptionType getType() {
        return type;
    }

    public BiConsumer<DrawContext, OptionPanel> getRenderConsumer() {
        return renderConsumer;
    }

    public Supplier<String> getStringSupplier() {
        return textSupplier;
    }

    public enum OptionType {
        RENDER,
        TEXT
    }

    public record OptionPanel(int x, int y, int width, int height) {
        public int endX() {
            return x + width;
        }


        public int endY() {
            return y + height;
        }
    }
}
