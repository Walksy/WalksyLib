package main.walksy.lib.core.render.font.msdf;

import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.gpu.WLPipeline;
import main.walksy.lib.core.render.gpu.WLPipelineSpec;

final class MsdfPipeline {
    private static final WLPipelineSpec SPEC = TextRenderPipelines.spec("msdf_text", "msdf_text", "Projection", "DynamicTransforms", "MsdfParams");

    private MsdfPipeline() {
    }

    static WLPipeline get() {
        return WLRenderSystem.pipeline(SPEC);
    }
}
