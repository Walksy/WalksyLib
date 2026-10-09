package main.walksy.lib.core.hud;

import main.walksy.lib.api.hud.HudElement;
import main.walksy.lib.api.hud.HudPlacement;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.render.WLGraphics2D;

import net.minecraft.client.Minecraft;

public final class HudSpace implements HudPlacement {
    private String id;
    private final String name;
    private final HudElement element;
    private float anchorX = 0.5f;
    private float anchorY = 0.5f;
    private float offsetX;
    private float offsetY;
    private float scale = 1f;
    private static final float SNAP = 2.5f;
    private boolean legacy;
    private float legacyX;
    private float legacyY;
    private static final float MIN_SCALE = 0.25f;
    private static final float MAX_SCALE = 8f;

    public HudSpace(final String id, final String name, final HudElement element) {
        this.id = id;
        this.name = name;
        this.element = element;
        this.offsetX = element.defaultOffsetX();
        this.offsetY = element.defaultOffsetY();
    }

    @Override
    public String id() {
        return this.id;
    }

    public boolean isBound() {
        return this.id != null;
    }

    void bind(final String id) {
        this.id = id;
    }

    public String name() {
        return this.name;
    }

    @Override
    public boolean visible() {
        return this.element.visible();
    }

    public HudElement element() {
        return this.element;
    }

    public float scale() {
        return this.scale;
    }

    public void setScale(final float scale) {
        final float step = this.element.scaleStep();
        float value = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
        if (step > 0f) {
            value = Math.min(MAX_SCALE, Math.max(step, Math.round(value / step) * step));
        }
        this.scale = value;
        WalksyLib.getInstance().getHudSpaces().markDirty();
    }

    public float anchorX() {
        this.resolveLegacy();
        return this.anchorX;
    }

    public float anchorY() {
        this.resolveLegacy();
        return this.anchorY;
    }

    public float offsetX() {
        this.resolveLegacy();
        return this.offsetX;
    }

    public float offsetY() {
        this.resolveLegacy();
        return this.offsetY;
    }

    public boolean isDefault() {
        this.resolveLegacy();
        return this.anchorX == 0.5f && this.anchorY == 0.5f && this.offsetX == this.element.defaultOffsetX() && this.offsetY == this.element.defaultOffsetY() && this.scale == 1f;
    }

    public void reset() {
        this.legacy = false;
        this.legacyX = 0f;
        this.legacyY = 0f;
        this.anchorX = 0.5f;
        this.anchorY = 0.5f;
        this.offsetX = this.element.defaultOffsetX();
        this.offsetY = this.element.defaultOffsetY();
        this.scale = 1f;
        WalksyLib.getInstance().getHudSpaces().markDirty();
    }

    void setAnchored(final float anchorX, final float anchorY, final float offsetX, final float offsetY) {
        this.legacy = false;
        this.anchorX = snapAnchor(anchorX);
        this.anchorY = snapAnchor(anchorY);
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    void setLegacyFraction(final float x, final float y) {
        this.legacy = true;
        this.legacyX = x;
        this.legacyY = y;
    }

    boolean isLegacy() {
        return this.legacy;
    }

    float legacyX() {
        return this.legacyX;
    }

    float legacyY() {
        return this.legacyY;
    }

    public void setScreenPosition(final float screenX, final float screenY) {
        this.legacy = false;
        final float screenWidth = screenWidth();
        final float screenHeight = screenHeight();
        final float w = this.width();
        final float h = this.height();
        final float x = onScreen(screenX, w, screenWidth);
        final float y = onScreen(screenY, h, screenHeight);
        this.anchorX = pickAnchor(x, w, screenWidth);
        this.anchorY = pickAnchor(y, h, screenHeight);
        this.offsetX = snapToAnchor(subPixel(x - this.anchoredX(screenWidth, w)));
        this.offsetY = snapToAnchor(subPixel(y - this.anchoredY(screenHeight, h)));
        WalksyLib.getInstance().getHudSpaces().markDirty();
    }

    private static float snapToAnchor(final float offset) {
        return Math.abs(offset) <= SNAP ? 0f : offset;
    }

    private static float subPixel(final float offset) {
        return Math.round(offset * 2f) / 2f;
    }

    public boolean isOnAnchorX() {
        this.resolveLegacy();
        return this.offsetX == 0f;
    }

    public boolean isOnAnchorY() {
        this.resolveLegacy();
        return this.offsetY == 0f;
    }

    public void nudge(final float dx, final float dy) {
        this.resolveLegacy();
        final float screenWidth = screenWidth();
        final float screenHeight = screenHeight();
        final float w = this.width();
        final float h = this.height();
        final float x = onScreen(this.anchoredX(screenWidth, w) + this.offsetX + dx, w, screenWidth);
        final float y = onScreen(this.anchoredY(screenHeight, h) + this.offsetY + dy, h, screenHeight);
        this.offsetX = subPixel(x - this.anchoredX(screenWidth, w));
        this.offsetY = subPixel(y - this.anchoredY(screenHeight, h));
        WalksyLib.getInstance().getHudSpaces().markDirty();
    }

    private static float pickAnchor(final float position, final float size, final float screenSize) {
        final float fromStart = Math.abs(position);
        final float fromEnd = Math.abs(screenSize - size - position);
        final float fromCentre = Math.abs(position + size / 2f - screenSize / 2f);
        if (fromCentre <= fromStart && fromCentre <= fromEnd) {
            return 0.5f;
        }
        return fromStart <= fromEnd ? 0f : 1f;
    }

    private static float onScreen(final float position, final float size, final float screenSize) {
        final float slack = screenSize - size;
        return Math.max(Math.min(0f, slack), Math.min(Math.max(0f, slack), position));
    }

    private static float snapAnchor(final float anchor) {
        if (anchor <= 0.25f) {
            return 0f;
        }
        return anchor >= 0.75f ? 1f : 0.5f;
    }

    private void resolveLegacy() {
        if (!this.legacy) {
            return;
        }
        final float screenWidth = screenWidth();
        final float screenHeight = screenHeight();
        if (screenWidth <= 1f || screenHeight <= 1f) {
            return;
        }
        this.legacy = false;
        this.setScreenPosition(this.legacyX * screenWidth, this.legacyY * screenHeight);
    }

    @Override
    public float screenX() {
        this.resolveLegacy();
        final float screenWidth = screenWidth();
        final float w = this.width();
        return onScreen(this.anchoredX(screenWidth, w) + this.offsetX, w, screenWidth);
    }

    @Override
    public float screenY() {
        this.resolveLegacy();
        final float screenHeight = screenHeight();
        final float h = this.height();
        return onScreen(this.anchoredY(screenHeight, h) + this.offsetY, h, screenHeight);
    }

    private float anchoredX(final float screenWidth, final float w) {
        return (float) Math.floor(this.anchorX * (screenWidth - w));
    }

    private float anchoredY(final float screenHeight, final float h) {
        return (float) Math.floor(this.anchorY * (screenHeight - h));
    }

    @Override
    public float width() {
        return Math.max(1f, this.element.width(this.scale));
    }

    @Override
    public float height() {
        return Math.max(1f, this.element.height(this.scale));
    }

    private static float screenWidth() {
        return Math.max(1, Minecraft.getInstance().getWindow().getGuiScaledWidth());
    }

    private static float screenHeight() {
        return Math.max(1, Minecraft.getInstance().getWindow().getGuiScaledHeight());
    }

    public void render(final WLGraphics2D graphics, final boolean editing) {
        this.element.render(graphics, this.screenX(), this.screenY(), this.scale, editing);
    }
}
