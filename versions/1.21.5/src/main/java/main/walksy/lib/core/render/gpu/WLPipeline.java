package main.walksy.lib.core.render.gpu;

import com.mojang.blaze3d.pipeline.RenderPipeline;

import java.util.List;

public final class WLPipeline {
    private final RenderPipeline pipeline;
    private final WLPipelineSpec spec;
    private final List<String> uniformBlocks;
    private VulkanBackend.Program vulkan;

    public WLPipeline(final RenderPipeline pipeline, final WLPipelineSpec spec) {
        this.pipeline = pipeline;
        this.spec = spec;
        this.uniformBlocks = List.copyOf(spec.uniforms());
    }

    WLPipelineSpec spec() {
        return this.spec;
    }

    VulkanBackend.Program vulkan() {
        if (this.vulkan == null) {
            this.vulkan = VulkanBackend.compile(this.spec);
        }
        return this.vulkan;
    }

    public RenderPipeline pipeline() {
        return this.pipeline;
    }

    List<String> uniformBlocks() {
        return this.uniformBlocks;
    }
}
