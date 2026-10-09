package main.walksy.lib.core.render.gpu;

import java.util.List;

public record WLPipelineSpec(String name, String vertexShader, String fragmentShader, WLVertexFormat format, WLBlend blend, List<String> uniforms, List<String> samplers) {

    public WLPipelineSpec {
        uniforms = List.copyOf(uniforms);
        samplers = List.copyOf(samplers);
    }
}
