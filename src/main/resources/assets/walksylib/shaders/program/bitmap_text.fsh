#version 150

uniform sampler2D Sampler0;

in vec2 vTexCoord;
in vec4 vColor;

out vec4 FragColor;

void main() {
    float coverage = texture(Sampler0, vTexCoord).r;
    if (coverage <= 0.001) discard;
    FragColor = vec4(vColor.rgb, vColor.a * coverage);
}
