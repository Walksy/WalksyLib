package main.walksy.lib.core.gui.widget;
import com.mojang.blaze3d.platform.InputConstants;

import main.walksy.lib.core.gui.style.UiStyle;
import main.walksy.lib.core.render.TextQueueMark;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.modal.PopoverHost;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.widget.options.control.LiquidKnob;
import main.walksy.lib.core.render.ContentStretch;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.render.texture.ListIcons;
import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.utils.MathUtils;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicLong;

public class ResetButton extends WLWidget {
    private static final AtomicLong FADE_KEYS = new AtomicLong(0x2E5E7B07L);
    private static final float HOVER_SPEED = 14f;
    private static final float AVAIL_SPEED = 10f;
    private static final float PRESS_IN_SPEED = 22f;
    private static final float PRESS_OUT_SPEED = 14f;
    private static final float PRESS_GROW = 0.06f;
    private static final float CAP_HEIGHT_RATIO = 0.70f;
    private static final float BASELINE_LIFT = 0.5f;
    private static final float TEXT_SIZE_HEIGHT_RATIO = 0.5f;
    private static final float TEXT_SIZE_MIN = Type.CAPTION1.size();
    private static final float TEXT_SIZE_MAX = Type.TITLE2.size();
    private static final float HORIZONTAL_PADDING_RATIO = 0.5f;
    private static final float OPTICAL_CENTER_NUDGE = 0.07f;
    private static final int GLASS_LAYER = 6;
    private static final float ICON_SIZE_RATIO = 0.62f;
    private static final float DIMMED_ICON_ALPHA = 0.4f;
    private final String label;
    private final Runnable onRelease;
    private final float cornerRadius;
    private final float textSize;
    private final GlassStyle style;
    private final boolean popoverContent;
    private final float uiScale = WLScreen.uiScaleFactor();
    private boolean pressed;
    private float warpX;
    private float warpY;
    private float hoverAnim;
    private float pressAnim;
    private float availAnim = 1f;
    private @Nullable String iconItem;
    private boolean iconDimmed;

    public ResetButton(final int x, final int y, final int width, final int height, final String label, final Runnable onRelease) {
        this(x, y, width, height, label, onRelease, GLASS_LAYER);
    }

    public ResetButton(final int x, final int y, final int width, final int height, final String label, final Runnable onRelease, final int glassLayer) {
        super(x, y, width, height);
        this.label = label == null ? "" : label;
        this.onRelease = onRelease;
        this.cornerRadius = 0.5f * Math.min(width, height);
        this.textSize = Mth.clamp(height * TEXT_SIZE_HEIGHT_RATIO, TEXT_SIZE_MIN * this.uiScale, TEXT_SIZE_MAX * this.uiScale);
        this.style = knobStyle(glassLayer).fadeKey(FADE_KEYS.incrementAndGet());
        this.popoverContent = glassLayer > PopoverHost.PANEL_LAYER;
        this.setMessage(Component.literal(this.label));
    }

    public static GlassStyle knobStyle(final int glassLayer) {
        return LiquidKnob.bevelKnobStyle(glassLayer).smoothing(0f);
    }

    public static void lift(final boolean popover, final Runnable body) {
        lift(popover, PopoverHost.PANEL_LAYER, body);
    }

    public static void lift(final boolean popover, final int boundaryLayer, final Runnable body) {
        final boolean lift = popover ? !WLRenderSystem.contentStratum().isPopoverActive() : !WLRenderSystem.contentStratum().isActive();
        final TextQueueMark mark = lift ? WLRenderSystem.textMark() : null;
        if (lift && popover) {
            WLRenderSystem.contentStratum().beginPopover(boundaryLayer);
        } else if (lift) {
            WLRenderSystem.contentStratum().begin();
        }
        try {
            body.run();
        } finally {
            if (lift && popover) {
                WLRenderSystem.contentStratum().endPopover();
                WLRenderSystem.markPopoverText(mark);
            } else if (lift) {
                WLRenderSystem.contentStratum().end();
                WLRenderSystem.markContentText(mark);
            }
        }
    }

    public void icon(@Nullable final String itemId, final boolean dimmed) {
        this.iconItem = itemId;
        this.iconDimmed = dimmed;
    }

    public static int preferredWidth(final String label, final int height) {
        final float uiScale = WLScreen.uiScaleFactor();
        final float textSize = Mth.clamp(height * TEXT_SIZE_HEIGHT_RATIO, TEXT_SIZE_MIN * uiScale, TEXT_SIZE_MAX * uiScale);
        final float textWidth = Fonts.SFPRO_DISPLAY.width(label, textSize);
        final float padding = height * HORIZONTAL_PADDING_RATIO;
        return Math.max(height, Math.round(textWidth + padding * 2f));
    }

    private GlassStyle materialFor(final float available) {
        if (available >= 0.999f) {
            return this.style;
        }
        final GlassStyle flat = GlassStyle.inactive(this.style);
        if (available <= 0.001f) {
            return flat;
        }
        final GlassStyle blend = GlassStyle.lerp(flat, this.style, available);
        blend.layer(this.style.getLayer());
        if (this.style.hasFadeKey()) {
            blend.fadeKey(this.style.getFadeKey());
        }
        return blend;
    }

    @Override
    protected void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float a) {
        final float dt = WLRenderSystem.frameDeltaSeconds();
        if (this.pressed && !WLButton.isLeftMouseDown()) {
            this.pressed = false;
        }
        final boolean held = this.pressed;
        final boolean hovered = (this.isHovered() || held) && this.active;
        WLRenderSystem.cursors().requestControl(this.isHovered() || held, this.active);
        this.hoverAnim = MathUtils.approach(this.hoverAnim, hovered ? 1f : 0f, HOVER_SPEED, dt);
        this.pressAnim = MathUtils.approach(this.pressAnim, held ? 1f : 0f, held ? PRESS_IN_SPEED : PRESS_OUT_SPEED, dt);
        this.warpX = MathUtils.approach(this.warpX, held && !UiStyle.minecraft() ? WLButton.warpTarget(mouseX, this.getX() + this.getWidth() / 2f, this.getWidth() / 2f, this.uiScale) : 0f, WLButton.WARP_SPEED, dt);
        this.warpY = MathUtils.approach(this.warpY, held && !UiStyle.minecraft() ? WLButton.warpTarget(mouseY, this.getY() + this.getHeight() / 2f, this.getHeight() / 2f, this.uiScale) : 0f, WLButton.WARP_SPEED, dt);
        this.availAnim = MathUtils.approach(this.availAnim, this.active ? 1f : 0f, AVAIL_SPEED, dt);
        final float alpha = Mth.clamp(this.getAlpha(), 0f, 1f);
        if (alpha <= 0.001f) {
            return;
        }
        final float grow = (UiStyle.minecraft() ? 0f : PRESS_GROW * this.pressAnim);
        final float growX = this.getWidth() * grow / 2f;
        final float growY = this.getHeight() * grow / 2f;
        final float gx = this.getX() - growX;
        final float gy = this.getY() - growY;
        final float gw = Math.max(1f, this.getWidth() + growX * 2f);
        final float gh = Math.max(1f, this.getHeight() + growY * 2f);
        final GlassStyle base = this.materialFor(this.availAnim).scaled(this.uiScale);
        final GlassStyle drawStyle = alpha < 1f ? base.copy().opacity(base.getOpacity() * alpha) : base;
        graphics.glass()
            .dimensions(gx, gy, gw, gh)
            .cornerRadius(Math.min(this.cornerRadius * (1f + grow), Math.min(gw, gh) / 2f))
            .style(drawStyle)
            .hover(this.active ? this.hoverAnim : 0f)
            .stretch(this.warpX, this.warpY)
            .extract();
        if (this.label.isEmpty() && this.iconItem == null) {
            return;
        }
        if (hovered && !this.label.isEmpty()) {
            Fonts.SFPRO_DISPLAY.warmStretched(this.textSize);
        }
        lift(this.popoverContent, () -> {
            if (this.iconItem != null) {
                this.extractIcon(graphics, alpha);
            }
            if (!this.label.isEmpty()) {
                this.extractLabel(graphics, alpha);
            }
        });
    }

    private void extractIcon(final WLGraphics2D graphics, final float alpha) {
        final float grow = (UiStyle.minecraft() ? 0f : PRESS_GROW * this.pressAnim);
        final float growX = this.getWidth() * grow / 2f;
        final float growY = this.getHeight() * grow / 2f;
        final float gx = this.getX() - growX;
        final float gy = this.getY() - growY;
        final float gw = Math.max(1f, this.getWidth() + growX * 2f);
        final float gh = Math.max(1f, this.getHeight() + growY * 2f);
        final float size = Math.min(this.getWidth(), this.getHeight()) * ICON_SIZE_RATIO;
        final float ix = this.getX() + (this.getWidth() - size) / 2f;
        final float iy = this.getY() + (this.getHeight() - size) / 2f;
        final float iconAlpha = alpha * (this.iconDimmed ? DIMMED_ICON_ALPHA : 1f);
        ContentStretch.run(ContentStretch.of(graphics.pose(), gx, gy, gw, gh, this.warpX, this.warpY, 1f + grow), () -> ListIcons.extractItem(graphics, this.iconItem, ix, iy, size, iconAlpha));
    }

    private void extractLabel(final WLGraphics2D graphics, final float alpha) {
        final float grow = (UiStyle.minecraft() ? 0f : PRESS_GROW * this.pressAnim);
        final float growX = this.getWidth() * grow / 2f;
        final float growY = this.getHeight() * grow / 2f;
        final float gx = this.getX() - growX;
        final float gy = this.getY() - growY;
        final float gw = Math.max(1f, this.getWidth() + growX * 2f);
        final float gh = Math.max(1f, this.getHeight() + growY * 2f);
        final float size = this.textSize;
        final float textWidth = Fonts.SFPRO_DISPLAY.width(this.label, size);
        final float cx = this.getX() + this.getWidth() / 2f;
        final float cy = this.getY() + this.getHeight() / 2f;
        final float tx = cx - textWidth / 2f - OPTICAL_CENTER_NUDGE * size;
        final float capHeight = size * CAP_HEIGHT_RATIO;
        final float baseline = cy + capHeight * 0.5f;
        final float ty = Fonts.SFPRO_DISPLAY.topForBaseline(baseline, size) - BASELINE_LIFT * this.uiScale;
        final int color = ColorUtils.withAlpha(this.active ? Theme.get().label() : Theme.get().tertiaryLabel(), alpha);
        ContentStretch.run(ContentStretch.of(graphics.pose(), gx, gy, gw, gh, this.warpX, this.warpY, 1f + grow), () -> Fonts.SFPRO_DISPLAY.extract(graphics, this.label, tx, ty, size, color));
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !this.active || !this.isMouseOver(event.x(), event.y())) {
            return false;
        }
        this.pressed = true;
        return true;
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        return this.pressed;
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        if (!this.pressed) {
            return false;
        }
        this.pressed = false;
        if (this.isMouseOver(event.x(), event.y())) {
            if (this.onRelease != null) {
                this.onRelease.run();
            }
        }
        return true;
    }
}
