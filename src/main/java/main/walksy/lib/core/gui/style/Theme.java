package main.walksy.lib.core.gui.style;

import main.walksy.lib.core.gui.WalksyLibPanelScreen;

public final class Theme {
    public static final int GRAY_DARK = 0x98989D;
    public static final int GRAY_LIGHT = 0x8E8E93;
    public static final Theme DARK = dark();
    public static final Theme LIGHT = light();
    private final boolean dark;
    private int label;
    private int secondaryLabel;
    private int tertiaryLabel;
    private int quaternaryLabel;
    private int separator;
    private int accent;
    private int destructive;
    private int material;
    private int menuMaterial;
    private int flatSurface;
    private float materialOpacity;
    private float menuOpacity;
    private int fillColor;
    private float fillPrimary;
    private float fillSecondary;
    private float fillTertiary;
    private float fillQuaternary;
    private float fillQuinary;
    private int trackFill;
    private int trackFillSolid;
    private int fieldFillSolid;
    private int cardSurface;
    private float trackOpacity;
    private float toneFloor;
    private float toneCeiling;
    private float chroma;
    private int sidebarTint;
    private int fieldTint;
    private int pillTint;
    private int selectedRowTint;
    private int cardFill;
    private float sidebarOpacity;
    private float fieldOpacity;
    private float pillOpacity;
    private float selectedRowOpacity;
    private float cardFillAlpha;
    private int rimLight;
    private int edgeDarkenColor;
    private int shadow;
    private int scrim;
    private float rimLightPanel;
    private float rimLightChrome;
    private float innerShadow;
    private float edgeDarken;
    private float outlineAlpha;
    private float outlineWidth;
    private float chromeEdgeDarken;
    private float shadowAlpha;
    private float shadowExpand;
    private float shadowFactor;
    private float shadowOffsetY;
    private float scrimAlpha;
    private float popoverShadowAlpha;
    private float popoverShadowExpand;
    private float popoverShadowFactor;
    private float popoverShadowOffsetY;
    private float chromeShadowAlpha;
    private float chromeShadowExpand;
    private float chromeShadowFactor;
    private float chromeShadowOffsetY;
    private int checkerLight;
    private int checkerDark;
    private int checkerMean;
    private int gridCheckerLight;
    private int gridCheckerDark;
    private int[] systemPalette;
    private static Integer accentOverride;
    public static final float SURFACE_OPACITY = 0.90f;

    public static Theme get() {
        return WalksyLibPanelScreen.THEME;
    }

    public static float tintAlpha(final float sketchOpacity) {
        return Math.min(sketchOpacity / 0.8f, 1.25f);
    }

    private static Theme dark() {
        final Theme t = new Theme(true);
        t.label = 0xFFFFFFFF;
        t.secondaryLabel = 0x8CFFFFFF;
        t.tertiaryLabel = 0x40FFFFFF;
        t.quaternaryLabel = 0x1AFFFFFF;
        t.separator = 0x24FFFFFF;
        t.accent = 0xFF0091FF;
        t.destructive = 0xFFFF4F44;
        t.material = 0x595959;
        t.materialOpacity = 0.61f;
        t.toneFloor = 0f;
        t.toneCeiling = 0f;
        t.chroma = 1.00f;
        t.menuMaterial = 0xFFFFFF;
        t.menuOpacity = 0.12f;
        t.flatSurface = 0x1E1E1E;
        t.fillColor = 0xFFFFFF;
        t.fillPrimary = 0.10f;
        t.fillSecondary = 0.08f;
        t.fillTertiary = 0.05f;
        t.fillQuaternary = 0.03f;
        t.fillQuinary = 0.02f;
        t.trackFill = 0xFFFFFF;
        t.trackOpacity = 0.10f;
        t.trackFillSolid = 0x646469;
        t.fieldFillSolid = 0x333338;
        t.cardSurface = 0x464648;
        t.sidebarTint = 0xFFFFFF;
        t.sidebarOpacity = 0.10f;
        t.fieldTint = 0xFFFFFF;
        t.fieldOpacity = 0.14f;
        t.pillTint = 0xFFFFFF;
        t.pillOpacity = 0.18f;
        t.selectedRowTint = 0xFFFFFF;
        t.selectedRowOpacity = 0.14f;
        t.cardFill = 0xFFFFFF;
        t.cardFillAlpha = 0.09f;
        t.rimLight = 0xFFFFFF;
        t.rimLightPanel = 0.40f;
        t.rimLightChrome = 0.40f;
        t.innerShadow = 0.32f;
        t.edgeDarkenColor = 0x000000;
        t.edgeDarken = 0f;
        t.outlineAlpha = 0.70f;
        t.outlineWidth = 0.6f;
        t.shadow = 0x000000;
        t.shadowAlpha = 0.38f;
        t.shadowExpand = 8f;
        t.shadowFactor = 0.90f;
        t.shadowOffsetY = 6f;
        t.popoverShadowAlpha = 0.25f;
        t.popoverShadowExpand = 10f;
        t.popoverShadowFactor = 1.60f;
        t.popoverShadowOffsetY = 1.4f;
        t.chromeShadowAlpha = 0.34f;
        t.chromeShadowExpand = 10f;
        t.chromeShadowFactor = 1.20f;
        t.chromeShadowOffsetY = 2.5f;
        t.chromeEdgeDarken = 0f;
        t.scrim = 0x000000;
        t.scrimAlpha = 0.45f;
        t.checkerLight = 0xFF8E8E93;
        t.checkerDark = 0xFF636366;
        t.checkerMean = 0xFF79797C;
        t.gridCheckerLight = 0xFF8C8C90;
        t.gridCheckerDark = 0xFF7A7A7E;
        t.systemPalette = new int[] { 0xFFFF4245, 0xFFFF9230, 0xFFFFD600, 0xFF30D158, 0xFF00DAC3, 0xFF00D2E0, 0xFF3CD3FE, 0xFF0091FF, 0xFF6D7CFF, 0xFFDB34F2, 0xFFFF375F, 0xFFB78A66 };
        return t;
    }

    private static Theme light() {
        final Theme t = new Theme(false);
        t.label = 0xD9000000;
        t.secondaryLabel = 0x80000000;
        t.tertiaryLabel = 0x40000000;
        t.quaternaryLabel = 0x1A000000;
        t.separator = 0x2E3C3C43;
        t.accent = 0xFF0088FF;
        t.destructive = 0xFFFF3B30;
        t.material = 0xF3EEEB;
        t.materialOpacity = 0.70f;
        t.toneFloor = 0f;
        t.toneCeiling = 0f;
        t.chroma = 1.00f;
        t.menuMaterial = 0xFFFFFF;
        t.menuOpacity = 0.12f;
        t.flatSurface = 0xFFFFFF;
        t.fillColor = 0x000000;
        t.fillPrimary = 0.10f;
        t.fillSecondary = 0.08f;
        t.fillTertiary = 0.05f;
        t.fillQuaternary = 0.03f;
        t.fillQuinary = 0.02f;
        t.trackFill = 0x000000;
        t.trackOpacity = 0.10f;
        t.trackFillSolid = 0xADA399;
        t.fieldFillSolid = 0xA99F95;
        t.cardSurface = 0xC4C0BD;
        t.sidebarTint = 0x32261E;
        t.sidebarOpacity = 0.10f;
        t.fieldTint = 0x32261E;
        t.fieldOpacity = 0.14f;
        t.pillTint = 0xFFFFFF;
        t.pillOpacity = 0.18f;
        t.selectedRowTint = 0x000000;
        t.selectedRowOpacity = 0.14f;
        t.cardFill = 0x000000;
        t.cardFillAlpha = 0.09f;
        t.rimLight = 0xFFFFFF;
        t.rimLightPanel = 0.58f;
        t.rimLightChrome = 0.58f;
        t.innerShadow = 0.32f;
        t.edgeDarkenColor = 0x000000;
        t.edgeDarken = 0f;
        t.outlineAlpha = 0.70f;
        t.outlineWidth = 0.6f;
        t.shadow = 0x000000;
        t.shadowAlpha = 0.28f;
        t.shadowExpand = 5f;
        t.shadowFactor = 0.90f;
        t.shadowOffsetY = 2.5f;
        t.popoverShadowAlpha = 0.25f;
        t.popoverShadowExpand = 10f;
        t.popoverShadowFactor = 1.60f;
        t.popoverShadowOffsetY = 1.4f;
        t.chromeShadowAlpha = 0.28f;
        t.chromeShadowExpand = 10f;
        t.chromeShadowFactor = 1.45f;
        t.chromeShadowOffsetY = 2.5f;
        t.chromeEdgeDarken = 0f;
        t.scrim = 0x000000;
        t.scrimAlpha = 0.28f;
        t.checkerLight = 0xFFFFFFFF;
        t.checkerDark = 0xFFD1D1D6;
        t.checkerMean = 0xFFE8E8EB;
        t.gridCheckerLight = 0xFF8C8C90;
        t.gridCheckerDark = 0xFF7A7A7E;
        t.systemPalette = new int[] { 0xFFFF383C, 0xFFFF8D28, 0xFFFFCC00, 0xFF34C759, 0xFF00C8B3, 0xFF00C3D0, 0xFF00C0E8, 0xFF0088FF, 0xFF6155F5, 0xFFCB30E0, 0xFFFF2D55, 0xFFAC7F5E };
        return t;
    }

    private Theme(final boolean dark) {
        this.dark = dark;
    }

    public boolean isDark() {
        return this.dark;
    }

    public int label() {
        return this.label;
    }

    public int secondaryLabel() {
        return this.secondaryLabel;
    }

    public int tertiaryLabel() {
        return this.tertiaryLabel;
    }

    public int quaternaryLabel() {
        return this.quaternaryLabel;
    }

    public int separator() {
        return this.separator;
    }

    public static void setAccentOverride(final Integer argb) {
        accentOverride = argb;
    }

    public static Integer accentOverride() {
        return accentOverride;
    }

    public int accent() {
        return accentOverride != null ? accentOverride : this.accent;
    }

    public int accentRgb() {
        return accent() & 0x00FFFFFF;
    }

    public int destructive() {
        return this.destructive;
    }

    public int panelTint() {
        return this.material;
    }

    public float panelTintAlpha() {
        return tintAlpha(this.materialOpacity);
    }

    public int menuTint() {
        return this.menuMaterial;
    }

    public float menuTintAlpha() {
        return tintAlpha(this.menuOpacity);
    }

    public int flatSurface() {
        return this.flatSurface;
    }

    public float toneFloor() {
        return this.toneFloor;
    }

    public float toneCeiling() {
        return this.toneCeiling;
    }

    public float chroma() {
        return this.chroma;
    }

    public int fillColor() {
        return this.fillColor;
    }

    public float fillPrimary() {
        return tintAlpha(this.fillPrimary);
    }

    public float fillSecondary() {
        return tintAlpha(this.fillSecondary);
    }

    public float fillTertiary() {
        return tintAlpha(this.fillTertiary);
    }

    public int fillTertiaryArgb() {
        return alpha(this.fillColor, this.fillTertiary);
    }

    public int fillSecondaryArgb() {
        return alpha(this.fillColor, this.fillSecondary);
    }

    public float fillQuaternary() {
        return tintAlpha(this.fillQuaternary);
    }

    public float fillQuinary() {
        return tintAlpha(this.fillQuinary);
    }

    public int sidebarTint() {
        return this.sidebarTint;
    }

    public float sidebarTintAlpha() {
        return tintAlpha(this.sidebarOpacity);
    }

    public int fieldTint() {
        return this.fieldTint;
    }

    public float fieldTintAlpha() {
        return tintAlpha(this.fieldOpacity);
    }

    public int pillTint() {
        return this.pillTint;
    }

    public float pillTintAlpha() {
        return tintAlpha(this.pillOpacity);
    }

    public int selectedRowTint() {
        return this.selectedRowTint;
    }

    public float selectedRowTintAlpha() {
        return tintAlpha(this.selectedRowOpacity);
    }

    public int cardFillArgb() {
        return alpha(this.cardFill, this.cardFillAlpha);
    }

    public int cardFill() {
        return this.cardFill;
    }

    public float cardFillAlpha() {
        return this.cardFillAlpha;
    }

    public int selectedRowArgb() {
        return alpha(this.selectedRowTint, this.selectedRowOpacity);
    }

    public int hoverFillArgb() {
        return alpha(0x000000, this.dark ? 0.16f : 0.07f);
    }

    public int chromeTint() {
        return this.fillColor;
    }

    public float chromeTintAlpha() {
        return fillPrimary();
    }

    public int controlTint() {
        return this.fillColor;
    }

    public float controlTintAlpha() {
        return this.dark ? 0.17f : fillSecondary();
    }

    public float chromeGlareFactor() {
        return this.dark ? 5f : 6f;
    }

    public int segmentPill() {
        return this.fillColor;
    }

    public float segmentPillAlpha() {
        return fillPrimary();
    }

    public int trackFill() {
        return this.trackFill;
    }

    public float trackFillAlpha() {
        return tintAlpha(this.trackOpacity);
    }

    public int trackFillSolid() {
        return this.trackFillSolid;
    }

    public int fieldFillSolid() {
        return this.fieldFillSolid;
    }

    public float fieldFillAlpha() {
        return tintAlpha(SURFACE_OPACITY);
    }

    public int cardSurface() {
        return this.cardSurface;
    }

    public int knobBody() {
        return 0xFFFFFF;
    }

    public int onAccent() {
        return 0xFFFFFFFF;
    }

    public int rimLightColor() {
        return this.rimLight;
    }

    public float rimLightPanel() {
        return this.rimLightPanel;
    }

    public float rimLightChrome() {
        return this.rimLightChrome;
    }

    public float innerShadow() {
        return this.innerShadow;
    }

    public int edgeDarkenColor() {
        return this.edgeDarkenColor;
    }

    public float edgeDarken() {
        return this.edgeDarken;
    }

    public float outlineAlpha() {
        return this.outlineAlpha;
    }

    public float outlineWidth() {
        return this.outlineWidth;
    }

    public int shadowColor() {
        return this.shadow;
    }

    public float shadowAlpha() {
        return this.shadowAlpha;
    }

    public float shadowExpand() {
        return this.shadowExpand;
    }

    public float shadowFactor() {
        return this.shadowFactor;
    }

    public float shadowOffsetY() {
        return this.shadowOffsetY;
    }

    public float popoverShadowAlpha() {
        return this.popoverShadowAlpha;
    }

    public float popoverShadowExpand() {
        return this.popoverShadowExpand;
    }

    public float popoverShadowFactor() {
        return this.popoverShadowFactor;
    }

    public float popoverShadowOffsetY() {
        return this.popoverShadowOffsetY;
    }

    public float chromeShadowAlpha() {
        return this.chromeShadowAlpha;
    }

    public float chromeShadowExpand() {
        return this.chromeShadowExpand;
    }

    public float chromeShadowFactor() {
        return this.chromeShadowFactor;
    }

    public float chromeShadowOffsetY() {
        return this.chromeShadowOffsetY;
    }

    public float chromeEdgeDarken() {
        return this.chromeEdgeDarken;
    }

    public int scrimColor() {
        return this.scrim;
    }

    public float scrimAlpha() {
        return this.scrimAlpha;
    }

    public int checkerLight() {
        return this.checkerLight;
    }

    public int checkerDark() {
        return this.checkerDark;
    }

    public int checkerMean() {
        return this.checkerMean;
    }

    public int gridCheckerLight() {
        return this.gridCheckerLight;
    }

    public int gridCheckerDark() {
        return this.gridCheckerDark;
    }

    public int[] systemPalette() {
        return this.systemPalette;
    }

    public static int alpha(final int rgb, final float alpha) {
        final int a = Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }
}
