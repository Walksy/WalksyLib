package main.walksy.lib.api.render;

import main.walksy.lib.core.render.WLGeometry;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

public final class WLGraphics3D {
    private final PoseStack pose;
    private final SubmitNodeCollector collector;
    private final DeltaTracker deltaTracker;

    public WLGraphics3D(final PoseStack pose, final SubmitNodeCollector collector, final DeltaTracker deltaTracker) {
        this.pose = pose;
        this.collector = collector;
        this.deltaTracker = deltaTracker;
    }

    public PoseStack pose() {
        return this.pose;
    }

    public SubmitNodeCollector collector() {
        return this.collector;
    }

    public DeltaTracker deltaTracker() {
        return this.deltaTracker;
    }

    public float partialTick() {
        return this.deltaTracker.getGameTimeDeltaPartialTick(true);
    }

    public void submit(final PoseStack matrices, final RenderType type, final WLGeometry geometry) {
        this.collector.submitCustomGeometry(matrices, type, (pose, buffer) -> {
            final PoseStack stack = new PoseStack();
            stack.last().set(pose);
            geometry.emit(stack, buffer);
        });
    }

    public void finish() {
    }
}
