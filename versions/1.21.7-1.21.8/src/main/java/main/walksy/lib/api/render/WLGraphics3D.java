package main.walksy.lib.api.render;

import main.walksy.lib.core.render.WLGeometry;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public final class WLGraphics3D {
    private final PoseStack pose;
    private final MultiBufferSource.BufferSource source;
    private final DeltaTracker deltaTracker;

    public WLGraphics3D(final PoseStack pose, final MultiBufferSource.BufferSource source, final DeltaTracker deltaTracker) {
        this.pose = pose;
        this.source = source;
        this.deltaTracker = deltaTracker;
    }

    public PoseStack pose() {
        return this.pose;
    }

    public MultiBufferSource.BufferSource source() {
        return this.source;
    }

    public DeltaTracker deltaTracker() {
        return this.deltaTracker;
    }

    public float partialTick() {
        return this.deltaTracker.getGameTimeDeltaPartialTick(true);
    }

    public void submit(final PoseStack matrices, final RenderType type, final WLGeometry geometry) {
        geometry.emit(matrices, this.source.getBuffer(type));
    }

    public void finish() {
        this.source.endBatch();
    }
}
