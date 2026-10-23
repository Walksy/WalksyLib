#version 150

uniform sampler2D DiffuseSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

#define WL_CONFIG_ARRAYS Weights[257]; }; struct ConfigVulkanModSize { vec4 arrays
layout(std140) uniform Config {
    vec4 Params;
    vec4 WL_CONFIG_ARRAYS;
    mat4 pad0; mat4 pad1; mat4 pad2; mat4 pad3; mat4 pad4; mat4 pad5; mat4 pad6; mat4 pad7;
    mat4 pad8; mat4 pad9; mat4 pad10; mat4 pad11; mat4 pad12; mat4 pad13; mat4 pad14; mat4 pad15;
    mat4 pad16; mat4 pad17; mat4 pad18; mat4 pad19; mat4 pad20; mat4 pad21; mat4 pad22; mat4 pad23;
    mat4 pad24; mat4 pad25; mat4 pad26; mat4 pad27; mat4 pad28; mat4 pad29; mat4 pad30; mat4 pad31;
    mat4 pad32; mat4 pad33; mat4 pad34; mat4 pad35; mat4 pad36; mat4 pad37; mat4 pad38; mat4 pad39;
    mat4 pad40; mat4 pad41; mat4 pad42; mat4 pad43; mat4 pad44; mat4 pad45; mat4 pad46; mat4 pad47;
    mat4 pad48; mat4 pad49; mat4 pad50; mat4 pad51; mat4 pad52; mat4 pad53; mat4 pad54; mat4 pad55;
    mat4 pad56; mat4 pad57; mat4 pad58; mat4 pad59; mat4 pad60; mat4 pad61; mat4 pad62; mat4 pad63;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    int taps = int(Params.z + 0.5);
    taps = clamp(taps, 0, 256);

    if (Params.w > 0.5) {
        vec2 texel = 1.0 / OutSize;
        vec4 grid = vec4(0.0);
        for (int j = -taps; j <= taps; ++j) {
            int aj = abs(j);
            float wy = Weights[aj].x;
            float oy = (j < 0 ? -Weights[aj].y : Weights[aj].y) * texel.y;
            for (int i = -taps; i <= taps; ++i) {
                int ai = abs(i);
                float ox = (i < 0 ? -Weights[ai].y : Weights[ai].y) * texel.x;
                grid += texture(DiffuseSampler, texCoord + vec2(ox, oy)) * (Weights[ai].x * wy);
            }
        }
        fragColor = grid;
        return;
    }

    vec2 delta = Params.xy / OutSize;

    vec4 sum = texture(DiffuseSampler, texCoord) * Weights[0].x;

    for (int i = 1; i <= taps; ++i) {
        float w = Weights[i].x;
        vec2 offs = delta * Weights[i].y;
        sum += texture(DiffuseSampler, texCoord + offs) * w;
        sum += texture(DiffuseSampler, texCoord - offs) * w;
    }

    fragColor = sum;
}
