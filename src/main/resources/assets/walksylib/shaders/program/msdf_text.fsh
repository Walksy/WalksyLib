#version 150

uniform sampler2D Sampler0;

layout(std140) uniform MsdfParams {
    vec4 Params0;
    vec4 Params1;
    vec4 OutlineColor;
};

in vec2 vTexCoord;
in vec4 vColor;

out vec4 FragColor;

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

void main() {
    float onEdge         = Params0.x;
    float distanceRange  = Params0.y;
    float thickness      = Params0.z;
    float smoothness     = Params0.w;
    float outlineThick   = Params1.x;
    bool  outlineEnabled = Params1.y > 0.5;
    bool  isMsdf         = Params1.z > 0.5;

    vec2 texSize = vec2(textureSize(Sampler0, 0));
    vec2 screenTexSize = vec2(1.0) / fwidth(vTexCoord);
    vec2 unitRange = vec2(distanceRange) / texSize;
    float screenPxRange = max(0.5 * dot(unitRange, screenTexSize), 1.0);

    vec3 msd = texture(Sampler0, vTexCoord).rgb;

    float dist = (isMsdf ? median(msd.r, msd.g, msd.b) : msd.r) - onEdge + thickness;
    float screenPxDist = dist * screenPxRange;

    float fillAlpha  = smoothstep(-smoothness, smoothness, screenPxDist);
    vec4  fillColor  = vec4(vColor.rgb, vColor.a * fillAlpha);

    if (outlineEnabled) {
        float outlineAlpha = smoothstep(-smoothness, smoothness, (dist + outlineThick) * screenPxRange);
        vec4 oColor  = vec4(OutlineColor.rgb, OutlineColor.a * outlineAlpha);
        FragColor    = mix(oColor, fillColor, fillAlpha);
        FragColor.a  = max(fillAlpha, outlineAlpha) * vColor.a;
    } else {
        FragColor = fillColor;
    }
}
