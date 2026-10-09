package main.walksy.lib.core.render.glass;

import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.gpu.WLBlend;
import main.walksy.lib.core.render.gpu.WLPipeline;
import main.walksy.lib.core.render.gpu.WLPipelineSpec;
import main.walksy.lib.core.render.gpu.WLVertexFormat;

import java.util.List;

public final class LiquidGlassPipelines {
    private static final WLPipelineSpec LIQUID_GLASS_GUI = new WLPipelineSpec("liquid_glass_gui", "core/blit_fullscreen", "program/liquid_glass_gui", WLVertexFormat.POSITION, WLBlend.OPAQUE, List.of("SamplerInfo", "CustomUniforms", "WidgetInfo", "BgConfig"), List.of("Sampler0", "Sampler1", "Sampler2", "Sampler3", "Sampler4", "Sampler5", "Sampler6", "Sampler7", "Sampler8", "Sampler9", "Sampler10", "Sampler11"));

    private LiquidGlassPipelines() {
    }

    public static WLPipeline getGuiPipeline() {
        return WLRenderSystem.pipeline(LIQUID_GLASS_GUI);
    }
}
