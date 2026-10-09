#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;

layout(std140) uniform Projection {
    mat4 ProjMat;
};

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
};

out vec2 vLocal;
out vec4 vColor;

void main() {
    vLocal = UV0;
    vColor = Color;
    gl_Position = ProjMat * ModelViewMat * vec4(Position.xy, 0.0, 1.0);
}
