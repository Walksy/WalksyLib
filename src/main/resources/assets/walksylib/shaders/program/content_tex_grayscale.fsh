#version 150

uniform sampler2D Sampler0;

in vec2 vTexCoord;
in vec4 vColor;

out vec4 fragColor;

void main() {
    fragColor = vec4(1.0, 1.0, 1.0, texture(Sampler0, vTexCoord).r) * vColor;
}
