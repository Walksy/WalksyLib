package main.walksy.lib.core.render.texture;

import main.walksy.lib.core.render.GuiContext;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.element.VanillaElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GlyphRenderState;
import org.joml.Matrix3x2f;

public final class VanillaFont {
    private VanillaFont() {
    }

    public static int width(final String text) {
        final Font font = font();
        return font == null || text == null ? 0 : font.width(text);
    }

    private static Font font() {
        final Minecraft client = Minecraft.getInstance();
        return client == null ? null : client.font;
    }

    public static void extract(final WLGraphics2D graphics, final String text, final float x, final float y, final int color, final boolean shadow) {
        final Font font = font();
        if (font == null || text == null || text.isEmpty() || (color >>> 24) == 0) {
            return;
        }
        final Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        final ScreenRectangle scissor = GuiContext.screen(graphics.scissor());
        font.prepareText(text, x, y, color, shadow, 0).visit(new Font.GlyphVisitor() {
            @Override
            public void acceptRenderable(final TextRenderable renderable) {
                graphics.submit(new VanillaElement(new GlyphRenderState(pose, renderable, scissor)));
            }
        });
    }
}
