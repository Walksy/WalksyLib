package main.walksy.lib.core.gui.modal;

import com.mojang.blaze3d.platform.InputConstants;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.gui.style.UiStyle;
import main.walksy.lib.core.gui.style.PopoverStyle;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.liquid.LiquidMorph;
import main.walksy.lib.core.gui.liquid.LiquidRect;
import main.walksy.lib.core.gui.liquid.MorphProfile;
import main.walksy.lib.core.gui.widget.options.control.LiquidKnob;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.render.glass.api.LiquidGlassConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class PopoverHost {
    private static final Set<PopoverHost> OPEN_HOSTS = Collections.newSetFromMap(new WeakHashMap<>());
    public static final int PANEL_LAYER = 30;
    public static final int CONTENT_LAYER = 31;
    private final int panelLayer;
    private final MorphProfile morphProfile;
    public static final int SCREEN_MARGIN = 6;
    public static final float SURFACE_TINT_ALPHA = 0.78f;
    private static final float PANEL_CORNER_RADIUS = 14f;
    private static final float CONTENT_REVEAL_THRESHOLD = 0.35f;
    private static final float CONTENT_REVEAL_FULL = 0.75f;
    private static final float CLOSE_CONTENT_GONE = 0.35f;
    private static final float HANDOFF_TICKS = 12f;
    private static final float HANDOFF_DRIFT = 0.10f;
    private static final float HANDOFF_RISE = 0.12f;
    private static final float HANDOFF_MOVE_FRACTION = 0.6f;
    private static final float HANDOFF_TICKS_FIXED = 7f;
    private static final float HANDOFF_ANCHOR_SPAN = 0.12f;
    private static final float LANDING_SPAN = 0.3f;
    private static final float DURATION_TICKS = 10f;
    private static final float CLOSE_GLASS_HOLD_UNTIL = 0.25f;
    private static final float CLOSE_STYLE_DONE = 0.05f;
    private static final float BLOOM_DECAY_TICKS = 2.5f;
    private static final float CORE_FLASH_DECAY_TICKS = 1.4f;
    private static final float CORE_FLASH_ALPHA = 0.95f;
    private static final float CORE_FLASH_RADIUS = 1.6f;
    private static final float CORE_FLASH_GROWTH = 1.2f;
    private static final float FROST_START_TICKS = 0.8f;
    private static final float FROST_END_TICKS = 3.0f;
    private static final float ALPHA_REVEAL_TICKS = 4f;
    private static final float FADE_TICKS = 4f;
    private static final float LENS_ENDPOINT_HOLD = 0.35f;
    private final GlassStyle panelStyle;
    private LiquidMorph morph = new LiquidMorph();
    private float handoffEmitted;
    private PopoverContent retiring;
    private LiquidMorph retiringMorph;
    private LiquidMorph spareMorph;
    private float retiringClock;
    private LiquidRect retiringAnchor;
    private float retiringFromCx;
    private float retiringFromCy;
    private PopoverContent content;
    private LiquidRect panelRect;
    private LiquidRect anchor;
    private int screenW;
    private int screenH;
    private PositionMode positionMode = PositionMode.ANCHORED;
    private int boundsX;
    private int boundsY;
    private int boundsW;
    private int boundsH;
    private float explicitX;
    private float explicitY;
    private float bloomCx;
    private float bloomCy;
    private float bloomSeedRadius;
    private float animT;
    private float closeClock;
    private boolean fading;
    private float hostAlpha = 1f;
    private float handoffFromCx;
    private float handoffFromCy;

    public static boolean anyOpen() {
        for (final PopoverHost host : OPEN_HOSTS) {
            if (host.isOpen()) {
                return true;
            }
        }
        return false;
    }

    public static boolean anyPanelContains(final double mouseX, final double mouseY) {
        for (final PopoverHost host : OPEN_HOSTS) {
            if (host.containsPoint(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    public static boolean anyPanelIntersects(final float x0, final float y0, final float x1, final float y1, final int layer) {
        for (final PopoverHost host : OPEN_HOSTS) {
            if (host.panelLayer > layer && host.intersects(x0, y0, x1, y1)) {
                return true;
            }
        }
        return false;
    }

    public PopoverHost() {
        this(PANEL_LAYER, MorphProfile.apple());
    }

    public PopoverHost(final int panelLayer) {
        this(panelLayer, MorphProfile.apple());
    }

    public PopoverHost(final MorphProfile morphProfile) {
        this(PANEL_LAYER, morphProfile);
    }

    public PopoverHost(final int panelLayer, final MorphProfile morphProfile) {
        this.panelLayer = panelLayer;
        this.morphProfile = morphProfile == null ? MorphProfile.apple() : morphProfile;
        this.panelStyle = LiquidKnob.bevelKnobStyle(panelLayer)
            .blurRadius(LiquidGlassConstants.BLUR_POPOVER)
            .tint(Theme.get().menuTint(), Theme.get().menuTintAlpha());
    }

    private static GlassStyle fadeLens(final GlassStyle style, final float settle) {
        final float travel = Math.min(settle, 1f - settle) * 2f;
        final float t = Math.max(0f, Math.min(1f, (travel - LENS_ENDPOINT_HOLD) / (1f - LENS_ENDPOINT_HOLD)));
        final float k = 1f - t * t * (3f - 2f * t);
        if (k >= 0.999f) {
            return style;
        }
        return softenLens(style, k)
            .refractionReach(style.getRefractionReach() * k)
            .bevel(style.getBevelHeight() * k)
            .lensFold(style.getLensFold() * k);
    }

    private static GlassStyle styleAt(final GlassStyle origin, final GlassStyle panel, final float t) {
        if (t <= 0f) {
            return origin.copy();
        }
        final GlassStyle s = GlassStyle.lerp(origin, panel, t);
        s.blurRadius(t < 0.5f ? origin.getBlurRadius() : panel.getBlurRadius());
        return s;
    }

    private static GlassStyle softenLens(final GlassStyle style, final float k) {
        if (k >= 1f) {
            return style;
        }
        final float clamped = Math.max(0f, k);
        return style.refractionThickness(style.getRefThickness() * clamped)
            .refractionDispersion(style.getRefDispersion() * clamped)
            .refractionFactor(1f + (style.getRefFactor() - 1f) * clamped);
    }

    public int panelLayer() {
        return this.panelLayer;
    }

    public MorphProfile morphProfile() {
        return this.morphProfile;
    }

    public int contentLayer() {
        return this.panelLayer + 1;
    }

    public static float screenMargin() {
        return SCREEN_MARGIN * WLScreen.uiScaleFactor();
    }

    public static GlassStyle surfaceStyle() {
        return new GlassStyle()
            .tint(Theme.get().cardSurface(), SURFACE_TINT_ALPHA)
            .blurRadius(LiquidGlassConstants.BLUR_CLEAR)
            .refractionReach(0f)
            .refractionThickness(0f)
            .refractionFactor(1f)
            .refractionDispersion(0f)
            .fresnelRange(0f)
            .fresnelHardness(0f)
            .fresnelFactor(0f)
            .glareRange(0f)
            .glareHardness(0f)
            .edgeDarken(0f)
            .brightEdge(0f)
            .rimLight(Theme.get().rimLightColor(), 0f)
            .shadow(0f, 0f, 0f, 0f)
            .shadowColor(Theme.get().shadowColor(), 0f);
    }

    public interface PopoverContent {
        default PopoverStyle style() {
            return PopoverStyle.getDefault();
        }

        default int preferredWidth() {
            return 160;
        }

        default int preferredHeight() {
            return 120;
        }

        default float reserveRightPx() {
            return 0f;
        }

        default float panelRefractionFactor() {
            return 0f;
        }

        void extract(WLGraphics2D graphics, int panelX, int panelY, int panelW, int panelH, int mouseX, int mouseY, float delta, float reveal);

        @Nullable
        default GlassStyle originStyle() {
            return null;
        }

        default float lensScale() {
            return 1f;
        }

        default int bloomColor() {
            return 0;
        }

        default boolean keepsOriginStyle() {
            return false;
        }

        default boolean bouncyOpen() {
            return true;
        }

        default boolean ridesContent() {
            return true;
        }

        default MorphProfile morphProfile() {
            return null;
        }

        default boolean bridged() {
            return true;
        }

        default boolean growFromTop() {
            return false;
        }

        default void onCloseBegan() {
        }

        @Nullable
        default LiquidRect liveAnchor() {
            return null;
        }

        default boolean landsOnAnchor() {
            return false;
        }

        default void landing(final float amount) {
        }

        default void hostAlpha(final float alpha) {
        }

        default int landingLayer() {
            return -1;
        }

        default void extractLanding(final WLGraphics2D graphics, final LiquidRect current, final float landing) {
        }

        default void onClosed() {
        }

        default boolean hasTriggerHandoff() {
            return false;
        }

        default boolean handoffRides() {
            return true;
        }

        default void extractTriggerHandoff(final WLGraphics2D graphics, final float handoff) {
        }

        boolean mouseClicked(MouseButtonEvent event, boolean doubleClick, int panelX, int panelY, int panelW, int panelH);

        default boolean mouseClickedOutside(final MouseButtonEvent event, final boolean doubleClick) {
            return false;
        }

        default boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY, final int panelX, final int panelY, final int panelW, final int panelH) {
            return false;
        }

        default boolean mouseReleased(final MouseButtonEvent event) {
            return false;
        }

        default boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY, final int panelX, final int panelY, final int panelW, final int panelH) {
            return false;
        }

        default boolean keyPressed(final KeyEvent event) {
            return false;
        }

        default boolean charTyped(final CharacterEvent event) {
            return false;
        }
    }

    private enum PositionMode { ANCHORED, CENTERED, EXPLICIT }

    private void retireCurrent() {
        if (this.content == null) {
            return;
        }
        this.finishRetiring();
        if (!this.morph.isClosing()) {
            this.reanchorLive();
            this.morph.close();
            this.closeClock = 0f;
            this.handoffFromCx = this.panelRect.centerX();
            this.handoffFromCy = this.panelRect.centerY();
            this.content.onCloseBegan();
        }
        this.retiring = this.content;
        this.retiringClock = this.closeClock;
        this.retiringAnchor = this.anchor;
        this.retiringFromCx = this.handoffFromCx;
        this.retiringFromCy = this.handoffFromCy;
        final LiquidMorph outgoing = this.morph;
        this.morph = this.spareMorph != null ? this.spareMorph : new LiquidMorph();
        this.spareMorph = null;
        this.retiringMorph = outgoing;
        this.content = null;
    }

    private void finishRetiring() {
        if (this.retiring == null) {
            return;
        }
        final PopoverContent done = this.retiring;
        this.retiring = null;
        this.spareMorph = this.retiringMorph;
        this.retiringMorph = null;
        done.onClosed();
    }

    private void extractRetiring(final WLGraphics2D graphics, final float delta) {
        if (this.retiring == null) {
            return;
        }
        this.retiringClock += Math.max(0f, delta);
        if (UiStyle.minecraft()) {
            this.retiringClock = Math.max(this.retiringClock, HANDOFF_TICKS);
        }
        this.retiringMorph.update(delta);
        final GlassStyle origin = this.retiring.originStyle();
        final GlassStyle style = origin != null ? origin.copy() : this.panelStyle.scaled(WLScreen.uiScaleFactor());
        style.opacity(style.getOpacity() * this.hostAlpha);
        this.retiring.hostAlpha(this.hostAlpha);
        if (this.retiring.landsOnAnchor()) {
            final float landing = landingAmount(this.retiringMorph);
            style.opacity(style.getOpacity() * (1f - landing));
            this.retiring.landing(landing);
        }
        this.emitLanding(graphics, this.retiring, this.retiringMorph, style, this.panelLayer - 1, 1f - closeStyleT(this.retiringMorph));
        final boolean rides = this.retiring.handoffRides();
        final float span = rides ? HANDOFF_TICKS : HANDOFF_TICKS_FIXED;
        final float t = Math.max(0f, Math.min(1f, this.retiringClock / span));
        final float handoff = t * t * (3f - 2f * t);
        if (handoff > 0f && this.retiringAnchor != null) {
            final var hp = graphics.pose();
            final boolean posed = rides;
            if (posed) {
                final float k = 1f - handoffMove(this.retiringClock);
                final float ax = this.retiringAnchor.centerX();
                final float ay = this.retiringAnchor.centerY();
                final float sc = 1f + HANDOFF_RISE * k;
                hp.pushMatrix();
                hp.translate(ax + (this.retiringFromCx - ax) * HANDOFF_DRIFT * k, ay + (this.retiringFromCy - ay) * HANDOFF_DRIFT * k);
                hp.scale(sc, sc);
                hp.translate(-ax, -ay);
            }
            try {
                this.retiring.extractTriggerHandoff(graphics, handoff);
            } finally {
                if (posed) {
                    hp.popMatrix();
                }
            }
        }
        if (this.retiringMorph.isDone() && (!this.retiring.hasTriggerHandoff() || this.retiringClock >= span)) {
            this.finishRetiring();
        }
    }

    private float handoffFade() {
        if (this.content == null) {
            return 0f;
        }
        if (this.content.handoffRides()) {
            if (!this.morph.isClosing()) {
                return 0f;
            }
            final float t = Math.max(0f, Math.min(1f, this.closeClock / HANDOFF_TICKS));
            return t * t * (3f - 2f * t);
        }
        final float t = 1f - Math.max(0f, Math.min(1f, this.morph.value() / HANDOFF_ANCHOR_SPAN));
        return t * t * (3f - 2f * t);
    }

    private static float handoffMove(final float clock) {
        final float t = Math.max(0f, Math.min(1f, clock / (HANDOFF_TICKS * HANDOFF_MOVE_FRACTION)));
        return t * t * (3f - 2f * t);
    }

    private boolean lingering() {
        return this.content != null && this.content.hasTriggerHandoff() && this.morph.isClosing() && this.morph.isDone();
    }

    private boolean finishLinger() {
        if (!this.lingering()) {
            return false;
        }
        final PopoverContent closed = this.content;
        this.content = null;
        closed.onClosed();
        return true;
    }

    public boolean isOpen() {
        return this.content != null;
    }

    public void setHostAlpha(final float alpha) {
        this.hostAlpha = Math.max(0f, Math.min(1f, alpha));
    }

    public boolean containsPoint(final double mouseX, final double mouseY) {
        if (this.content == null || this.panelRect == null) {
            return false;
        }
        final int px = Math.round(this.panelRect.x());
        final int py = Math.round(this.panelRect.y());
        final int pw = Math.round(this.panelRect.w());
        final int ph = Math.round(this.panelRect.h());
        return mouseX >= px && mouseX < px + pw && mouseY >= py && mouseY < py + ph;
    }

    private boolean intersects(final float x0, final float y0, final float x1, final float y1) {
        if (this.content == null || this.panelRect == null) {
            return false;
        }
        return x0 < this.panelRect.x() + this.panelRect.w() && x1 > this.panelRect.x() && y0 < this.panelRect.y() + this.panelRect.h() && y1 > this.panelRect.y();
    }

    public void open(final LiquidRect anchor, final int screenW, final int screenH, final PopoverContent content) {
        this.fading = false;
        this.positionMode = PositionMode.ANCHORED;
        this.openInternal(anchor, screenW, screenH, content);
    }

    public void openCentered(final LiquidRect anchor, final int boundsX, final int boundsY, final int boundsW, final int boundsH, final int screenW, final int screenH, final PopoverContent content) {
        this.fading = false;
        this.positionMode = PositionMode.CENTERED;
        this.boundsX = boundsX;
        this.boundsY = boundsY;
        this.boundsW = boundsW;
        this.boundsH = boundsH;
        this.openInternal(anchor, screenW, screenH, content);
    }

    public void openFaded(final int boundsX, final int boundsY, final int boundsW, final int boundsH, final int screenW, final int screenH, final PopoverContent content) {
        this.fading = true;
        this.positionMode = PositionMode.CENTERED;
        this.boundsX = boundsX;
        this.boundsY = boundsY;
        this.boundsW = boundsW;
        this.boundsH = boundsH;
        this.screenW = screenW;
        this.screenH = screenH;
        final int w = Math.min(content.preferredWidth(), Math.max(1, Math.round(screenW - screenMargin() * 2)));
        final int h = Math.min(content.preferredHeight(), Math.max(1, Math.round(screenH - screenMargin() * 2)));
        this.openInternal(this.layoutPanel(w, h, true), screenW, screenH, content);
    }

    public void openAt(final LiquidRect anchor, final float x, final float y, final int screenW, final int screenH, final PopoverContent content) {
        this.fading = false;
        this.positionMode = PositionMode.EXPLICIT;
        this.explicitX = x;
        this.explicitY = y;
        this.openInternal(anchor, screenW, screenH, content);
    }

    private void openInternal(final LiquidRect anchor, final int screenW, final int screenH, final PopoverContent content) {
        if (this.content != null && !this.morph.isClosing() && anchor.equals(this.anchor)) {
            return;
        }
        this.retireCurrent();
        OPEN_HOSTS.add(this);
        this.content = content;
        this.handoffEmitted = 0f;
        this.anchor = anchor;
        this.screenW = screenW;
        this.screenH = screenH;
        final int w = Math.min(Math.max(Math.round(anchor.w()), content.preferredWidth()), Math.max(1, Math.round(screenW - screenMargin() * 2)));
        final int h = Math.min(content.preferredHeight(), Math.max(1, Math.round(screenH - screenMargin() * 2)));
        this.bloomCx = anchor.x() + anchor.w() / 2f;
        this.bloomCy = anchor.y() + anchor.h() / 2f;
        this.bloomSeedRadius = Math.max(anchor.w(), anchor.h()) / 2f;
        this.animT = 0f;
        final MorphProfile profile = content.morphProfile() != null ? content.morphProfile() : this.morphProfile;
        this.morph.profile(profile).bridged(content.bridged());
        this.panelRect = this.layoutPanel(w, h, true);
        this.morph.open(anchor, this.panelRect, content.bouncyOpen() && !this.fading);
    }

    private static float centeredInBounds(final int boundsStart, final int boundsSize, final int size) {
        final float centered = boundsStart + (boundsSize - size) / 2f;
        final float lo = boundsStart + screenMargin();
        final float hi = boundsStart + boundsSize - screenMargin() - size;
        return hi >= lo ? Math.max(lo, Math.min(centered, hi)) : centered;
    }

    private LiquidRect layoutPanel(final int w, final int h, final boolean initialOpen) {
        if (this.positionMode == PositionMode.EXPLICIT) {
            final float x = Math.max(screenMargin(), Math.min(this.explicitX, this.screenW - screenMargin() - w));
            final float y = Math.max(screenMargin(), Math.min(this.explicitY, this.screenH - screenMargin() - h));
            return new LiquidRect(x, y, w, h, PANEL_CORNER_RADIUS * WLScreen.uiScaleFactor());
        }
        if (this.positionMode == PositionMode.CENTERED) {
            final float x = Math.max(screenMargin(), centeredInBounds(this.boundsX, this.boundsW, w));
            final float y;
            if (!initialOpen && this.content != null && this.content.growFromTop()) {
                y = Math.max(screenMargin(), Math.min(this.panelRect.y(), this.screenH - screenMargin() - h));
            } else {
                y = Math.max(screenMargin(), centeredInBounds(this.boundsY, this.boundsH, h));
            }
            return new LiquidRect(x, y, w, h, PANEL_CORNER_RADIUS * WLScreen.uiScaleFactor());
        }
        final float anchorCenter = this.anchor.x() + this.anchor.w() / 2f;
        final float xAligned = anchorCenter > this.screenW / 2f ? this.anchor.x() + this.anchor.w() - w : this.anchor.x();
        final float reserveRight = this.content == null ? screenMargin() : Math.max(screenMargin(), this.content.reserveRightPx());
        final float x = Math.max(screenMargin(), Math.min(xAligned, this.screenW - reserveRight - w));
        float y;
        if (!initialOpen && this.content != null && this.content.growFromTop()) {
            y = Math.max(screenMargin(), Math.min(this.panelRect.y(), this.screenH - screenMargin() - h));
        } else {
            y = this.anchor.y();
            if (y + h > this.screenH - screenMargin()) {
                final float yBottomAligned = this.anchor.y() + this.anchor.h() - h;
                y = yBottomAligned >= screenMargin() ? yBottomAligned : Math.max(screenMargin(), this.screenH - screenMargin() - h);
            }
        }
        return new LiquidRect(x, y, w, h, PANEL_CORNER_RADIUS * WLScreen.uiScaleFactor());
    }

    public void close() {
        if (this.content != null && !this.morph.isClosing()) {
            this.reanchorLive();
            this.morph.close();
            this.closeClock = 0f;
            this.handoffFromCx = this.panelRect.centerX();
            this.handoffFromCy = this.panelRect.centerY();
            this.content.onCloseBegan();
        }
    }

    private void reanchorLive() {
        final LiquidRect live = this.content.liveAnchor();
        if (live != null) {
            this.anchor = live;
            this.morph.followAnchor(live);
        }
    }

    private static float closeStyleT(final LiquidMorph morph) {
        final float sv = Math.max(0f, Math.min(1f, morph.value()));
        return Math.max(0f, Math.min(1f, (sv - CLOSE_STYLE_DONE) / (CLOSE_GLASS_HOLD_UNTIL - CLOSE_STYLE_DONE)));
    }

    private void emitLanding(final WLGraphics2D graphics, final PopoverContent content, final LiquidMorph morph, final GlassStyle style, final int layer, final float shift) {
        final int landingLayer = content.landingLayer();
        if (landingLayer < 0) {
            morph.emit(graphics, style, layer);
            return;
        }
        final float landing = Math.max(0f, Math.min(1f, shift));
        if (landing < 0.999f) {
            morph.emit(graphics, style.copy().opacity(style.getOpacity() * (1f - landing)), layer);
        }
        if (landing > 0.001f) {
            morph.emit(graphics, style.copy().opacity(style.getOpacity() * landing), landingLayer);
        }
        content.extractLanding(graphics, morph.current(), landing);
    }

    private static float landingAmount(final LiquidMorph morph) {
        final float span = Math.max(1f, LANDING_SPAN * morph.anchorSize());
        final float t = Math.max(0f, Math.min(1f, morph.anchorDeviation() / span));
        return 1f - t * t * (3f - 2f * t);
    }

    public void closeInstant() {
        this.finishRetiring();
        if (this.content != null) {
            final PopoverContent closed = this.content;
            this.content = null;
            closed.onClosed();
        }
    }

    public void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float delta) {
        this.extractRetiring(graphics, delta);
        if (this.content == null) {
            OPEN_HOSTS.remove(this);
            return;
        }
        if (this.morph.isClosing()) {
            this.closeClock += Math.max(0f, delta);
        } else {
            this.animT = Math.min(DURATION_TICKS, this.animT + Math.max(0f, delta));
        }
        if (UiStyle.minecraft()) {
            this.animT = DURATION_TICKS;
            if (this.morph.isClosing()) {
                this.closeClock = Math.max(this.closeClock, HANDOFF_TICKS);
            }
        }
        this.reanchorLive();
        this.morph.update(delta);
        final float fadeIn = this.fadeAlpha();
        final boolean retireAfterFrame = this.fading ? this.morph.isClosing() && fadeIn <= 0f : this.morph.isDone() && (!this.content.hasTriggerHandoff() || !this.content.handoffRides() || this.closeClock >= HANDOFF_TICKS);
        if (!this.morph.isClosing() && this.animT >= DURATION_TICKS) {
            final int w = Math.min(Math.max(Math.round(this.anchor.w()), this.content.preferredWidth()), Math.max(1, Math.round(this.screenW - screenMargin() * 2)));
            final int h = Math.min(this.content.preferredHeight(), Math.max(1, Math.round(this.screenH - screenMargin() * 2)));
            if (w != Math.round(this.panelRect.w()) || h != Math.round(this.panelRect.h())) {
                final LiquidRect next = this.layoutPanel(w, h, false);
                this.morph.retarget(next);
                this.panelRect = next;
            }
        }
        final boolean stillOpening = !this.morph.isClosing() && this.animT < DURATION_TICKS;
        final boolean pinnedGeneric = !stillOpening && !this.morph.isClosing();
        final GlassStyle origin = this.content.originStyle();
        final GlassStyle scaledPanelStyle = this.panelStyle.scaled(WLScreen.uiScaleFactor());
        GlassStyle panelStyle = scaledPanelStyle;
        float panelMix = 1f;
        if (origin != null) {
            final float sv = Math.max(0f, Math.min(1f, this.morph.value()));
            if (this.content.keepsOriginStyle()) {
                panelStyle = origin.copy();
                panelMix = 0f;
            } else if (pinnedGeneric || this.morph.isClosing()) {
                final float styleT = pinnedGeneric ? 1f : Math.max(0f, Math.min(1f, (sv - CLOSE_STYLE_DONE) / (CLOSE_GLASS_HOLD_UNTIL - CLOSE_STYLE_DONE)));
                panelMix = styleT;
                panelStyle = styleAt(origin, scaledPanelStyle, styleT);
            } else {
                final float frostT = Math.max(0f, Math.min(1f, (this.animT - FROST_START_TICKS) / (FROST_END_TICKS - FROST_START_TICKS)));
                panelMix = frostT;
                panelStyle = styleAt(origin, scaledPanelStyle, frostT);
            }
        }
        final float lensK = 1f + (this.content.lensScale() - 1f) * panelMix;
        final GlassStyle emitted = fadeLens(softenLens(panelStyle, lensK), Math.max(0f, Math.min(1f, this.morph.value())));
        final float panelIor = this.content.panelRefractionFactor();
        if (panelIor > 0f && panelMix > 0f) {
            final float ior = Math.max(1f, panelIor);
            emitted.refractionFactor(emitted.getRefFactor() + (ior - emitted.getRefFactor()) * panelMix);
        }
        if (this.fading) {
            emitted.opacity(emitted.getOpacity() * fadeIn);
        }
        if (this.content.landsOnAnchor()) {
            final float landing = landingAmount(this.morph);
            emitted.opacity(emitted.getOpacity() * (1f - landing));
            this.content.landing(landing);
        }
        emitted.opacity(emitted.getOpacity() * this.hostAlpha);
        this.content.hostAlpha(this.hostAlpha);
        this.emitLanding(graphics, this.content, this.morph, emitted, this.panelLayer, origin != null ? 1f - panelMix : landingAmount(this.morph));
        final int bloom = this.content.bloomColor();
        final float v = Math.max(0f, Math.min(1f, this.morph.value()));
        if (bloom != 0) {
            final float fade = this.morph.isClosing() ? Math.min(1f, this.closeClock) * Math.min(1f, v * 4f) : (float) Math.exp(-this.animT / BLOOM_DECAY_TICKS);
            final float alpha = ((bloom >>> 24) & 0xFF) / 255f * fade * this.hostAlpha;
            if (alpha > 0.01f) {
                final float radius = this.bloomSeedRadius * (1.4f + 2.6f * v);
                final int glow = (Math.round(alpha * 255f) << 24) | (bloom & 0x00FFFFFF);
                graphics.radialGlow(this.bloomCx, this.bloomCy, radius, glow);
            }
            final float coreFade = this.morph.isClosing() ? Math.min(1f, this.closeClock * 1.5f) * Math.min(1f, v * 4f) : (float) Math.exp(-this.animT / CORE_FLASH_DECAY_TICKS);
            final float coreAlpha = CORE_FLASH_ALPHA * coreFade * this.hostAlpha;
            if (coreAlpha > 0.01f) {
                final float coreRadius = this.bloomSeedRadius * (CORE_FLASH_RADIUS + CORE_FLASH_GROWTH * v);
                final int core = (Math.round(coreAlpha * 255f) << 24) | 0x00FFFFFF;
                graphics.radialGlow(this.bloomCx, this.bloomCy, coreRadius, core);
            }
        }
        final float reveal;
        if (this.fading) {
            reveal = fadeIn;
        } else if (this.morph.isClosing()) {
            reveal = Math.max(0f, Math.min(1f, (v - CLOSE_CONTENT_GONE) / 0.5f));
        } else if (stillOpening) {
            final float gate = Math.max(0f, Math.min(1f, (v - CONTENT_REVEAL_THRESHOLD) / (CONTENT_REVEAL_FULL - CONTENT_REVEAL_THRESHOLD)));
            reveal = Math.min(gate, Math.min(1f, this.animT / ALPHA_REVEAL_TICKS));
        } else {
            reveal = 1f;
        }
        final LiquidRect current = this.morph.current();
        final float excess = Math.max(0f, current.clampedRadius() - this.panelRect.cornerRadius());
        final float cut = 0.293f * excess;
        final int insetX = Math.round(Math.min(cut, 0.06f * current.w()));
        final int insetY = Math.round(Math.min(cut, 0.06f * current.h()));
        int scX0 = Math.round(current.x()) + insetX;
        int scY0 = Math.round(current.y()) + insetY;
        int scX1 = Math.round(current.x() + current.w()) - insetX;
        int scY1 = Math.round(current.y() + current.h()) - insetY;
        final int winW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        final int winH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        scX0 = Math.max(0, scX0);
        scY0 = Math.max(0, scY0);
        scX1 = Math.min(winW, scX1);
        scY1 = Math.min(winH, scY1);
        if (scX1 > scX0 && scY1 > scY0) {
            final boolean riding = (stillOpening || this.morph.isClosing()) && this.content.ridesContent();
            final int px = Math.round(this.panelRect.x());
            final int py = Math.round(this.panelRect.y());
            final int pw = Math.round(this.panelRect.w());
            final int ph = Math.round(this.panelRect.h());
            final float panelCx = this.panelRect.centerX();
            final float panelCy = this.panelRect.centerY();
            final float ride = riding ? Math.max(1e-3f, Math.min(current.w() / this.panelRect.w(), current.h() / this.panelRect.h())) : 1f;
            int mx = mouseX;
            int my = mouseY;
            if (riding) {
                mx = Math.round(panelCx + (mouseX - current.centerX()) / ride);
                my = Math.round(panelCy + (mouseY - current.centerY()) / ride);
            }
            graphics.enableScissor(scX0, scY0, scX1, scY1);
            final var pose = graphics.pose();
            if (riding) {
                pose.pushMatrix();
                pose.translate(current.centerX(), current.centerY());
                pose.scale(ride, ride);
                pose.translate(-panelCx, -panelCy);
            }
            try {
                this.content.extract(graphics, px, py, pw, ph, mx, my, delta, reveal * this.hostAlpha);
            } finally {
                if (riding) {
                    pose.popMatrix();
                }
            }
            graphics.disableScissor();
        }
        final float handoff = this.handoffFade();
        if (handoff > 0f || this.handoffEmitted > 0f) {
            this.handoffEmitted = handoff;
            final boolean posed = this.content.handoffRides();
            final var hp = graphics.pose();
            if (posed) {
                final float k = 1f - handoffMove(this.closeClock);
                final float ax = this.anchor.centerX();
                final float ay = this.anchor.centerY();
                final float sc = 1f + HANDOFF_RISE * k;
                hp.pushMatrix();
                hp.translate(ax + (this.handoffFromCx - ax) * HANDOFF_DRIFT * k, ay + (this.handoffFromCy - ay) * HANDOFF_DRIFT * k);
                hp.scale(sc, sc);
                hp.translate(-ax, -ay);
            }
            try {
                this.content.extractTriggerHandoff(graphics, handoff);
            } finally {
                if (posed) {
                    hp.popMatrix();
                }
            }
        }
        if (retireAfterFrame) {
            final PopoverContent closed = this.content;
            this.content = null;
            closed.onClosed();
        }
    }

    private float fadeAlpha() {
        if (!this.fading) {
            return 1f;
        }
        final float t = this.morph.isClosing() ? 1f - this.closeClock / FADE_TICKS : this.animT / FADE_TICKS;
        return Math.max(0f, Math.min(1f, t));
    }

    public boolean isOverPanel(final double x, final double y) {
        if (!this.isOpen() || this.content == null) {
            return false;
        }
        final int px = Math.round(this.panelRect.x());
        final int py = Math.round(this.panelRect.y());
        final int pw = Math.round(this.panelRect.w());
        final int ph = Math.round(this.panelRect.h());
        return x >= px && x < px + pw && y >= py && y < py + ph;
    }

    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (this.finishLinger()) {
            return false;
        }
        if (this.content == null) {
            return false;
        }
        final int px = Math.round(this.panelRect.x());
        final int py = Math.round(this.panelRect.y());
        final int pw = Math.round(this.panelRect.w());
        final int ph = Math.round(this.panelRect.h());
        if (event.x() >= px && event.x() < px + pw && event.y() >= py && event.y() < py + ph) {
            this.content.mouseClicked(event, doubleClick, px, py, pw, ph);
            return true;
        }
        if (this.content.mouseClickedOutside(event, doubleClick)) {
            return true;
        }
        if (this.anchor != null && !this.morph.isClosing() && event.x() >= this.anchor.x() && event.x() < this.anchor.x() + this.anchor.w() && event.y() >= this.anchor.y() && event.y() < this.anchor.y() + this.anchor.h()) {
            return true;
        }
        this.close();
        return false;
    }

    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        if (this.finishLinger()) {
            return false;
        }
        if (this.content == null) {
            return false;
        }
        this.content.mouseDragged(event, dragX, dragY, Math.round(this.panelRect.x()), Math.round(this.panelRect.y()), Math.round(this.panelRect.w()), Math.round(this.panelRect.h()));
        return true;
    }

    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
        if (this.finishLinger()) {
            return false;
        }
        if (this.content == null) {
            return false;
        }
        return this.content.mouseScrolled(mouseX, mouseY, scrollX, scrollY, Math.round(this.panelRect.x()), Math.round(this.panelRect.y()), Math.round(this.panelRect.w()), Math.round(this.panelRect.h()));
    }

    public boolean mouseReleased(final MouseButtonEvent event) {
        if (this.finishLinger()) {
            return false;
        }
        if (this.content == null) {
            return false;
        }
        this.content.mouseReleased(event);
        return true;
    }

    public boolean keyPressed(final KeyEvent event) {
        if (this.finishLinger()) {
            return false;
        }
        if (this.content == null) {
            return false;
        }
        if (event.key() == InputConstants.KEY_ESCAPE) {
            if (this.hasOpenAbove() && this.content.keyPressed(event)) {
                return true;
            }
            this.close();
            return true;
        }
        return this.content.keyPressed(event);
    }

    private boolean hasOpenAbove() {
        for (final PopoverHost host : OPEN_HOSTS) {
            if (host != this && host.isOpen() && host.panelLayer > this.panelLayer) {
                return true;
            }
        }
        return false;
    }

    public boolean charTyped(final CharacterEvent event) {
        if (this.finishLinger()) {
            return false;
        }
        return this.content != null && this.content.charTyped(event);
    }
}
