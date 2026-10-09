package main.walksy.lib.core.render.font.msdf;

import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.gpu.WLPipeline;
import main.walksy.lib.core.render.gpu.WLPipelineSpec;

final class BitmapTextPipeline {
    private static final WLPipelineSpec SPEC = TextRenderPipelines.spec("bitmap_text", "bitmap_text", "Projection", "DynamicTransforms");
    private static final WLPipelineSpec STRETCHED = TextRenderPipelines.spec("bitmap_text_stretched", "bitmap_text_stretched", "Projection", "DynamicTransforms");
    private static final WLPipelineSpec PIXEL = TextRenderPipelines.spec("bitmap_text_pixel", "bitmap_text_pixel", "Projection", "DynamicTransforms");

    private BitmapTextPipeline() {
    }

    static WLPipeline get() {
        return WLRenderSystem.pipeline(SPEC);
    }

    static WLPipeline stretched() {
        return WLRenderSystem.pipeline(STRETCHED);
    }

    static WLPipeline pixel() {
        return WLRenderSystem.pipeline(PIXEL);
    }
}
