#version 150

uniform sampler2D Sampler0;

in vec2 vTexCoord;
in vec4 vColor;

out vec4 FragColor;

void main() {
    vec2 texSize = vec2(textureSize(Sampler0, 0));
    vec2 texel = vTexCoord * texSize;
    vec2 seam = floor(texel + 0.5);
    vec2 span = max(fwidth(texel), vec2(1e-6));
    texel = seam + clamp((texel - seam) / span, -0.5, 0.5);
    float coverage = texture(Sampler0, texel / texSize).r;
    if (coverage <= 0.001) discard;
    FragColor = vec4(vColor.rgb, vColor.a * coverage);
}
