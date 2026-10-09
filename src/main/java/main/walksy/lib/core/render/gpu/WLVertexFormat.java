package main.walksy.lib.core.render.gpu;

public enum WLVertexFormat {
    POSITION(12),
    POSITION_COLOR(16),
    POSITION_TEX_COLOR(24);

    private final int bytesPerVertex;

    WLVertexFormat(final int bytesPerVertex) {
        this.bytesPerVertex = bytesPerVertex;
    }

    public int bytesPerVertex() {
        return this.bytesPerVertex;
    }

    public boolean hasUv() {
        return this == POSITION_TEX_COLOR;
    }

    public boolean hasColor() {
        return this != POSITION;
    }
}
