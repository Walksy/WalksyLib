#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;

layout(std140) uniform Projection {
    mat4 ProjMat;
};

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
};

layout(location = 1) out vec4 vColor;

void main() {
    vColor = Color;
    gl_Position = ProjMat * ModelViewMat * vec4(Position.xy, 0.0, 1.0);
}
