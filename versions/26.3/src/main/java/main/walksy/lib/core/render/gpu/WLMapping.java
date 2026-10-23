package main.walksy.lib.core.render.gpu;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class WLMapping implements AutoCloseable {
    private final @Nullable GpuBufferSlice.MappedView view;
    private final @Nullable ByteBuffer staging;
    private final @Nullable Runnable onClose;

    public WLMapping(final GpuBufferSlice.MappedView view) {
        this.view = view;
        this.staging = null;
        this.onClose = null;
    }

    public WLMapping(final ByteBuffer staging, final Runnable onClose) {
        this.view = null;
        this.staging = staging.clear().order(ByteOrder.nativeOrder());
        this.onClose = onClose;
    }

    public ByteBuffer data() {
        return this.view != null ? this.view.data().order(ByteOrder.nativeOrder()) : this.staging;
    }

    public Std140Writer writer() {
        return Std140Writer.into(this.view != null ? this.view.data() : this.staging);
    }

    @Override
    public void close() {
        if (this.view != null) {
            this.view.close();
        } else {
            this.onClose.run();
        }
    }
}
