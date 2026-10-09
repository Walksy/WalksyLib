package main.walksy.lib.core.render.font.msdf;

import main.walksy.lib.core.render.gpu.WLBlend;
import main.walksy.lib.core.render.gpu.WLPipelineSpec;
import main.walksy.lib.core.render.gpu.WLVertexFormat;

import java.util.List;

final class TextRenderPipelines {
    private TextRenderPipelines() {
    }

    static WLPipelineSpec spec(final String pipelineName, final String fragmentShader, final String... uniforms) {
        return new WLPipelineSpec(pipelineName, "core/msdf_text", "program/" + fragmentShader, WLVertexFormat.POSITION_TEX_COLOR, WLBlend.TRANSLUCENT, List.of(uniforms), List.of("Sampler0"));
    }
}
