#version 150

in vec3 Position;
in vec4 Color;

layout(std140) uniform Projection {
    mat4 ProjMat;
};

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
};

out vec4 vColor;

void main() {
    vColor = Color;
    gl_Position = ProjMat * ModelViewMat * vec4(Position.xy, 0.0, 1.0);
}
