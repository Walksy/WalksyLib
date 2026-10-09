#version 150

uniform sampler2D Sampler0;

in vec2 vTexCoord;
in vec4 vColor;

out vec4 fragColor;

void main() {
    float dist = length(vColor.rg) - 1.0;
    float aa = max(fwidth(dist), 1e-6);
    float coverage = clamp(0.5 - dist / aa, 0.0, 1.0);
    fragColor = texture(Sampler0, vTexCoord) * vec4(1.0, 1.0, 1.0, vColor.a * coverage);
}
