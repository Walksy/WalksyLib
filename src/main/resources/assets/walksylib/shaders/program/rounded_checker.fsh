#version 150

in vec2 vLocal;
in vec4 vColor;

out vec4 fragColor;

const vec3 DARK = vec3(0.7058824, 0.7058824, 0.7058824);
const vec3 LIGHT = vec3(0.9098039, 0.9098039, 0.9098039);

void main() {
    float dist = length(vLocal) - 1.0;
    float aa = max(fwidth(dist), 1e-6);
    float coverage = clamp(0.5 - dist / aa, 0.0, 1.0);
    bool darkCell = (vLocal.x >= 0.0) != (vLocal.y >= 0.0);
    vec3 col = darkCell ? DARK : LIGHT;
    fragColor = vec4(col, vColor.a * coverage);
}
