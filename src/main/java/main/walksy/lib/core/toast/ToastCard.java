package main.walksy.lib.core.toast;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.config.internal.AppearanceConfig;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.style.UiStyle;
import main.walksy.lib.core.gui.widget.options.control.LiquidKnob;
import main.walksy.lib.core.render.DevicePixels;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.msdf.MsdfFont;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.render.glass.api.LiquidGlassConstants;
import main.walksy.lib.core.utils.ColorUtils;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

final class ToastCard {
    private static final AtomicLong FADE_KEYS = new AtomicLong(0x707A5700L);
    private static final float PAD_X = 12f;
    private static final float PAD_Y = 7f;
    private static final float ICON_SIZE = 22f;
    private static final float ICON_GAP = 10f;
    private static final float CORNER_RADIUS = 16f;
    private static final float MIN_WIDTH = 148f;
    private static final float MAX_WIDTH = 236f;
    private static final float TITLE_LINE = 14f;
    private static final float MESSAGE_LINE = 12f;
    private static final float TITLE_MESSAGE_GAP = 1f;
    private static final int MAX_MESSAGE_LINES = 3;
    private static final float CAP_HEIGHT_RATIO = 0.59043f;

    private static final float MARQUEE_START_HOLD = 1.2f;
    private static final float MARQUEE_SPEED = 30f;
    private static final float MARQUEE_GAP = 28f;
    private static final float MARQUEE_END_HOLD = 1.5f;
    private static final float MARQUEE_TOLERANCE = 0.5f;
    private static final float MARQUEE_FADE = 10f;

    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final float MESSAGE_LABEL_ALPHA = 0.72f;
    private static final float CARD_TINT_ALPHA = 0.20f;
    private static final int CARD_BLUR = 2;
    private static final float CARD_LENS_THICKNESS = 6f;
    private static final float CARD_LENS_REACH = 0.34f;

    private static final float ENTER_OMEGA = 13f;
    private static final float ENTER_ZETA = 0.72f;
    private static final float LEAVE_OMEGA = 17f;
    private static final float LEAVE_ZETA = 0.95f;
    private static final float SLOT_OMEGA = 17f;
    private static final float SLOT_ZETA = 1f;
    private static final float GONE_BELOW = 0.004f;
    private static final float MAX_SPRING_STEP = 1f / 120f;

    private static final float APPEAR_END = 0.30f;
    private static final float DESCEND_END = 0.45f;
    private static final float GROW_START = 0.20f;
    private static final float GROW_END = 0.80f;
    private static final float GLIDE_START = 0.30f;
    private static final float CONTENT_START = 0.70f;
    private static final float CONTENT_END = 0.92f;
    private static final float SMALL_SCALE = 0.58f;
    private static final Identifier MINECRAFT_FRAME = Identifier.withDefaultNamespace("textures/gui/sprites/toast/advancement.png");
    private static final float FRAME_WIDTH = 160f;
    private static final float FRAME_HEIGHT = 32f;
    private static final float FRAME_BORDER = 4f;
    private static final float MAX_DROP = 140f;
    private static final float SLIDE_RIGHT = 30f;

    private final long fadeKey = FADE_KEYS.incrementAndGet();
    private final Spring presence = new Spring();
    private final Spring slot = new Spring();
    private WLToast spec;
    private float age;
    private boolean leaving;
    private boolean dismissRequested;
    private boolean slotPlaced;
    private float uiScale = -1f;
    private float width;
    private float height;
    private @Nullable List<String> messageLines;
    private String title = "";
    private float titleWidth;
    private float textWidth;
    private float marqueeClock;

    ToastCard(final WLToast spec) {
        this.spec = spec;
        this.presence.configure(ENTER_OMEGA, ENTER_ZETA);
        this.presence.reset(0f);
        this.presence.target = 1f;
        this.slot.configure(SLOT_OMEGA, SLOT_ZETA);
    }

    void replaceWith(final WLToast next) {
        this.spec = next;
        this.age = 0f;
        this.marqueeClock = 0f;
        this.uiScale = -1f;
        if (this.leaving) {
            this.leaving = false;
            this.dismissRequested = false;
            this.presence.configure(ENTER_OMEGA, ENTER_ZETA);
            this.presence.target = 1f;
        }
    }

    WLToast spec() {
        return this.spec;
    }

    @Nullable Object id() {
        return this.spec.getId();
    }

    void requestDismiss() {
        this.dismissRequested = true;
    }

    boolean isGone() {
        return this.leaving && this.presence.value < GONE_BELOW;
    }

    float height() {
        return this.height;
    }

    float occupancy() {
        return this.leaving ? Math.max(0f, Math.min(1f, this.presence.value)) : 1f;
    }

    void update(final float dt, final float targetY) {
        if (!this.slotPlaced) {
            this.slot.reset(targetY);
            this.slotPlaced = true;
        }
        this.slot.target = targetY;
        this.marqueeClock += Math.max(0f, dt);
        if (!this.leaving) {
            this.age += dt;
            final float timed = this.spec.getSeconds() * WalksyLib.getInstance().getAppearancePage().toastTimeScale();
            final float seconds = timed > 0f ? Math.max(timed, this.marqueeReadSeconds()) : timed;
            if (this.dismissRequested || (seconds > 0f && this.age >= seconds)) {
                this.leaving = true;
                this.presence.configure(LEAVE_OMEGA, LEAVE_ZETA);
                this.presence.target = 0f;
            }
        }
        float remaining = Math.max(0f, dt);
        while (remaining > 0f) {
            final float step = Math.min(MAX_SPRING_STEP, remaining);
            this.presence.step(step);
            this.slot.step(step);
            remaining -= step;
        }
    }

    void measure(final float scale, final float available) {
        final MsdfFont titleFont = Type.HEADLINE.emphasized();
        final MsdfFont messageFont = Type.SUBHEADLINE.regular();
        final float padX = PAD_X * scale;
        final float padY = PAD_Y * scale;
        final float ceiling = Math.max(padX * 2f + 1f, Math.min(MAX_WIDTH * scale, available));
        final ToastContent content = this.spec.getContent();
        if (content != null) {
            this.uiScale = scale;
            this.messageLines = null;
            this.width = Math.min(Math.max(ceiling, available), padX * 2f + Math.max(1f, content.width(scale)));
            this.height = padY * 2f + Math.max(1f, content.height(scale));
            return;
        }
        final float titleSize = Type.HEADLINE.size() * scale;
        final float messageSize = Type.SUBHEADLINE.size() * scale;
        final float iconSpan = this.spec.resolveIcon() == null ? 0f : (ICON_SIZE + ICON_GAP) * scale;
        final String rawTitle = this.spec.getTitle();
        final String rawMessage = this.spec.getMessage();
        final float roomForText = Math.max(1f, ceiling - padX * 2f - iconSpan);
        final float natural = Math.max(titleFont.width(rawTitle, titleSize), rawMessage == null ? 0f : messageFont.width(rawMessage, messageSize));
        final float fitted = Math.max(Math.min(roomForText, MIN_WIDTH * scale - padX * 2f - iconSpan), Math.min(roomForText, natural));
        this.title = rawTitle;
        this.titleWidth = titleFont.width(rawTitle, titleSize);
        final float textWidth = Math.max(fitted, Math.min(Math.max(1f, available - padX * 2f - iconSpan), this.titleWidth));
        this.textWidth = textWidth;
        this.messageLines = rawMessage == null ? null : ToastText.wrap(messageFont, rawMessage, messageSize, textWidth, MAX_MESSAGE_LINES);
        final int lines = this.messageLines == null ? 0 : this.messageLines.size();
        final float textHeight = TITLE_LINE * scale + (lines == 0 ? 0f : (TITLE_MESSAGE_GAP + MESSAGE_LINE * lines) * scale);
        this.uiScale = scale;
        this.width = padX * 2f + iconSpan + textWidth;
        this.height = padY * 2f + Math.max(textHeight, iconSpan == 0f ? 0f : ICON_SIZE * scale);
    }

    void extract(final WLGraphics2D graphics, final float windowWidth, final float margin, final ToastCorner corner) {
        if (this.uiScale <= 0f) {
            return;
        }
        final float scale = this.uiScale;
        final float reveal = Math.max(0f, this.presence.value);
        final float alpha = smoothstep(reveal / APPEAR_END);
        if (alpha <= 0.002f) {
            return;
        }
        final boolean right = corner.isRight();
        final float restX = right ? windowWidth - margin - this.width : margin;
        final float restY = this.slot.value;
        final float descend = smoothstep(reveal / DESCEND_END);
        final float grow = smoothstep((reveal - GROW_START) / (GROW_END - GROW_START));
        final float glide = settle((reveal - GLIDE_START) / (1f - GLIDE_START));
        final boolean minecraft = UiStyle.minecraft();
        final float body = minecraft ? 1f : SMALL_SCALE + (1f - SMALL_SCALE) * grow;
        final float w = this.width * body;
        final float h = this.height * body;
        final float drop = Math.min(restY + this.height + margin, MAX_DROP * scale);
        final float y = restY - (1f - descend) * drop;
        final float slide = (1f - glide) * SLIDE_RIGHT * scale;
        final float pinned = right ? restX + this.width + slide : restX - slide;
        final float x = right ? pinned - w : pinned;
        if (minecraft) {
            extractMinecraftFrame(graphics, x, y, w, h, scale, alpha);
        } else {
            graphics.glass()
                .dimensions(x, y, w, h)
                .cornerRadius(Math.min(CORNER_RADIUS * scale, 0.5f * Math.min(w, h)))
                .style(cardStyle(scale).fadeKey(this.fadeKey).opacity(alpha))
                .extract();
        }
        final float content = smoothstep((reveal - CONTENT_START) / (CONTENT_END - CONTENT_START));
        if (content <= 0.002f) {
            return;
        }
        this.extractBody(graphics, x, y, w, h, scale, content);
    }

    private static void extractMinecraftFrame(final WLGraphics2D graphics, final float x, final float y, final float w, final float h, final float scale, final float alpha) {
        final float guiScale = DevicePixels.perLogicalY();
        final float texel = Math.max(1f, Math.round(guiScale * scale)) / guiScale;
        final float border = Math.min(FRAME_BORDER * texel, 0.5f * Math.min(w, h));
        final float left = DevicePixels.snapX(x, graphics.pose());
        final float top = DevicePixels.snapY(y, graphics.pose());
        final float right = DevicePixels.snapX(x + w, graphics.pose());
        final float bottom = DevicePixels.snapY(y + h, graphics.pose());
        final float[] xs = {left, left + border, right - border, right};
        final float[] ys = {top, top + border, bottom - border, bottom};
        final float bu = FRAME_BORDER / FRAME_WIDTH;
        final float bv = FRAME_BORDER / FRAME_HEIGHT;
        final float[] us = {0f, bu, 1f - bu, 1f};
        final float[] vs = {0f, bv, 1f - bv, 1f};
        final int color = ColorUtils.withAlpha(0xFFFFFFFF, alpha);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                final float cw = xs[col + 1] - xs[col];
                final float ch = ys[row + 1] - ys[row];
                if (cw > 0f && ch > 0f) {
                    graphics.blitUv(MINECRAFT_FRAME, xs[col], ys[row], cw, ch, us[col], vs[row], us[col + 1], vs[row + 1], color);
                }
            }
        }
    }

    private void extractBody(final WLGraphics2D graphics, final float x, final float y, final float w, final float h, final float scale, final float alpha) {
        final float padX = PAD_X * scale;
        final float padY = PAD_Y * scale;
        final ToastContent custom = this.spec.getContent();
        if (custom != null) {
            custom.extract(graphics, x + padX, y + padY, w - padX * 2f, h - padY * 2f, alpha, scale, WLToastManager.CONTENT_LAYER);
            return;
        }
        final ToastIcon icon = this.spec.resolveIcon();
        float textX = x + padX;
        if (icon != null) {
            final float iconSize = ICON_SIZE * scale;
            icon.extract(graphics, x + padX, y + (h - iconSize) / 2f, iconSize, this.spec.resolveAccent(), alpha, WLToastManager.CONTENT_LAYER);
            textX += iconSize + ICON_GAP * scale;
        }
        final int lines = this.messageLines == null ? 0 : this.messageLines.size();
        final float textHeight = TITLE_LINE * scale + (lines == 0 ? 0f : (TITLE_MESSAGE_GAP + MESSAGE_LINE * lines) * scale);
        float lineTop = y + (h - textHeight) / 2f;
        final float titleSize = Type.HEADLINE.size() * scale;
        final int titleColor = ColorUtils.withAlpha(TITLE_COLOR, alpha);
        if (this.titleOverflow() > 0f) {
            final float column = Math.max(0f, Math.min(this.textWidth, x + w - padX - textX));
            this.extractMarquee(graphics, textX, column, lineTop, y, h, scale, titleSize, titleColor);
        } else {
            extractLine(graphics, Type.HEADLINE.emphasized(), this.title, textX, lineTop, TITLE_LINE * scale, titleSize, titleColor);
        }
        lineTop += (TITLE_LINE + TITLE_MESSAGE_GAP) * scale;
        if (lines == 0) {
            return;
        }
        final float messageSize = Type.SUBHEADLINE.size() * scale;
        final int messageColor = ColorUtils.withAlpha(TITLE_COLOR, alpha * MESSAGE_LABEL_ALPHA);
        for (final String line : this.messageLines) {
            extractLine(graphics, Type.SUBHEADLINE.regular(), line, textX, lineTop, MESSAGE_LINE * scale, messageSize, messageColor);
            lineTop += MESSAGE_LINE * scale;
        }
    }

    private float titleOverflow() {
        final float overflow = this.titleWidth - this.textWidth;
        return overflow > MARQUEE_TOLERANCE ? overflow : 0f;
    }

    private float marqueeReadSeconds() {
        final float overflow = this.titleOverflow();
        if (overflow <= 0f || this.uiScale <= 0f) {
            return 0f;
        }
        return MARQUEE_START_HOLD + overflow / (MARQUEE_SPEED * this.uiScale) + MARQUEE_END_HOLD;
    }

    private void extractMarquee(final WLGraphics2D graphics, final float textX, final float column, final float lineTop, final float cardY, final float cardH, final float scale, final float size, final int color) {
        final int left = (int) Math.ceil(textX);
        final int right = (int) Math.floor(textX + column);
        if (right <= left) {
            return;
        }
        final float speed = MARQUEE_SPEED * scale;
        final float travel = this.titleWidth + MARQUEE_GAP * scale;
        final float cycle = MARQUEE_START_HOLD + travel / speed;
        final float t = this.marqueeClock % cycle;
        final float offset = t <= MARQUEE_START_HOLD ? 0f : (t - MARQUEE_START_HOLD) * speed;
        final float start = textX - offset;
        final int top = (int) Math.floor(cardY);
        final int bottom = (int) Math.ceil(cardY + cardH);
        final int fade = Math.min(Math.round(MARQUEE_FADE * scale), (right - left) / 4);
        final int leftFade = fade <= 0 ? 0 : Math.round(fade * Math.min(1f, offset / fade));
        this.extractMarqueeBand(graphics, left + leftFade, right - fade, top, bottom, start, travel, lineTop, scale, size, color);
        for (int i = 0; i < leftFade; i++) {
            this.extractMarqueeBand(graphics, left + i, left + i + 1, top, bottom, start, travel, lineTop, scale, size, ColorUtils.withAlpha(color, (i + 0.5f) / leftFade));
        }
        for (int i = 0; i < fade; i++) {
            this.extractMarqueeBand(graphics, right - i - 1, right - i, top, bottom, start, travel, lineTop, scale, size, ColorUtils.withAlpha(color, (i + 0.5f) / fade));
        }
    }

    private void extractMarqueeBand(final WLGraphics2D graphics, final int x0, final int x1, final int y0, final int y1, final float start, final float travel, final float lineTop, final float scale, final float size, final int color) {
        if (x1 <= x0 || (color >>> 24) == 0) {
            return;
        }
        final MsdfFont font = Type.HEADLINE.emphasized();
        final float lineHeight = TITLE_LINE * scale;
        graphics.enableScissor(x0, y0, x1, y1);
        try {
            if (start + this.titleWidth > x0) {
                extractLine(graphics, font, this.title, start, lineTop, lineHeight, size, color);
            }
            final float trailing = start + travel;
            if (trailing < x1) {
                extractLine(graphics, font, this.title, trailing, lineTop, lineHeight, size, color);
            }
        } finally {
            graphics.disableScissor();
        }
    }

    private static float smoothstep(final float t) {
        final float k = Math.max(0f, Math.min(1f, t));
        return k * k * (3f - 2f * k);
    }

    private static float settle(final float t) {
        if (t <= 0f) {
            return 0f;
        }
        return t >= 1f ? t : t * t * (3f - 2f * t);
    }

    private static void extractLine(final WLGraphics2D graphics, final MsdfFont font, final String text, final float x, final float lineTop, final float lineHeight, final float size, final int color) {
        final float baseline = lineTop + lineHeight / 2f + size * CAP_HEIGHT_RATIO * 0.5f;
        font.extract(graphics, text, x, font.topForBaseline(baseline, size), size, color);
    }

    private static GlassStyle cardStyle(final float scale) {
        final AppearanceConfig appearance = WalksyLib.getInstance().getAppearancePage();
        return LiquidKnob.bevelKnobStyle(WLToastManager.PANEL_LAYER)
            .blurRadius(CARD_BLUR)
            .tint(appearance.toastTint(), CARD_TINT_ALPHA * appearance.toastOpacityScale())
            .refractionThickness(CARD_LENS_THICKNESS)
            .refractionReach(CARD_LENS_REACH)
            .refractionDispersion(0f)
            .toneRange(LiquidGlassConstants.OVER_WORLD_TONE_FLOOR, LiquidGlassConstants.OVER_WORLD_TONE_CEILING)
            .chroma(Theme.get().chroma())
            .scaled(scale);
    }

    private static final class Spring {
        private float value;
        private float velocity;
        private float target;
        private float omega = 1f;
        private float zeta = 1f;

        void configure(final float omega, final float zeta) {
            this.omega = omega;
            this.zeta = zeta;
        }

        void reset(final float at) {
            this.value = at;
            this.velocity = 0f;
            this.target = at;
        }

        void step(final float dt) {
            final float k = this.omega * this.omega;
            final float c = 2f * this.zeta * this.omega;
            this.velocity += (k * (this.target - this.value) - c * this.velocity) * dt;
            this.value += this.velocity * dt;
        }
    }
}
