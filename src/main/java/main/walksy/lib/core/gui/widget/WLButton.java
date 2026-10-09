package main.walksy.lib.core.gui.widget;

import com.mojang.blaze3d.platform.InputConstants;
import main.walksy.lib.core.gui.style.UiStyle;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.modal.PopoverHost;
import main.walksy.lib.core.gui.widget.options.control.LiquidKnob;
import main.walksy.lib.core.platform.WLClient;
import main.walksy.lib.core.render.ContentStretch;
import main.walksy.lib.core.render.DevicePixels;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.render.glass.api.LiquidGlassConstants;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.utils.MathUtils;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicLong;

public class WLButton extends WLWidget {
    private static final AtomicLong FADE_KEYS = new AtomicLong(0xB07701L);
    private static final float HOVER_SPEED = 14f;
    private static final float PRESS_IN_SPEED = 22f;
    private static final float PRESS_OUT_SPEED = 14f;
    private static final float PRESS_GROW = 0.06f;
    private static final float WARP_MAX = 8f;
    private static final float WARP_REACH = 26f;
    static final float WARP_SPEED = 16f;
    private static final float BASELINE_CENTER = 0.572f;
    private String label;
    private PressAction onPress;
    private float cornerRadius;
    private float textSize;
    private int textColor = ColorUtils.withAlpha(Theme.get().label(), 0.95f);
    private int textColorHovered = Theme.get().label();
    private GlassStyle style;
    private final float uiScale = WLScreen.uiScaleFactor();
    private float hoverAnim;
    private float pressAnim;
    private float warpX;
    private float warpY;
    private @Nullable ContentStretch stretch;
    private boolean armed;
    private boolean flat;
    private static final float ICON_REFRACTION_THICKNESS_RATIO = 1f / 3f;

    @FunctionalInterface
    public interface PressAction {
        void onPress(WLButton button);
    }

    public WLButton(final int x, final int y, final int width, final int height, final String label, final PressAction onPress) {
        this(x, y, width, height, label, onPress, false);
    }

    public static WLButton icon(final int x, final int y, final int width, final int height, final PressAction onPress) {
        return new WLButton(x, y, width, height, "", onPress, true);
    }

    protected WLButton(final int x, final int y, final int width, final int height, final String label, final PressAction onPress, final boolean chromeGlass) {
        super(x, y, width, height);
        this.label = label == null ? "" : label;
        this.onPress = onPress;
        this.cornerRadius = 0.5f * Math.min(width, height);
        this.textSize = Mth.clamp(height * 0.5f, Type.CAPTION1.size() * this.uiScale, Type.TITLE2.size() * this.uiScale);
        this.style = (chromeGlass ? LiquidKnob.bevelKnobStyle(LiquidGlassConstants.DEFAULT_LAYER) : ResetButton.knobStyle(LiquidGlassConstants.DEFAULT_LAYER)).fadeKey(FADE_KEYS.incrementAndGet());
        this.setMessage(Component.literal(this.label));
    }

    public static void sizeIconRefraction(final GlassStyle style, final float diameter) {
        final float authored = diameter / Math.max(0.01f, WLScreen.uiScaleFactor());
        style.refractionThickness(Math.max(1f, authored * ICON_REFRACTION_THICKNESS_RATIO));
    }

    public static GlassStyle defaultStyle() {
        return SearchBar.defaultStyle().layer(LiquidGlassConstants.DEFAULT_LAYER);
    }

    public static GlassStyle panelLens(final GlassStyle style) {
        return style.refractionStrength(1.2f).refractionThickness(14f).refractionFactor(2f).refractionDispersion(4f);
    }

    public void settleInteraction() {
        this.hoverAnim = 0f;
        this.pressAnim = 0f;
        this.warpX = 0f;
        this.warpY = 0f;
        this.stretch = null;
        this.armed = false;
    }

    @Override
    protected void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float a) {
        final float dt = WLRenderSystem.frameDeltaSeconds();
        final boolean hovered = this.isHovered() && this.active;
        if (this.armed && !isLeftMouseDown()) {
            this.armed = false;
            if (hovered) {
                WLClient.schedule(this::fire);
            }
        }
        final boolean held = this.armed;
        WLRenderSystem.cursors().requestControl(this.isHovered() || held, this.active);
        this.hoverAnim = MathUtils.approach(this.hoverAnim, hovered || (held && this.active) ? 1f : 0f, HOVER_SPEED, dt);
        this.pressAnim = MathUtils.approach(this.pressAnim, held ? 1f : 0f, held ? PRESS_IN_SPEED : PRESS_OUT_SPEED, dt);
        this.warpX = MathUtils.approach(this.warpX, held ? warpTarget(mouseX, this.getX() + this.getWidth() / 2f, this.getWidth() / 2f, this.uiScale) : 0f, WARP_SPEED, dt);
        this.warpY = MathUtils.approach(this.warpY, held ? warpTarget(mouseY, this.getY() + this.getHeight() / 2f, this.getHeight() / 2f, this.uiScale) : 0f, WARP_SPEED, dt);
        final float alpha = Mth.clamp(this.getAlpha(), 0f, 1f);
        if (alpha <= 0.001f) {
            this.stretch = null;
            return;
        }
        final boolean still = this.flat || UiStyle.minecraft();
        final float grow = still ? 0f : PRESS_GROW * this.pressAnim;
        final float warpX = still ? 0f : this.warpX;
        final float warpY = still ? 0f : this.warpY;
        final float growX = this.getWidth() * grow / 2f;
        final float growY = this.getHeight() * grow / 2f;
        final float gx = this.getX() - growX;
        final float gy = this.getY() - growY;
        final float gw = Math.max(1f, this.getWidth() + growX * 2f);
        final float gh = Math.max(1f, this.getHeight() + growY * 2f);
        this.stretch = ContentStretch.of(graphics.pose(), gx, gy, gw, gh, warpX, warpY, 1f + grow);
        final GlassStyle base = (this.active ? this.style : GlassStyle.inactive(this.style)).scaled(this.uiScale);
        final GlassStyle drawStyle = alpha < 1f ? base.opacity(base.getOpacity() * alpha) : base;
        graphics.glass()
            .dimensions(gx, gy, gw, gh)
            .cornerRadius(Math.min(this.cornerRadius * (1f + grow), Math.min(gw, gh) / 2f))
            .style(drawStyle)
            .hover(this.active && !this.flat ? this.hoverAnim : 0f)
            .stretch(warpX, warpY)
            .extract();
        if (this.label.isEmpty()) {
            return;
        }
        final float size = this.textSize;
        final float textWidth = Fonts.SFPRO_DISPLAY.width(this.label, size);
        if (hovered) {
            Fonts.SFPRO_DISPLAY.warmStretched(size);
        }
        final float centerX = this.getX() + this.getWidth() / 2f;
        final float centerY = this.getY() + this.getHeight() / 2f;
        final boolean pixelated = Fonts.SFPRO_DISPLAY.pixelated(size);
        final float tx = pixelated ? Fonts.SFPRO_DISPLAY.leftForPixelCenter(centerX, this.label, size) : centerX - textWidth / 2f;
        final float ty = pixelated ? Fonts.SFPRO_DISPLAY.topForPixelCenter(centerY, size) : centerY - BASELINE_CENTER * size;
        int color = ColorUtils.lerpColor(this.textColor, this.textColorHovered, this.hoverAnim);
        if (!this.active) {
            color = ColorUtils.withAlpha(color, 0.5f);
        }
        final int labelColor = ColorUtils.withAlpha(color, alpha);
        ResetButton.lift(this.style.getLayer() > PopoverHost.PANEL_LAYER, this.style.getLayer(), () -> this.stretchContent(() -> Fonts.SFPRO_DISPLAY.extract(graphics, this.label, tx, ty, size, labelColor)));
    }

    @Override
    public void onClick(final MouseButtonEvent event, final boolean doubleClick) {
        this.armed = true;
    }

    @Override
    public void onRelease(final MouseButtonEvent event) {
        if (!this.armed) {
            return;
        }
        this.armed = false;
        if (this.active && this.isMouseOver(event.x(), event.y())) {
            this.fire();
        }
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (!this.active || !this.visible) {
            return false;
        }
        final int key = event.key();
        if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER || key == InputConstants.KEY_SPACE) {
            this.activateFromKeyboard();
            return true;
        }
        return false;
    }

    private void fire() {
        if (this.onPress != null) {
            this.onPress.onPress(this);
        }
    }

    private void activateFromKeyboard() {
        this.pressAnim = 1f;
        this.fire();
    }

    static boolean isLeftMouseDown() {
        return WalksyLib.getInstance().getMouseHandler().isLeftDown();
    }

    public WLButton setLabel(final String label) {
        this.label = label == null ? "" : label;
        this.setMessage(Component.literal(this.label));
        return this;
    }

    public WLButton setOnPress(final PressAction onPress) {
        this.onPress = onPress;
        return this;
    }

    public WLButton setCornerRadius(final float radiusPx) {
        this.cornerRadius = Math.max(0f, radiusPx);
        return this;
    }

    public float getCornerRadius() {
        return this.cornerRadius;
    }

    public WLButton setTextSize(final float size) {
        this.textSize = Math.max(DevicePixels.snapSizeY(0f), size);
        return this;
    }

    public WLButton setTextColor(final int color) {
        this.textColor = color;
        return this;
    }

    public WLButton setTextColor(final int color, final int hoveredColor) {
        this.textColor = color;
        this.textColorHovered = hoveredColor;
        return this;
    }

    public String getLabel() {
        return this.label;
    }

    public GlassStyle style() {
        return this.style;
    }

    public void stretchContent(final Runnable body) {
        ContentStretch.run(this.stretch, body);
    }

    public boolean isArmed() {
        return this.armed;
    }

    public WLButton flat() {
        this.flat = true;
        this.style = defaultStyle().layer(this.style.getLayer()).fadeKey(this.style.getFadeKey());
        this.style.refractionStrength(0f)
            .refractionThickness(0f)
            .refractionReach(0f)
            .refractionDispersion(0f)
            .refractionFactor(1f)
            .bevel(0f)
            .lensFold(0f)
            .brightEdge(0f)
            .edgeDarken(0f)
            .innerShadow(0f)
            .glareFactor(0f)
            .glareRange(0f)
            .fresnelFactor(0f)
            .fresnelRange(0f)
            .rimLight(Theme.get().rimLightColor(), 0f);
        return this;
    }

    public static boolean release(@Nullable final WLButton button, final MouseButtonEvent event) {
        if (button == null || !button.isArmed()) {
            return false;
        }
        button.mouseReleased(event);
        return true;
    }

    static float warpTarget(final double cursor, final float center, final float half, final float uiScale) {
        final float delta = (float) cursor - center;
        final float outside = Math.abs(delta) - half;
        if (outside <= 0f) {
            return 0f;
        }
        final float t = outside / (WARP_REACH * uiScale);
        return Math.signum(delta) * WARP_MAX * uiScale * (t / (1f + t));
    }
}
