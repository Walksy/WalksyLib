package main.walksy.lib.core.render.element;

import main.walksy.lib.core.render.gpu.WLTexture;
import org.jetbrains.annotations.Nullable;

public interface WLElement {
    WLElementKind kind();

    default @Nullable WLTexture texture() {
        return null;
    }

    @Nullable WLRect scissorArea();

    @Nullable WLRect bounds();

    void emit(WLVertexSink sink);
}
