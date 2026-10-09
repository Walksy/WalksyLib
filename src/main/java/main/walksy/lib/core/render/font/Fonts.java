package main.walksy.lib.core.render.font;

import main.walksy.lib.core.WalksyLibConstants;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.render.font.msdf.MsdfFont;
import net.minecraft.resources.Identifier;

public final class Fonts {
    public static final float PIXEL_SIZE_PER_PIXEL = 12f;
    public static MsdfFont SFPRO_DISPLAY;
    public static MsdfFont SFPRO_DISPLAY_MEDIUM;
    public static MsdfFont SFPRO_DISPLAY_BOLD;

    private Fonts() {
    }

    public static void load() {
        SFPRO_DISPLAY = MsdfFont.builder().otf(getOtf("sfpro_display")).build();
        SFPRO_DISPLAY_MEDIUM = MsdfFont.builder().otf(getOtf("sfpro_display_medium")).build();
        SFPRO_DISPLAY_BOLD = MsdfFont.builder().otf(getOtf("sfpro_display_bold")).build();
    }

    public static void warm(final float uiScale) {
        for (final Type type : Type.values()) {
            final float size = type.size() * uiScale;
            SFPRO_DISPLAY.warm(size);
            SFPRO_DISPLAY_MEDIUM.warm(size);
            SFPRO_DISPLAY_BOLD.warm(size);
        }
    }

    private static Identifier getOtf(final String font) {
        return Identifier.fromNamespaceAndPath(WalksyLibConstants.NAMESPACE, "fonts/" + font + ".otf");
    }
}
