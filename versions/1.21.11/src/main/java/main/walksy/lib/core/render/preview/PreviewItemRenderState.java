package main.walksy.lib.core.render.preview;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import org.jetbrains.annotations.Nullable;

public record PreviewItemRenderState(String key, TrackingItemStackRenderState item, int x0, int y0, int x1, int y1, float scale, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {
    public static PreviewItemRenderState of(final String key, final TrackingItemStackRenderState item, final int x0, final int y0, final int x1, final int y1, @Nullable final ScreenRectangle scissorArea) {
        return new PreviewItemRenderState(key, item, x0, y0, x1, y1, x1 - x0, scissorArea, PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
    }

    public String rendererKey() {
        return this.key + "@" + (this.x1 - this.x0) + "x" + (this.y1 - this.y0);
    }
}
