package main.walksy.lib.core.gui.style;

import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.font.msdf.MsdfFont;

public enum Type {
    LARGE_TITLE(26f, 32f, Face.REGULAR, Face.BOLD),
    TITLE1(22f, 26f, Face.REGULAR, Face.BOLD),
    TITLE2(17f, 22f, Face.REGULAR, Face.BOLD),
    TITLE3(15f, 20f, Face.REGULAR, Face.MEDIUM),
    HEADLINE(13f, 16f, Face.BOLD, Face.BOLD),
    BODY(13f, 16f, Face.REGULAR, Face.MEDIUM),
    CALLOUT(12f, 15f, Face.REGULAR, Face.MEDIUM),
    SUBHEADLINE(11f, 14f, Face.REGULAR, Face.MEDIUM),
    FOOTNOTE(10f, 13f, Face.REGULAR, Face.MEDIUM),
    CAPTION1(10f, 13f, Face.REGULAR, Face.MEDIUM),
    CAPTION2(10f, 13f, Face.REGULAR, Face.MEDIUM);

    private final float size;
    private final float lineHeight;
    private final Face defaultFace;
    private final Face emphasizedFace;

    Type(final float size, final float lineHeight, final Face defaultFace, final Face emphasizedFace) {
        this.size = size;
        this.lineHeight = lineHeight;
        this.defaultFace = defaultFace;
        this.emphasizedFace = emphasizedFace;
    }

    public float size() {
        return this.size;
    }

    public float lineHeight() {
        return this.lineHeight;
    }

    public MsdfFont regular() {
        return resolve(this.defaultFace);
    }

    public MsdfFont emphasized() {
        return resolve(this.emphasizedFace);
    }

    private static MsdfFont resolve(final Face face) {
        return switch (face) {
            case REGULAR -> Fonts.SFPRO_DISPLAY;
            case MEDIUM -> Fonts.SFPRO_DISPLAY_MEDIUM;
            case BOLD -> Fonts.SFPRO_DISPLAY_BOLD;
        };
    }

    private enum Face {
        REGULAR,
        MEDIUM,
        BOLD
    }
}
