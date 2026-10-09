#version 150
#ifdef WALKSYLIB_VULKANMOD
#define WL_FRAG_COORD(height) vec2(gl_FragCoord.x, (height) - gl_FragCoord.y)
#define WL_UV(uv) vec2((uv).x, 1.0 - (uv).y)
#else
#define WL_FRAG_COORD(height) gl_FragCoord.xy
#define WL_UV(uv) (uv)
#endif

uniform sampler2D Sampler0;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform BgConfig {
    float ShadowExpand;
    float ShadowFactor;
    vec2 ShadowOffset;
};

#define MAX_WIDGETS 64
#define WL_WIDGETINFO_ARRAYS Rects[MAX_WIDGETS]; vec4 Rads[MAX_WIDGETS]; vec4 Tints[MAX_WIDGETS]; vec4 Optics0[MAX_WIDGETS]; vec4 Optics1[MAX_WIDGETS]; vec4 Optics2[MAX_WIDGETS]; vec4 Smoothings[MAX_WIDGETS]; vec4 ScissorRects[MAX_WIDGETS]; }; struct WidgetInfoVulkanModSize { vec4 arrays
layout(std140) uniform WidgetInfo {
    float Count;
    vec4 WL_WIDGETINFO_ARRAYS;
    mat4 pad0; mat4 pad1; mat4 pad2; mat4 pad3; mat4 pad4; mat4 pad5; mat4 pad6; mat4 pad7;
    mat4 pad8; mat4 pad9; mat4 pad10; mat4 pad11; mat4 pad12; mat4 pad13; mat4 pad14; mat4 pad15;
    mat4 pad16; mat4 pad17; mat4 pad18; mat4 pad19; mat4 pad20; mat4 pad21; mat4 pad22; mat4 pad23;
    mat4 pad24; mat4 pad25; mat4 pad26; mat4 pad27; mat4 pad28; mat4 pad29; mat4 pad30; mat4 pad31;
    mat4 pad32; mat4 pad33; mat4 pad34; mat4 pad35; mat4 pad36; mat4 pad37; mat4 pad38; mat4 pad39;
    mat4 pad40; mat4 pad41; mat4 pad42; mat4 pad43; mat4 pad44; mat4 pad45; mat4 pad46; mat4 pad47;
    mat4 pad48; mat4 pad49; mat4 pad50; mat4 pad51; mat4 pad52; mat4 pad53; mat4 pad54; mat4 pad55;
    mat4 pad56; mat4 pad57; mat4 pad58; mat4 pad59; mat4 pad60; mat4 pad61; mat4 pad62; mat4 pad63;
    mat4 pad64; mat4 pad65; mat4 pad66; mat4 pad67; mat4 pad68; mat4 pad69; mat4 pad70; mat4 pad71;
    mat4 pad72; mat4 pad73; mat4 pad74; mat4 pad75; mat4 pad76; mat4 pad77; mat4 pad78; mat4 pad79;
    mat4 pad80; mat4 pad81; mat4 pad82; mat4 pad83; mat4 pad84; mat4 pad85; mat4 pad86; mat4 pad87;
    mat4 pad88; mat4 pad89; mat4 pad90; mat4 pad91; mat4 pad92; mat4 pad93; mat4 pad94; mat4 pad95;
    mat4 pad96; mat4 pad97; mat4 pad98; mat4 pad99; mat4 pad100; mat4 pad101; mat4 pad102; mat4 pad103;
    mat4 pad104; mat4 pad105; mat4 pad106; mat4 pad107; mat4 pad108; mat4 pad109; mat4 pad110; mat4 pad111;
    mat4 pad112; mat4 pad113; mat4 pad114; mat4 pad115; mat4 pad116; mat4 pad117; mat4 pad118; mat4 pad119;
    mat4 pad120; mat4 pad121; mat4 pad122; mat4 pad123; mat4 pad124; mat4 pad125; mat4 pad126; vec4 pad127;
    vec4 pad128; vec4 pad129;
};

out vec4 fragColor;

vec4 wlSample(sampler2D s, vec2 uv) {
    return texture(s, WL_UV(uv));
}

#define PI 3.141592653589793

struct SDFResult {
    float dist;
    vec2 normal;
    float aspect;
    int index;
};

float lerp01(float a, float b, float x) {
    return clamp((x - a) / (b - a), 0.0, 1.0);
}

vec2 screenToUV(vec2 screen, vec2 res) {
    return (screen.xy - 0.5 * res.xy) / res.y;
}

vec3 sdgBox(vec2 p, vec2 b, vec4 ra) {
    ra.xy = (p.x > 0.0) ? ra.xy : ra.zw;
    float r = (p.y > 0.0) ? ra.x : ra.y;
    vec2 w = abs(p) - (b - r);
    vec2 s = vec2(p.x < 0.0 ? -1 : 1, p.y < 0.0 ? -1 : 1);
    float g = max(w.x, w.y);
    vec2 q = max(w, 0.0);
    float l = length(q);
    float dist = (g > 0.0) ? l - r : g - r;
    vec2 n = (g > 0.0) ? (q / max(l, 1e-6)) : ((w.x > w.y) ? vec2(1, 0) : vec2(0, 1));
    return vec3(dist, s * n);
}

SDFResult opSmoothUnion(SDFResult a, SDFResult b, float k) {
    if (k == 0.0) return (a.dist < b.dist) ? a : b;
    float h = clamp(0.5 + 0.5 * (a.dist - b.dist) / k, 0.0, 1.0);
    float d = mix(a.dist, b.dist, h) - k * h * (1.0 - h);
    vec2 n = normalize(mix(a.normal, b.normal, h));
    float aspect = mix(a.aspect, b.aspect, h);
    int index = (a.dist < b.dist) ? a.index : b.index;
    return SDFResult(d, n, aspect, index);
}

SDFResult opHardUnion(SDFResult a, SDFResult b) {
    return (a.dist < b.dist) ? a : b;
}

SDFResult opHardSubtract(SDFResult a, SDFResult b) {
    float d = max(a.dist, -b.dist);
    if (d == a.dist) return a;
    return SDFResult(d, -b.normal, a.aspect, a.index);
}

SDFResult fieldWidgets(vec2 p, vec2 inSize) {
    int n = int(Count + 0.5);
    if (n == 0) return SDFResult(1e6, vec2(0.0), 1.0, -1);

    SDFResult posShapes = SDFResult(1e6, vec2(0.0), 1.0, -1);
    bool hasPos = false;
    vec2 fragCoord = WL_FRAG_COORD(OutSize.y > 0.0 ? OutSize.y : inSize.y) + ShadowOffset;

    float shadowReach = max(ShadowExpand, 0.0) * 3.0;

    for (int i = 0; i < MAX_WIDGETS; i++) {
        if (i >= n) break;
        if (Smoothings[i].x < 0.0) continue;

        vec4 sc = ScissorRects[i];
        vec4 rc = Rects[i];
        bool bodyClipped = rc.x < sc.x - 0.5 || rc.y < sc.y - 0.5 || rc.x + rc.z > sc.z + 0.5 || rc.y + rc.w > sc.w + 0.5;
        float shadowBleed = bodyClipped ? 0.0 : shadowReach;
        if (fragCoord.x < sc.x - shadowBleed || fragCoord.y < sc.y - shadowBleed || fragCoord.x > sc.z + shadowBleed || fragCoord.y > sc.w + shadowBleed) continue;

        vec4 rr = Rads[i];
        vec2 cPx = vec2(rc.x + 0.5 * rc.z, rc.y + 0.5 * rc.w);
        vec2 c = screenToUV(cPx, inSize);
        vec2 b = 0.5 * vec2(rc.z, rc.w) / inSize.y;
        vec4 rad = rr / inSize.y;
        vec3 gvec = sdgBox(p - c, b, rad);
        float aspect = min(rc.z, rc.w) / max(rc.z, rc.w);
        SDFResult g = SDFResult(gvec.x, gvec.yz, aspect, i);

        if (!hasPos) {
            posShapes = g; hasPos = true;
        } else {
            posShapes = opSmoothUnion(posShapes, g, Smoothings[i].x);
        }
    }

    SDFResult f = posShapes;

    for (int i = 0; i < MAX_WIDGETS; i++) {
        if (i >= n) break;
        if (Smoothings[i].x >= 0.0) continue;

        vec4 sc = ScissorRects[i];
        vec4 rc = Rects[i];
        bool bodyClipped = rc.x < sc.x - 0.5 || rc.y < sc.y - 0.5 || rc.x + rc.z > sc.z + 0.5 || rc.y + rc.w > sc.w + 0.5;
        float shadowBleed = bodyClipped ? 0.0 : shadowReach;
        if (fragCoord.x < sc.x - shadowBleed || fragCoord.y < sc.y - shadowBleed || fragCoord.x > sc.z + shadowBleed || fragCoord.y > sc.w + shadowBleed) continue;

        vec4 rr = Rads[i];
        vec2 cPx = vec2(rc.x + 0.5 * rc.z, rc.y + 0.5 * rc.w);
        vec2 c = screenToUV(cPx, inSize);
        vec2 b = 0.5 * vec2(rc.z, rc.w) / inSize.y;
        vec4 rad = rr / inSize.y;
        vec3 gvec = sdgBox(p - c, b, rad);
        float aspect = min(rc.z, rc.w) / max(rc.z, rc.w);
        SDFResult g = SDFResult(gvec.x, gvec.yz, aspect, i);

        float rep = -Smoothings[i].x;
        SDFResult ge = SDFResult(g.dist - rep, g.normal, g.aspect, g.index);

        f = opHardSubtract(f, ge);
        f = opHardUnion(f, g);
    }

    return f;
}

void main() {
    vec2 inSize = InSize;
    if (inSize.x <= 0.0 || inSize.y <= 0.0) {
        inSize = vec2(textureSize(Sampler0, 0));
    }

    vec2 coord = WL_FRAG_COORD(OutSize.y > 0.0 ? OutSize.y : inSize.y);
    vec2 UV = coord / inSize;
    vec2 p = screenToUV(coord + ShadowOffset, inSize);

    vec3 base = wlSample(Sampler0, UV).rgb;

    SDFResult f = fieldWidgets(p, inSize);

    float dist = f.dist;
    float shadow = exp(-abs(dist) * inSize.y / max(ShadowExpand, 1e-4)) * 0.6 * ShadowFactor;

    vec3 col = base - vec3(shadow);
    fragColor = vec4(col, 1.0);
}
