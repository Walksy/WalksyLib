package main.walksy.lib.core.render.gpu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class WLMapping implements AutoCloseable {
    private final ByteBuffer data;
    private final Runnable onClose;

    public WLMapping(final ByteBuffer data, final Runnable onClose) {
        this.data = data.clear().order(ByteOrder.nativeOrder());
        this.onClose = onClose;
    }

    public ByteBuffer data() {
        return this.data;
    }

    public Std140Writer writer() {
        return Std140Writer.into(this.data);
    }

    @Override
    public void close() {
        this.onClose.run();
    }
}
