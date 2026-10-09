package main.walksy.lib.core.render.element;

import org.joml.Matrix3x2fc;

public interface WLVertexSink {
    WLVertexSink vertex(float x, float y);

    WLVertexSink uv(float u, float v);

    WLVertexSink color(int argb);

    default WLVertexSink posed(final Matrix3x2fc pose, final float x, final float y) {
        return this.vertex(pose.m00() * x + pose.m10() * y + pose.m20(), pose.m01() * x + pose.m11() * y + pose.m21());
    }
}
