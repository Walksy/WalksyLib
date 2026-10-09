#version 150

in vec2 vLocal;
in vec4 vColor;

out vec4 fragColor;

void main() {
    float dist = length(vLocal) - 1.0;
    float aa = max(fwidth(dist), 1e-6);
    float coverage = clamp(0.5 - dist / aa, 0.0, 1.0);
    fragColor = vec4(vColor.rgb, vColor.a * coverage);
}
