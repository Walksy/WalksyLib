package main.walksy.lib.core.render.preview;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;

public final class PreviewItemRenderer extends PictureInPictureRenderer<PreviewItemRenderState> {
    private static final int FULL_BRIGHT = 15728880;
    private boolean usedOnThisFrame;
    private Object modelOnTexture;

    public PreviewItemRenderer(final MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    public void markUsed() {
        this.usedOnThisFrame = true;
    }

    public boolean usedOnThisFrame() {
        return this.usedOnThisFrame;
    }

    public void resetUsedOnThisFrame() {
        this.usedOnThisFrame = false;
    }

    @Override
    public Class<PreviewItemRenderState> getRenderStateClass() {
        return PreviewItemRenderState.class;
    }

    @Override
    protected boolean textureIsReadyToBlit(final PreviewItemRenderState state) {
        return !state.item().isAnimated() && state.item().getModelIdentity().equals(this.modelOnTexture);
    }

    @Override
    protected void renderToTexture(final PreviewItemRenderState state, final PoseStack poseStack) {
        poseStack.scale(1f, -1f, -1f);
        Minecraft.getInstance().gameRenderer.getLighting().setupFor(state.item().usesBlockLight() ? Lighting.Entry.ITEMS_3D : Lighting.Entry.ITEMS_FLAT);
        final FeatureRenderDispatcher dispatcher = Minecraft.getInstance().gameRenderer.getFeatureRenderDispatcher();
        state.item().submit(poseStack, dispatcher.getSubmitNodeStorage(), FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        dispatcher.renderAllFeatures();
        this.modelOnTexture = state.item().getModelIdentity();
    }

    @Override
    protected float getTranslateY(final int height, final int guiScale) {
        return height / 2f;
    }

    @Override
    protected String getTextureLabel() {
        return "walksylib preview item";
    }
}
