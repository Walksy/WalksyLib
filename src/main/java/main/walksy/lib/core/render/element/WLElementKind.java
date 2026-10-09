package main.walksy.lib.core.render.element;

import main.walksy.lib.core.render.gpu.WLVertexFormat;

public enum WLElementKind {
    FLAT(WLVertexFormat.POSITION_COLOR),
    INVERT(WLVertexFormat.POSITION_COLOR),
    TEXTURED(WLVertexFormat.POSITION_TEX_COLOR),
    PREMULTIPLIED(WLVertexFormat.POSITION_TEX_COLOR),
    GRAYSCALE(WLVertexFormat.POSITION_TEX_COLOR),
    ROUNDED_TEXTURED(WLVertexFormat.POSITION_TEX_COLOR),
    CORNER(WLVertexFormat.POSITION_TEX_COLOR),
    CHECKER(WLVertexFormat.POSITION_TEX_COLOR);

    private final WLVertexFormat format;

    WLElementKind(final WLVertexFormat format) {
        this.format = format;
    }

    public WLVertexFormat format() {
        return this.format;
    }

    public boolean textured() {
        return this == TEXTURED || this == PREMULTIPLIED || this == GRAYSCALE || this == ROUNDED_TEXTURED;
    }
}
