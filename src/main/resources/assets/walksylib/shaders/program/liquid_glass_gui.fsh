#version 150
#ifdef WALKSYLIB_VULKANMOD
#define WL_FRAG_COORD(height) vec2(gl_FragCoord.x, (height) - gl_FragCoord.y)
#define WL_UV(uv) vec2((uv).x, 1.0 - (uv).y)
#else
#define WL_FRAG_COORD(height) gl_FragCoord.xy
#define WL_UV(uv) (uv)
#endif
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform sampler2D Sampler3;
uniform sampler2D Sampler4;
uniform sampler2D Sampler5;

uniform sampler2D Sampler6;

uniform sampler2D Sampler7;

uniform sampler2D Sampler8;

uniform sampler2D Sampler9;

uniform sampler2D Sampler10;

uniform sampler2D Sampler11;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform CustomUniforms {
    float Time;
    vec4 Mouse;
    float ScreenWantsBlur;
    vec3 RIM_LIGHT_VEC;
    vec4 RIM_LIGHT_COLOR;
    float EPS_PIX;
    float DebugStep;
    float Pixelated;
    float PixelGridSize;
    float HoverScalePx;
    float FocusScalePx;
    float FocusBorderWidthPx;
    float FocusBorderIntensity;
    float FocusBorderSpeed;

    float PopoverContentBoundary;

    float ActiveSlot;

    float CompositeTextStart;
    float CompositeContentStart;
    float CompositePopoverStart;
    float CompositeTextEnd;
    float CompositeContentEnd;
    float CompositePopoverEnd;

    float TextInCapture;
    float ContentInCapture;
    float PopoverInCapture;
};

#define MAX_WIDGETS 64
#define WL_WIDGETINFO_ARRAYS LayerRanges[24]; vec4 Rects[MAX_WIDGETS]; vec4 Rads[MAX_WIDGETS]; vec4 Tints[MAX_WIDGETS]; vec4 Optics0[MAX_WIDGETS]; vec4 Optics1[MAX_WIDGETS]; vec4 Optics2[MAX_WIDGETS]; vec4 Smoothings[MAX_WIDGETS]; vec4 ScissorRects[MAX_WIDGETS]; vec4 Shadow0[MAX_WIDGETS]; vec4 ShadowColor[MAX_WIDGETS]; vec4 Extra0[MAX_WIDGETS]; vec4 Extra1[MAX_WIDGETS]; vec4 Extra2[MAX_WIDGETS]; vec4 Extra3[MAX_WIDGETS]; vec4 Extra4[MAX_WIDGETS]; vec4 Extra5[MAX_WIDGETS]; }; struct WidgetInfoVulkanModSize { vec4 arrays
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
    mat4 pad120; mat4 pad121; mat4 pad122; mat4 pad123; mat4 pad124; mat4 pad125; mat4 pad126; mat4 pad127;
    mat4 pad128; mat4 pad129; mat4 pad130; mat4 pad131; mat4 pad132; mat4 pad133; mat4 pad134; mat4 pad135;
    mat4 pad136; mat4 pad137; mat4 pad138; mat4 pad139; mat4 pad140; mat4 pad141; mat4 pad142; mat4 pad143;
    mat4 pad144; mat4 pad145; mat4 pad146; mat4 pad147; mat4 pad148; mat4 pad149; mat4 pad150; mat4 pad151;
    mat4 pad152; mat4 pad153; mat4 pad154; mat4 pad155; mat4 pad156; mat4 pad157; mat4 pad158; mat4 pad159;
    mat4 pad160; mat4 pad161; mat4 pad162; mat4 pad163; mat4 pad164; mat4 pad165; mat4 pad166; mat4 pad167;
    mat4 pad168; mat4 pad169; mat4 pad170; mat4 pad171; mat4 pad172; mat4 pad173; mat4 pad174; mat4 pad175;
    mat4 pad176; mat4 pad177; mat4 pad178; mat4 pad179; mat4 pad180; mat4 pad181; mat4 pad182; mat4 pad183;
    mat4 pad184; mat4 pad185; mat4 pad186; mat4 pad187; mat4 pad188; mat4 pad189; mat4 pad190; mat4 pad191;
    mat4 pad192; mat4 pad193; mat4 pad194; mat4 pad195; mat4 pad196; mat4 pad197; mat4 pad198; mat4 pad199;
    mat4 pad200; mat4 pad201; mat4 pad202; mat4 pad203; mat4 pad204; mat4 pad205; mat4 pad206; mat4 pad207;
    mat4 pad208; mat4 pad209; mat4 pad210; mat4 pad211; mat4 pad212; mat4 pad213; mat4 pad214; mat4 pad215;
    mat4 pad216; mat4 pad217; mat4 pad218; mat4 pad219; mat4 pad220; mat4 pad221; mat4 pad222; mat4 pad223;
    mat4 pad224; mat4 pad225; mat4 pad226; mat4 pad227; mat4 pad228; mat4 pad229; mat4 pad230; mat4 pad231;
    mat4 pad232; mat4 pad233; mat4 pad234; mat4 pad235; mat4 pad236; mat4 pad237; mat4 pad238; mat4 pad239;
    mat4 pad240; mat4 pad241; mat4 pad242; mat4 pad243; mat4 pad244; mat4 pad245; mat4 pad246; mat4 pad247;
    mat4 pad248; mat4 pad249; mat4 pad250; mat4 pad251; mat4 pad252; mat4 pad253; mat4 pad254; mat4 pad255;
    mat4 pad256; mat4 pad257; mat4 pad258; mat4 pad259; mat4 pad260; vec4 pad261; vec4 pad262; vec4 pad263;
};

const int RIM_TAPS_MAX = 4;

const float BRIGHT_EDGE_WIDTH = 0.7;
const float BRIGHT_EDGE_GAIN = 1.5;
const float BRIGHT_EDGE_LIFT = 0.18;
const float BRIGHT_EDGE_MIX = 0.62;

const float BRIGHT_EDGE_REF_PX = 44.0;
const float BRIGHT_EDGE_MIN_SCALE = 0.4;
const float BRIGHT_EDGE_LUMA_LO = 0.10;
const float BRIGHT_EDGE_LUMA_HI = 0.55;
const float BRIGHT_EDGE_FLOOR = 0.90;
const float BRIGHT_EDGE_CONTRAST = 0.55;

const float OUTLINE_LIT_REACH_PX = 9.0;
const float OUTLINE_LIT_REACH_FRAC = 0.45;
const float OUTLINE_LIT_EXPOSURE = 0.85;
const float OUTLINE_LIT_WHITE = 0.25;

const float SIDE_EDGE_WIDTH = 1.1;
const float SIDE_EDGE_DEPTH = 0.30;
const float SIDE_EDGE_MIX = 0.85;

const float INNER_INSET = 7.0;
const float INNER_WIDTH = 4.0;

const float INNER_INSET_FRAC = 0.45;

const float INNER_TOP_SCALE = 0.18;

layout(std140) uniform BgConfig {
    float ShadowExpand;
    float ShadowFactor;
    vec2 ShadowOffset;
};

out vec4 fragColor;

vec4 wlSample(sampler2D s, vec2 uv) {
    return texture(s, WL_UV(uv));
}

struct SDFResult { float dist; vec2 normal; float aspect; int index; };

vec2 screenToUV(vec2 screen, vec2 res) {
    return (screen.xy - 0.5 * res.xy) / res.y;
}

float lineGaussAA(float dPx, float width) {
    float w = max(width, 1e-3);
    float a = (dPx - 0.375) / w;
    float b = (dPx - 0.125) / w;
    float c = (dPx + 0.125) / w;
    float d = (dPx + 0.375) / w;
    return 0.25 * (exp(-a * a) + exp(-b * b) + exp(-c * c) + exp(-d * d));
}

float softFloor(float x, float lo, float knee) {
    float d = x - lo;
    return lo + 0.5 * (d + sqrt(d * d + knee * knee));
}

float softCeil(float x, float hi, float knee) {
    float d = hi - x;
    return hi - 0.5 * (d + sqrt(d * d + knee * knee));
}

vec3 sdgBox(vec2 p, vec2 b, vec4 ra) {
    ra.xy = (p.x > 0.0) ? ra.xy : ra.zw;
    float r = (p.y > 0.0) ? ra.x : ra.y;
    vec2 w = abs(p) - (b - r);
    vec2 s = vec2(p.x < 0.0 ? -1.0 : 1.0, p.y < 0.0 ? -1.0 : 1.0);
    float g = max(w.x, w.y);
    vec2 q = max(w, 0.0);
    float l = length(q);
    float dist = (g > 0.0) ? l - r : g - r;
    vec2 n = (g > 0.0) ? (q / max(l, 1e-6)) : (w.x > w.y ? vec2(1.0, 0.0) : vec2(0.0, 1.0));
    return vec3(dist, s * n);
}

const float STRETCH_MAX_FRACTION = 0.6;
const float STRETCH_THIN = 0.3;

vec3 stretchedBox(vec2 p, vec2 b, vec4 rad, vec2 pull) {
    float raw = length(pull);
    if (raw <= 0.0) return sdgBox(p, b, rad);
    vec2 n = pull / raw;
    vec2 m = vec2(-n.y, n.x);
    float len = min(raw, STRETCH_MAX_FRACTION * min(b.x, b.y));
    float along = max(abs(n.x) * b.x + abs(n.y) * b.y, 1e-6);
    float across = max(abs(m.x) * b.x + abs(m.y) * b.y, 1e-6);
    float sa = 1.0 + len / (2.0 * along);
    float sp = 1.0 - STRETCH_THIN * (sa - 1.0) * min(1.0, along / across);
    vec2 q = p - n * (0.5 * len);
    vec3 g = sdgBox(n * (dot(q, n) / sa) + m * (dot(q, m) / sp), b, rad);
    vec2 grad = n * (dot(g.yz, n) / sa) + m * (dot(g.yz, m) / sp);
    float gl = length(grad);
    if (gl < 1e-6) return g;
    return vec3(g.x / gl, grad / gl);
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

SDFResult opHardUnion(SDFResult a, SDFResult b) { return (a.dist < b.dist) ? a : b; }

SDFResult opHardSubtract(SDFResult a, SDFResult b) {
    float d = max(a.dist, -b.dist);
    if (d == a.dist) return a;
    return SDFResult(d, -b.normal, a.aspect, a.index);
}

float gFrostRamp = 1.0;

bool ramping() { return gFrostRamp < 0.999; }

float contentSurvival() { return 1.0 - gFrostRamp; }

vec4 sampleBlur(int idx, vec2 uv) {
    if (idx < 0) return wlSample(Sampler0, uv);
    vec4 b;
    if (idx <= 0) b = wlSample(Sampler2, uv);
    else if (idx == 1) b = wlSample(Sampler3, uv);
    else if (idx == 2) b = wlSample(Sampler4, uv);
    else b = wlSample(Sampler5, uv);

    if (ramping()) b = mix(wlSample(Sampler0, uv), b, gFrostRamp);
    return b;
}

const int EARLY_TEXT_LAYER = 20;

const float BLUR_FALLOFF_BIAS = 8.0;

const float EDGE_REACH = 26.0;

const int SLOT_HAS_REPULSION = 1;
const int SLOT_HAS_SHADOW = 2;

const int CONTENT_LAYER = 15;

vec4 sampleBackdrop(int idx, vec2 uv, bool overText, bool overContent, bool overPopoverContent) {
    vec4 c = sampleBlur(idx, uv);

    if (overText) {
        vec4 t = wlSample(Sampler11, uv);
        if (ramping()) {
            t = mix(wlSample(Sampler6, uv), t, gFrostRamp);

            t *= contentSurvival();
        }
        c.rgb = c.rgb * (1.0 - t.a) + t.rgb;
    }

    if (overContent) {
        vec4 rc = wlSample(Sampler8, uv);
        if (ramping()) {
            rc = mix(wlSample(Sampler7, uv), rc, gFrostRamp);

            rc *= contentSurvival();
        }
        c.rgb = c.rgb * (1.0 - rc.a) + rc.rgb;
    }

    if (overPopoverContent) {
        vec4 pc = wlSample(Sampler10, uv);
        if (ramping()) {
            pc = mix(wlSample(Sampler9, uv), pc, gFrostRamp);

            pc *= contentSurvival();
        }
        c.rgb = c.rgb * (1.0 - pc.a) + pc.rgb;
    }
    return c;
}
vec4 sampleBackdropFiltered(int idx, vec2 uv, vec2 tapStep, int taps, bool overText, bool overContent, bool overPopoverContent) {
    if (taps <= 1) return sampleBackdrop(idx, uv, overText, overContent, overPopoverContent);
    vec4 acc = vec4(0.0);
    float centre = 0.5 * float(taps - 1);
    for (int i = 0; i < RIM_TAPS_MAX; i++) {
        if (i >= taps) break;
        acc += sampleBackdrop(idx, uv + (float(i) - centre) * tapStep, overText, overContent, overPopoverContent);
    }
    return acc / float(taps);
}

float rimEdgeFactor(float n, float thickness, float refFactor) {
    if (n >= thickness) return 0.0;
    float xR = clamp(1.0 - n / max(thickness, 1e-6), 0.0, 1.0);
    float thetaI = asin(xR * xR);
    float thetaT = asin((1.0 / max(refFactor, 1e-6)) * sin(thetaI));
    return -tan(thetaT - thetaI);
}

SDFResult fieldWidgets(vec2 p, vec2 inSize, vec2 fragCoord, float layer) {
    int n = int(Count + 0.5);
    if (n == 0) return SDFResult(1e6, vec2(0.0), 1.0, -1);
    int layerIndex = int(layer + 0.5);

    if (layerIndex < 0 || layerIndex >= 24) return SDFResult(1e6, vec2(0.0), 1.0, -1);
    vec4 layerRange = LayerRanges[layerIndex];
    int start = int(layerRange.x + 0.5);
    int count = int(layerRange.y + 0.5);
    if (count <= 0) return SDFResult(1e6, vec2(0.0), 1.0, -1);

    SDFResult pos = SDFResult(1e6, vec2(0.0), 1.0, -1);
    bool hasPos = false;

    for (int i = 0; i < MAX_WIDGETS; i++) {
        if (i >= count) break;
        int wi = start + i;
        if (wi >= n) break;
        if (Smoothings[wi].x < 0.0) continue;

        vec4 sc = ScissorRects[wi];
        if (fragCoord.x < sc.x || fragCoord.y < sc.y || fragCoord.x > sc.z || fragCoord.y > sc.w) continue;

        vec4 rc = Rects[wi];
        vec4 rr = Rads[wi];
        vec2 cPx = vec2(rc.x + 0.5 * rc.z, rc.y + 0.5 * rc.w);
        vec2 c = screenToUV(cPx, inSize);
        vec2 b = 0.5 * vec2(rc.z, rc.w) / inSize.y;
        vec4 rad = rr / inSize.y;

        vec3 g = stretchedBox(p - c, b, rad, Extra5[wi].zw / inSize.y);

        vec4 extra = Extra0[wi];
        float scaleOff = (HoverScalePx * extra.y + FocusScalePx * extra.z) / inSize.y;
        float dist = g.x - scaleOff;

        float aspect = min(rc.z, rc.w) / max(rc.z, rc.w);
        SDFResult s = SDFResult(dist, g.yz, aspect, wi);

        if (!hasPos) { pos = s; hasPos = true; }
        else { pos = opSmoothUnion(pos, s, Smoothings[wi].x); }
    }

    SDFResult f = pos;

    if ((int(layerRange.w + 0.5) & SLOT_HAS_REPULSION) == 0) return f;

    for (int i = 0; i < MAX_WIDGETS; i++) {
        if (i >= count) break;
        int wi = start + i;
        if (wi >= n) break;
        if (Smoothings[wi].x >= 0.0) continue;

        vec4 sc = ScissorRects[wi];
        if (fragCoord.x < sc.x || fragCoord.y < sc.y || fragCoord.x > sc.z || fragCoord.y > sc.w) continue;

        vec4 rc = Rects[wi];
        vec4 rr = Rads[wi];
        vec2 cPx = vec2(rc.x + 0.5 * rc.z, rc.y + 0.5 * rc.w);
        vec2 c = screenToUV(cPx, inSize);
        vec2 b = 0.5 * vec2(rc.z, rc.w) / inSize.y;
        vec4 rad = rr / inSize.y;

        vec3 g = stretchedBox(p - c, b, rad, Extra5[wi].zw / inSize.y);

        vec4 extra = Extra0[wi];
        float scaleOff = (HoverScalePx * extra.y + FocusScalePx * extra.z) / inSize.y;
        float dist = g.x - scaleOff;

        float aspect = min(rc.z, rc.w) / max(rc.z, rc.w);
        SDFResult s = SDFResult(dist, g.yz, aspect, wi);

        float repulsion = -Smoothings[wi].x;
        SDFResult se = SDFResult(s.dist - repulsion, s.normal, s.aspect, s.index);
        f = opHardSubtract(f, se);
        f = opHardUnion(f, s);
    }

    return f;
}

void main() {
    vec2 inSize = InSize;
    if (inSize.x <= 0.0 || inSize.y <= 0.0) inSize = vec2(textureSize(Sampler0, 0));

    vec2 coord = WL_FRAG_COORD(OutSize.y > 0.0 ? OutSize.y : inSize.y);
    vec2 uv = coord / inSize;
    vec3 base = (ScreenWantsBlur > 0.5 ? wlSample(Sampler1, uv) : wlSample(Sampler0, uv)).rgb;

    vec3 currentColor = base;
    int n = int(Count + 0.5);

    float earlyTextConsumed = 0.0;

    int STEP = int(DebugStep + 0.5);
    if (STEP <= 1) {
        float globalMerged = 1e6;
        for (int l = 0; l < 16; l++) {
            globalMerged = min(globalMerged, fieldWidgets(screenToUV(coord, inSize), inSize, coord, float(l)).dist);
        }
        vec3 col = (globalMerged > 0.0) ? vec3(0.9, 0.6, 0.3) : vec3(0.65, 0.85, 1.0);
        col *= 1.0 - exp(-0.03 * abs(globalMerged) * inSize.y);
        fragColor = vec4(col, 1.0);
        return;
    }

    int l = int(ActiveSlot + 0.5);
    float fl = float(l);
    vec4 layerRange = LayerRanges[l];
    int start = int(layerRange.x + 0.5);
    int count = int(layerRange.y + 0.5);

    if (CompositeTextStart > 0.5) {
        vec4 earlyText = wlSample(Sampler6, uv);
        currentColor = currentColor * (1.0 - earlyText.a) + earlyText.rgb;
    }
    if (CompositeContentStart > 0.5) {
        vec4 rowContent = wlSample(Sampler7, uv);
        currentColor = currentColor * (1.0 - rowContent.a) + rowContent.rgb;
    }
    if (CompositePopoverStart > 0.5) {
        vec4 popoverContent = wlSample(Sampler9, uv);
        currentColor = currentColor * (1.0 - popoverContent.a) + popoverContent.rgb;
    }

    if (count > 0) {

        if ((int(layerRange.w + 0.5) & SLOT_HAS_SHADOW) != 0) {
            for (int i = 0; i < MAX_WIDGETS; i++) {
                if (i >= count) break;
                int wi = start + i;
                if (wi >= n) break;

                float expandRaw = Shadow0[wi].x;
                bool outlineMode = expandRaw < 0.0;
                vec2 shOffset = outlineMode ? vec2(0.0, Shadow0[wi].w) : Shadow0[wi].zw;

                if (ShadowColor[wi].a <= 0.0 || Smoothings[wi].w <= 0.0 || (!outlineMode && Shadow0[wi].y == 0.0)) continue;

                vec4 sc = ScissorRects[wi];
                vec4 rc = Rects[wi];
                float shFalloff = max(outlineMode ? Shadow0[wi].z : expandRaw, 0.0);
                bool bodyClipped = rc.x < sc.x - 0.5 || rc.y < sc.y - 0.5 || rc.x + rc.z > sc.z + 0.5 || rc.y + rc.w > sc.w + 0.5;
                float shBleed = bodyClipped ? 0.0 : shFalloff * 3.0 + max(abs(shOffset.x), abs(shOffset.y));

                if (coord.x < sc.x - shBleed || coord.y < sc.y - shBleed || coord.x > sc.z + shBleed || coord.y > sc.w + shBleed) continue;

                vec4 rr = Rads[wi];
                vec2 cPx = vec2(rc.x + 0.5 * rc.z, rc.y + 0.5 * rc.w);

                vec2 pShadow = screenToUV(coord + shOffset, inSize);
                vec2 c = screenToUV(cPx, inSize);
                vec2 b = 0.5 * vec2(rc.z, rc.w) / inSize.y;
                vec4 rad = rr / inSize.y;

                vec4 hoverExtra = Extra0[wi];
                float shScaleOff = (HoverScalePx * hoverExtra.y + FocusScalePx * hoverExtra.z) / inSize.y;

                vec2 shPull = Extra5[wi].zw / inSize.y;
                vec3 g = stretchedBox(pShadow - c, b, rad, shPull);
                float factor = Shadow0[wi].y;
                vec3 scol = ShadowColor[wi].rgb;
                float dPx = abs(g.x - shScaleOff) * inSize.y;
                float sh;
                if (outlineMode) {

                    float w = max(-expandRaw, 1e-4);
                    vec3 gOutline = stretchedBox(screenToUV(coord, inSize) - c, b, rad, shPull);
                    float dOutline = abs(gOutline.x - shScaleOff) * inSize.y;
                    float outline = clamp(w - dOutline + 0.5, 0.0, 1.0);

                    vec3 outlineCol = scol;
                    if (factor < 0.0) {

                        vec2 nOutline = normalize(gOutline.yz + vec2(1e-6));

                        float litReach = min(OUTLINE_LIT_REACH_PX, OUTLINE_LIT_REACH_FRAC * min(rc.z, rc.w));
                        vec2 litUv = (coord - nOutline * litReach) / inSize;
                        vec3 litSrc = (ScreenWantsBlur > 0.5 ? wlSample(Sampler1, litUv) : wlSample(Sampler0, litUv)).rgb;

                        float litMax = max(max(litSrc.r, litSrc.g), litSrc.b);
                        vec3 litExposed = clamp(litSrc + (min(1.0, litMax + OUTLINE_LIT_EXPOSURE) - litMax), 0.0, 1.0);
                        vec3 litFace = mix(litExposed, vec3(1.0), OUTLINE_LIT_WHITE);
                        outlineCol = mix(litFace, scol, nOutline.x * nOutline.x);
                    }

                    float softExpand = Shadow0[wi].z;
                    float soft = softExpand > 0.0 ? exp(-dPx / softExpand) * 0.6 * abs(factor) : 0.0;
                    sh = max(outline, soft);

                    scol = mix(scol, outlineCol, sh > 0.0 ? min(outline / max(sh, 1e-5), 1.0) : 0.0);
                } else {
                    float expand = max(expandRaw, 1e-4);
                    sh = exp(-dPx / expand) * 0.6 * factor;
                }
                float opaShadow = clamp(Smoothings[wi].w, 0.0, 1.0);
                float sa = clamp(ShadowColor[wi].a * sh, 0.0, 1.0) * opaShadow;
                currentColor = mix(currentColor, scol, sa);
            }
            }

        vec2 p = screenToUV(coord, inSize);
        SDFResult f = fieldWidgets(p, inSize, coord, fl);
        float merged = f.dist;

        float focusHere = Extra0[max(f.index, 0)].z;
        float bodyGate = focusHere > 0.0001 ? 0.01 : 0.001;

        if (merged < bodyGate && f.index >= 0) {
            vec2 coordPix = coord;
            if (Pixelated > 0.5) {
                coordPix = floor(coord / PixelGridSize) * PixelGridSize + 0.5 * PixelGridSize;
            }
            vec2 uvPix = coordPix / inSize;

            int idx = max(f.index, 0);

            vec4 o0 = Optics0[idx];
            vec4 o1 = Optics1[idx];
            vec4 o2 = Optics2[idx];
            vec4 tint = Tints[idx];
            vec4 extra = Extra0[idx];

            float refThickness = o0.x;
            float refFactor = o0.y;
            float refDisp = o0.z;
            float refFresRange = o0.w;

            float refFresHard = o1.x / 100.0;
            float refFresFac = o1.y / 100.0;
            float glareRange = o1.z;
            float glareHard = o1.w / 100.0;

            float hoverK = extra.y;
            float focusK = extra.z;

            float blurCode = extra.x;
            bool frostFalloff = blurCode >= BLUR_FALLOFF_BIAS - 0.5;
            if (frostFalloff) blurCode -= BLUR_FALLOFF_BIAS;

            int blurIndex = int(floor(blurCode + 0.5));

            gFrostRamp = 1.0;
            if (frostFalloff) {
                vec4 rcF = Rects[idx];
                float tTop = clamp((coord.y - rcF.y) / max(rcF.w, 1.0), 0.0, 1.0);

                gFrostRamp = smoothstep(0.0, 1.0, tTop);
            }

            vec2 normal = f.normal;
            float nlen = length(normal);
            if (nlen > 1e-6) normal /= nlen;

            float nmerged = -merged * inSize.y;

            float edgeFactor = rimEdgeFactor(nmerged, refThickness, refFactor);

            float warpSlope = (rimEdgeFactor(nmerged + 1.0, refThickness, refFactor) - edgeFactor) * 0.08 * inSize.y;

            bool overText = layerRange.z > float(EARLY_TEXT_LAYER) + 0.5;
            bool overContent = layerRange.z > float(CONTENT_LAYER) + 0.5;
            bool overPopoverContent = layerRange.z > PopoverContentBoundary + 0.5;

            bool addText = overText && TextInCapture < 0.5;
            bool addContent = overContent && ContentInCapture < 0.5;
            bool addPopover = overPopoverContent && PopoverInCapture < 0.5;

            vec2 refrOffset = -normal * edgeFactor * 0.08 * vec2(inSize.y / inSize.x, 1.0);

            float bevelHint = Extra3[idx].x;
            float reachFrac = Extra3[idx].y;
            bool lensMode = abs(reachFrac) > 0.0001;

            float foldRaw = Extra4[idx].y;
            bool bakesTextAllowed = foldRaw >= 0.0;
            float lensFoldAmount = bakesTextAllowed ? foldRaw : (-foldRaw - 1.0);
            bool softLens = lensFoldAmount > 1.5;
            if (softLens) lensFoldAmount -= 2.0;

            float textScale = Extra5[idx].x;
            bool scalesText = textScale > 0.0 && abs(textScale - 1.0) > 0.0001;

            bool lensBakesText = ((lensMode && reachFrac > 0.0) || bevelHint > 0.0001 || scalesText) && bakesTextAllowed;

            float lensTextFade = 0.0;
            if (lensMode) {
                vec4 rcR = Rects[idx];
                float bandPx = max(refThickness, 1e-4);

                float peakCap = mix(0.75, 3.0, clamp(lensFoldAmount, 0.0, 1.0));
                float peakPx = min(abs(reachFrac) * min(rcR.z, rcR.w), peakCap * bandPx);
                float t = clamp(max(nmerged, 0.0) / bandPx, 0.0, 1.0);

                float lensDir = reachFrac < 0.0 ? 1.0 : -1.0;
                float lensFalloff = softLens ? (1.0 - t) * (1.0 - t) : 1.0 - t;
                refrOffset = lensDir * normal * (peakPx * lensFalloff) * vec2(1.0 / inSize.x, 1.0 / inSize.y);
                warpSlope = lensDir * peakPx / bandPx * (softLens ? 2.0 * (1.0 - t) : 1.0);
                lensTextFade = clamp(peakPx / max(0.3 * bandPx, 1e-4), 0.0, 1.0);
            }

            float bevelH = Extra3[idx].x;
            bool bevelMode = bevelH > 0.0001;
            float bevelSpec = 0.0;
            if (bevelMode) {
                float bandB = max(refThickness, 1e-4);
                float bt = clamp(nmerged / bandB, 0.0, 1.0);
                float bu = 1.0 - bt;
                float broot = sqrt(max(1.0 - bu * bu, 1e-6));

                float bslope = min((bevelH / bandB) * (bu / broot), 12.0);
                float bsn = sqrt(bslope * bslope + 1.0);
                vec3 Nw = vec3(bslope * normal / bsn, 1.0 / bsn);

                float bevelOptic = clamp(Smoothings[idx].y, 0.0, 4.0);

                float rimBite = clamp(glareHard, 0.0, 1.0);
                float localH = mix(bevelH * broot, bevelH, rimBite) * bevelOptic;
                vec3 Iw = vec3(0.0, 0.0, -1.0);
                vec3 Rw = refract(Iw, Nw, 1.0 / max(refFactor, 1.0001));
                if (abs(Rw.z) > 1e-4) {
                    refrOffset = Rw.xy * (localH / abs(Rw.z)) * vec2(1.0 / inSize.x, 1.0 / inSize.y);
                } else {
                    refrOffset = vec2(0.0);
                }
                warpSlope = 0.0;

                const float BEVEL_FILL = 0.7;
                vec3 Lw = normalize(vec3(-0.18, 0.62, 0.76));
                vec3 Lfill = normalize(vec3(Lw.x, -Lw.y, Lw.z));
                float shin = max(refFresHard * 100.0, 1.0);
                bevelSpec = pow(max(dot(Nw, Lw), 0.0), shin) * refFresFac;
                bevelSpec += pow(max(dot(Nw, Lfill), 0.0), shin) * refFresFac * BEVEL_FILL;

                lensTextFade = 1.0;
            }

            const float NR = 0.985;
            const float NG = 1.000;
            const float NB = 1.015;

            vec2 dispDelta = refrOffset * ((NB - 1.0) * refDisp);

            if (blurIndex < 0) {
                float maxReachPx = max(refThickness, 1.0);
                float reachPx = length(refrOffset * inSize);
                if (reachPx > maxReachPx) {
                    refrOffset *= maxReachPx / reachPx;
                    warpSlope *= maxReachPx / reachPx;
                }

                float maxDispersionPx = lensMode ? 6.0 : 1.5;
                dispDelta = refrOffset * ((NB - 1.0) * refDisp);
                float dispPx = length(dispDelta * inSize);
                if (dispPx > maxDispersionPx) dispDelta *= maxDispersionPx / dispPx;
            }

            vec2 rimOffset = refrOffset;

            float wedgePx = extra.w;
            if (abs(wedgePx) > 0.0001) {
                float wedgeRamp = smoothstep(0.0, max(refThickness, 1e-4), max(nmerged, 0.0));
                if (bevelMode) {

                    const float SLIDE_RAMP_PER_SHIFT = 1.55;
                    float slideRamp = clamp(nmerged / max(SLIDE_RAMP_PER_SHIFT * abs(wedgePx), 1e-4), 0.0, 1.0);

                    vec4 rcS = Rects[idx];
                    float halfMinS = max(0.5 * min(rcS.z, rcS.w), 1.0);
                    float slideFade = 1.0 - smoothstep(0.45 * halfMinS, 0.85 * halfMinS, nmerged);
                    refrOffset += normal * (wedgePx * slideRamp * slideFade) * vec2(1.0 / inSize.x, 1.0 / inSize.y);                } else {
                        refrOffset.y += wedgePx * wedgeRamp / inSize.y;
                    }
            }

            vec2 oR = refrOffset + dispDelta;
            vec2 oG = refrOffset;
            vec2 oB = refrOffset - dispDelta;

            float footprintPx = clamp(abs(1.0 + warpSlope), 1.0, float(RIM_TAPS_MAX));
            int rimTaps = int(floor(footprintPx + 0.5));
            vec2 tapStep = rimTaps > 1 ? normal * (footprintPx / float(rimTaps)) * vec2(1.0 / inSize.x, 1.0 / inSize.y) : vec2(0.0);

            vec4 backdropHere = sampleBackdrop(blurIndex, uvPix, addText, addContent, addPopover);

            vec4 blurred;
            vec4 dispPixel;
            dispPixel.a = 1.0;

            if (refrOffset.x == 0.0 && refrOffset.y == 0.0) {

                blurred = backdropHere;
                dispPixel.rgb = backdropHere.rgb;
            } else {
                blurred = sampleBackdropFiltered(blurIndex, uvPix + refrOffset, tapStep, rimTaps, addText, addContent, addPopover);

                dispPixel.g = blurred.g;
                if (dispDelta.x == 0.0 && dispDelta.y == 0.0) {
                    dispPixel.r = blurred.r;
                    dispPixel.b = blurred.b;
                } else {
                    dispPixel.r = sampleBackdropFiltered(blurIndex, uvPix + oR, tapStep, rimTaps, addText, addContent, addPopover).r;
                    dispPixel.b = sampleBackdropFiltered(blurIndex, uvPix + oB, tapStep, rimTaps, addText, addContent, addPopover).b;
                }
            }
            vec3 heldTextRgb = vec3(0.0);
            vec3 heldTextA = vec3(0.0);

            if (lensBakesText) {

                vec2 textUv = uvPix;
                if (scalesText) {
                    vec4 rcT = Rects[idx];
                    vec2 centerPx = vec2(rcT.x + 0.5 * rcT.z, rcT.y + 0.5 * rcT.w);
                    textUv = (centerPx + (coordPix - centerPx) / textScale) / inSize;
                }

                float textFade = scalesText ? 1.0 : lensTextFade;

                float textRefract = clamp(Extra5[idx].y, 0.0, 1.0);
                vec2 textOffset = rimOffset * textRefract;
                vec2 textDisp = dispDelta * textRefract;

                vec4 tR = wlSample(Sampler6, textUv + textOffset + textDisp) * textFade;
                vec4 tG = wlSample(Sampler6, textUv + textOffset) * textFade;
                vec4 tB = wlSample(Sampler6, textUv + textOffset - textDisp) * textFade;
                if (scalesText) {
                    heldTextRgb = vec3(tR.r, tG.g, tB.b);
                    heldTextA = vec3(tR.a, tG.a, tB.a);
                } else {
                    dispPixel.r = dispPixel.r * (1.0 - tR.a) + tR.r;
                    dispPixel.g = dispPixel.g * (1.0 - tG.a) + tG.g;
                    dispPixel.b = dispPixel.b * (1.0 - tB.a) + tB.b;
                }
            }

            vec4 outColor = mix(dispPixel, vec4(tint.rgb, 1.0), tint.a * 0.8);

            float toneLo = Extra4[idx].z;
            float toneHi = Extra4[idx].w;
            float chromaK = Smoothings[idx].z;
            if (toneHi > toneLo || chromaK < 0.999) {
                float bodyLuma = dot(outColor.rgb, vec3(0.2126, 0.7152, 0.0722));
                float toned = bodyLuma;
                if (toneHi > toneLo) {

                    float knee = max(0.02, (toneHi - toneLo) * 0.22);
                    toned = softCeil(softFloor(bodyLuma, toneLo, knee), toneHi, knee);
                }

                outColor.rgb = clamp(vec3(toned) + (outColor.rgb - vec3(bodyLuma)) * chromaK, 0.0, 1.0);
            }

            float headroom = clamp(1.0 - dot(outColor.rgb, vec3(0.2126, 0.7152, 0.0722)), 0.0, 1.0);

            float glareGeo = 0.0;
            if (bevelMode) {
                vec3 specTint = mix(vec3(1.0), tint.rgb, tint.a * 0.5);
                outColor.rgb = clamp(outColor.rgb + specTint * (bevelSpec * nlen * headroom), 0.0, 1.0);
            } else {

                float fresRangeK = 500.0 / max(refFresRange, 1e-6);
                float fresBase = max(1.0 + merged * inSize.y / 1500.0 * (fresRangeK * fresRangeK) + refFresHard, 0.0);
                float fresBase2 = fresBase * fresBase;
                float fresnelFactor = clamp(fresBase2 * fresBase2 * fresBase, 0.0, 1.0);
                vec3 fresTint = mix(vec3(1.0), tint.rgb, tint.a * 0.5);
                outColor = mix(outColor, vec4(fresTint, 1.0), fresnelFactor * refFresFac * 0.7 * nlen * headroom);

                float glareRangeK = 500.0 / max(glareRange, 1e-6);
                float glareBase = max(1.0 + merged * inSize.y / 1500.0 * (glareRangeK * glareRangeK) + glareHard, 0.0);
                float glareBase2 = glareBase * glareBase;
                glareGeo = clamp(glareBase2 * glareBase2 * glareBase, 0.0, 1.0);

                vec4 e2 = Extra2[idx];
                float dirGlareFactor = clamp(e2.w / 100.0, 0.0, 1.0);
                if (dirGlareFactor > 0.0001) {
                    vec2 glareDir = vec2(cos(e2.x), sin(e2.x));
                    float mainAlign = clamp(dot(normal, glareDir), 0.0, 1.0);
                    float oppAlign = clamp(dot(normal, -glareDir), 0.0, 1.0) * clamp(e2.z / 100.0, 0.0, 1.0);
                    float dirMask = max(mainAlign, oppAlign);

                    float sharpness = mix(3.0, 40.0, clamp(e2.y / 100.0, 0.0, 1.0));
                    dirMask = pow(dirMask, max(sharpness, 1e-3));
                    glareGeo *= dirMask * dirGlareFactor;
                }

                vec3 glareMix = mix(blurred.rgb, tint.rgb, tint.a * 0.5);
                outColor.rgb = mix(outColor.rgb, glareMix, 0.25 * glareGeo * nlen * headroom);
                }

            float mergedPxAbs = abs(merged) * inSize.y;

            float innerSh = Extra4[idx].x;
            if (innerSh > 0.0001) {

                vec3 innerCol = ShadowColor[idx].rgb;
                float innerOpa = clamp(Smoothings[idx].w, 0.0, 1.0);

                float halfH = 0.5 * Rects[idx].w;
                float inset = min(INNER_INSET, halfH * INNER_INSET_FRAC);
                float width = max(1.0, inset * (INNER_WIDTH / INNER_INSET));
                float upFace = clamp(normal.y, 0.0, 1.0);
                float t = (max(nmerged, 0.0) - inset) / width;
                float band = exp(-t * t);

                float innerAmt = clamp(innerSh * INNER_TOP_SCALE * band * upFace * nlen * innerOpa, 0.0, 1.0);
                outColor.rgb = mix(outColor.rgb, innerCol, innerAmt);
            }

            float edgeDarkenRaw = o2.x;
            float edgeDarken = clamp(abs(edgeDarkenRaw), 0.0, 1.0);

            if (edgeDarken > 0.0001 && mergedPxAbs < EDGE_REACH) {
                float darkRim = lineGaussAA(merged * inSize.y, 1.0);

                float faceWeight = edgeDarkenRaw < 0.0 ? (1.0 - normal.y * normal.y) : 1.0;
                outColor.rgb = mix(outColor.rgb, o2.yzw, edgeDarken * darkRim * faceWeight * nlen);
            }

            float edgeLuma = dot(backdropHere.rgb, vec3(0.2126, 0.7152, 0.0722));
            float lumaGate = mix(BRIGHT_EDGE_FLOOR, 1.0, smoothstep(BRIGHT_EDGE_LUMA_LO, BRIGHT_EDGE_LUMA_HI, edgeLuma));
            float brightEdge = lumaGate * clamp(Extra3[idx].z, 0.0, 1.0);
            if (brightEdge > 0.0001 && mergedPxAbs < EDGE_REACH) {

                vec4 rcE = Rects[idx];
                float sizeScale = clamp(max(min(rcE.z, rcE.w), 1.0) / BRIGHT_EDGE_REF_PX, BRIGHT_EDGE_MIN_SCALE, 1.0);

                float vertFace = normal.y * normal.y;
                float horizFace = 1.0 - vertFace;
                float edgeWidth = max(BRIGHT_EDGE_WIDTH, Extra3[idx].w);
                float litLine = lineGaussAA(merged * inSize.y, edgeWidth * sizeScale);
                vec3 litEdge = clamp(backdropHere.rgb * BRIGHT_EDGE_GAIN + BRIGHT_EDGE_LIFT * sizeScale, 0.0, 1.0);

                float bodyLuma = dot(outColor.rgb, vec3(0.2126, 0.7152, 0.0722));
                float litLuma = dot(litEdge, vec3(0.2126, 0.7152, 0.0722));
                litEdge = clamp(litEdge + max(0.0, bodyLuma + BRIGHT_EDGE_CONTRAST * sizeScale - litLuma), 0.0, 1.0);

                outColor.rgb = mix(outColor.rgb, litEdge, BRIGHT_EDGE_MIX * sizeScale * brightEdge * vertFace * litLine * nlen);
                float sideLine = lineGaussAA(merged * inSize.y, SIDE_EDGE_WIDTH * sizeScale);
                vec3 deepEdge = backdropHere.rgb * SIDE_EDGE_DEPTH;
                float dl = dot(deepEdge, vec3(0.2126, 0.7152, 0.0722));
                deepEdge = clamp(mix(vec3(dl), deepEdge, 1.2), 0.0, 1.0);
                outColor.rgb = mix(outColor.rgb, deepEdge, SIDE_EDGE_MIX * brightEdge * horizFace * sideLine * nlen);
            }

            if (hoverK > 0.0001) {

                float edgeProx = exp(-mergedPxAbs / 9.0);
                vec2 tdir = normalize(vec2(-normal.y, normal.x));
                float refBoost = 1.0 + 0.35 * hoverK * edgeProx;
                vec2 refrHover = refrOffset * refBoost;
                vec2 chromaT = tdir * (1.5 * hoverK * edgeProx) / inSize;
                vec3 subtle = vec3(sampleBackdrop(blurIndex, uvPix + refrHover + chromaT, addText, addContent, addPopover).r, sampleBackdrop(blurIndex, uvPix + refrHover, addText, addContent, addPopover).g, sampleBackdrop(blurIndex, uvPix + refrHover - chromaT, addText, addContent, addPopover).b);
                vec3 add = mix(subtle, vec3(1.0), 0.08) * (0.15 + 0.35 * edgeProx) * hoverK * headroom;
                outColor.rgb = clamp(outColor.rgb + add, 0.0, 1.0);
            }

            vec3 focusRing = vec3(0.0);
            if (focusK > 0.0001) {
                vec4 rcF = Rects[idx];
                vec2 centerPx = vec2(rcF.x + 0.5 * rcF.z, rcF.y + 0.5 * rcF.w);
                vec2 v = (coord - centerPx) / inSize.y;

                float phi = atan(v.y, v.x);
                float phi0 = Time * FocusBorderSpeed;
                float c = 0.5 + 0.5 * cos(phi - phi0);
                float sweep = smoothstep(0.75, 1.0, c);

                float mergedPx = merged * inSize.y;
                float w = max(FocusBorderWidthPx, 1e-3);

                float bandT = mergedPx / w;
                float band = exp(-bandT * bandT);

                float intensity = FocusBorderIntensity * focusK;
                vec3 ringTint = mix(vec3(1.0), tint.rgb, 0.2);
                focusRing = ringTint * (band * sweep) * intensity * headroom;
            }

            vec3 rimGlow = vec3(0.0);
            float rimIntensity = Extra1[idx].w;
            if (rimIntensity > 0.0001 && mergedPxAbs < EDGE_REACH) {
                vec3 rimColor = Extra1[idx].xyz;
                float mergedPx = merged * inSize.y;
                float rimLine = lineGaussAA(mergedPx, 0.9);

                float edgeBias = edgeDarkenRaw < 0.0 ? normal.y * normal.y : mix(0.6, 1.0, clamp(abs(normal.y), 0.0, 1.0));
                float rimAmt = rimLine * edgeBias * rimIntensity * nlen * headroom;
                if (edgeDarkenRaw < 0.0) {

                    rimGlow = (outColor.rgb * 1.8 + rimColor * 0.18) * rimAmt;
                } else {
                    rimGlow = rimColor * rimAmt;
                }
            }

            outColor.rgb = outColor.rgb * (1.0 - heldTextA) + heldTextRgb;

            float opaBody = clamp(Smoothings[idx].w, 0.0, 1.0);
            float aa = clamp(0.5 - merged * inSize.y, 0.0, 1.0) * opaBody;
            currentColor = mix(currentColor, outColor.rgb, aa);

            if (lensBakesText) earlyTextConsumed = max(earlyTextConsumed, aa * (scalesText ? 1.0 : lensTextFade));

            currentColor.rgb = clamp(currentColor.rgb + focusRing * opaBody, 0.0, 1.0);

            currentColor.rgb = clamp(currentColor.rgb + rimGlow * aa, 0.0, 1.0);
        }
    }

    if (CompositeTextEnd > 0.5) {
        vec4 earlyText = wlSample(Sampler6, uv) * (1.0 - earlyTextConsumed);
        currentColor = currentColor * (1.0 - earlyText.a) + earlyText.rgb;
    }
    if (CompositeContentEnd > 0.5) {
        vec4 rowContent = wlSample(Sampler7, uv);
        currentColor = currentColor * (1.0 - rowContent.a) + rowContent.rgb;
    }
    if (CompositePopoverEnd > 0.5) {
        vec4 popoverContent = wlSample(Sampler9, uv);
        currentColor = currentColor * (1.0 - popoverContent.a) + popoverContent.rgb;
    }

    fragColor = vec4(currentColor, 1.0);
}
