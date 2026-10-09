package main.walksy.lib.core.render.glass.gui;

import main.walksy.lib.core.render.element.WLElement;
import main.walksy.lib.core.render.element.WLElementKind;
import main.walksy.lib.core.render.element.WLRect;
import main.walksy.lib.core.render.element.WLVertexSink;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

public record LiquidGlassGuiElementRenderState(float x1, float y1, float x2, float y2, float cornerRadius, @Nullable Component text, GlassStyle style, Matrix3x2f pose, @Nullable WLRect scissorArea, float hover, float focus, float stretchX, float stretchY) implements WLElement {

    @Override
    public WLElementKind kind() {
        return WLElementKind.FLAT;
    }

    @Override
    public void emit(final WLVertexSink sink) {
        sink.posed(this.pose, this.x1, this.y1).color(0);
        sink.posed(this.pose, this.x1, this.y2).color(0);
        sink.posed(this.pose, this.x2, this.y2).color(0);
        sink.posed(this.pose, this.x2, this.y1).color(0);
    }

    @Override
    public @Nullable WLRect bounds() {
        final WLRect ownBounds = new WLRect(Math.round(this.x1), Math.round(this.y1), Math.round(this.x2 - this.x1), Math.round(this.y2 - this.y1)).transformMaxBounds(this.pose);
        return WLRect.clipped(ownBounds, this.scissorArea);
    }
}
