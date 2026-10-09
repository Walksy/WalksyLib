package main.walksy.lib.core.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

@FunctionalInterface
public interface WLGeometry {
    void emit(PoseStack matrices, VertexConsumer buffer);
}
