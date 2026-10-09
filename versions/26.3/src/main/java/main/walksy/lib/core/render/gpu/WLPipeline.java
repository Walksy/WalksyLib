package main.walksy.lib.core.render.gpu;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;

public final class WLPipeline {
    private final RenderPipeline pipeline;

    public WLPipeline(final RenderPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public RenderPipeline pipeline() {
        return this.pipeline;
    }
}
