#version 150

uniform sampler2D Sampler0;

in vec2 vTexCoord;
in vec4 vColor;

out vec4 FragColor;

const int TAPS = 3;

void main() {
    vec2 dx = dFdx(vTexCoord);
    vec2 dy = dFdy(vTexCoord);
    float coverage = 0.0;
    for (int j = 0; j < TAPS; j++) {
        for (int i = 0; i < TAPS; i++) {
            vec2 offset = ((float(i) + 0.5) / float(TAPS) - 0.5) * dx + ((float(j) + 0.5) / float(TAPS) - 0.5) * dy;
            coverage += texture(Sampler0, vTexCoord + offset).r;
        }
    }
    coverage /= float(TAPS * TAPS);
    if (coverage <= 0.001) discard;
    FragColor = vec4(vColor.rgb, vColor.a * coverage);
}
