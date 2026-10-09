package main.walksy.lib.core.gui.widget.options.category;
import com.mojang.blaze3d.platform.InputConstants;

import main.walksy.lib.core.gui.style.UiStyle;
import main.walksy.lib.core.render.TextQueueMark;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.api.config.option.Category;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.modal.PopoverHost;
import main.walksy.lib.core.gui.widget.WLButton;
import main.walksy.lib.core.gui.widget.options.control.LiquidKnob;
import main.walksy.lib.core.gui.widget.WLWidget;
import main.walksy.lib.core.render.Cursors;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.render.glass.api.LiquidGlassConstants;
import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.utils.MathUtils;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CategoryTabBar extends WLWidget {
    static final float SMALL_SURFACE_INNER_SHADOW = 3f;
    public static final int BAR_LAYER = 19;
    private static final int HIGHLIGHT_LAYER = 20;
    private static final float TEXT_SIZE = Type.SUBHEADLINE.size();
    private static final float SEGMENT_PADDING_X = 14f;
    private static final float OUTER_PADDING_X = 6f;
    private static final float HIGHLIGHT_INSET = 3f;
    private static final float GLASS_INSET = -6.5f;
    private static final float CLICK_GLOW_RISE_SPEED = 50f;
    private static final float CLICK_GLOW_DECAY_SPEED = 10f;
    private static final float TAP_SIZE_AMOUNT = 0.35f;
    private static final float HOLD_GROW_TIME = 0.25f;
    private static final float COMPLETE_THRESHOLD = 0.99f;
    private static final float BAR_HOVER_SPEED = 14f;
    private static final float BAR_HOVER_POP = 0f;
    private static final float WIDTH_LERP_SPEED = 10f;
    private static final float MOULD_BULGE = 0f;
    private static final float MOULD_SMOOTHING = 0.006f;

    private static final float FOLLOW_STIFFNESS = 170f;
    private static final float FOLLOW_DAMPING = 26.1f;
    private static final float EDGE_OVERFLOW_ALLOWANCE = 4f;
    private static final float VELOCITY_SAMPLE_WINDOW = 0.03f;
    private static final float VELOCITY_SMOOTH_SPEED = 7f;
    private static final float STRETCH_STIFFNESS = 150f;
    private static final float STRETCH_DAMPING = 9f;
    private static final float STRETCH_PER_VELOCITY = 0.035f;
    private static final float MAX_STRETCH = 9f;
    private static final float STRETCH_OVERSHOOT_CAP = MAX_STRETCH * 1.5f;
    private static final float MIN_HEIGHT_SCALE = 0.8f;

    private static final float BAR_LENS_REACH = 0.225f;

    private static final float BLOB_TEXT_SCALE = 0.85f;
    private static final float TEXT_SCALE_DEAD_ZONE = 0.01f;

    private static final float TEXT_SIZE_DECAY_SPEED = 12f;

    private static final float CAP_HEIGHT_RATIO = 0.59043f;
    private static final float EDGE_BOUNCE_KICK_PER_VELOCITY = 0.25f;
    private static final float EDGE_BOUNCE_MIN_SPEED = 250f;
    private static final float STOP_VELOCITY_THRESHOLD = 550f;
    private static final float STOP_DECEL_RATIO = 0.2f;
    private static final float STOP_BOUNCE_KICK = 0.025f;
    private final List<Category> categories;
    private final Consumer<Category> onSelect;
    private final Supplier<Category> selectedSupplier;
    private final float uiScale;
    private final GlassStyle barStyle;
    private final GlassStyle mouldStyle;
    private final GlassStyle restStyle;
    private final GlassStyle hoverStyle;
    private final float[] segmentX;
    private final float[] segmentW;
    private Spring1D followerX;
    private Spring1D stretchSpring;
    private boolean pressed;
    private float prevMouseX;
    private boolean mouseTracked;
    private float velocityWindowTime;
    private float velocityWindowDelta;
    private float sampleVelocity;
    private float velocitySmoothed;
    private float prevEdgeOverflow;
    private float clickGlow;
    private float materialAmount;
    private boolean completing;
    private float earnedSize;
    private float pressHoldTime;
    private float sizeAmount;
    private float smoothedBaseW;
    private float barHoverAnim;
    private float textSizeAmount;
    private int prevSelected = Integer.MIN_VALUE;

    private final PopoverHost activePopover;

    private static int LABEL_COLOR() {
        return Theme.get().label();
    }

    private static float BLOB_OUTLINE_WIDTH() {
        return Theme.get().outlineWidth() * 0.5f;
    }

    private static final class Spring1D {
        private static final float MAX_STEP = 1f / 240f;
        float value;
        float velocity;

        Spring1D(final float initial) {
            this.value = initial;
        }

        void step(final float target, final float stiffness, final float damping, final float dt) {
            if (UiStyle.minecraft()) {
                this.value = target;
                this.velocity = 0f;
                return;
            }
            if (dt <= 0f) {
                return;
            }
            final int steps = (int) Math.ceil(dt / MAX_STEP);
            final float h = dt / steps;
            for (int i = 0; i < steps; i++) {
                final float accel = (target - this.value) * stiffness - this.velocity * damping;
                this.velocity += accel * h;
                this.value += this.velocity * h;
            }
        }
}

    public CategoryTabBar(final List<Category> categories, final Consumer<Category> onSelect, final Supplier<Category> selectedSupplier, final PopoverHost activePopover, final int x, final int y, final int height) {
        super(x, y, 1, height);
        this.categories = categories;
        this.onSelect = onSelect;
        this.selectedSupplier = selectedSupplier;
        this.activePopover = activePopover;
        this.uiScale = WLScreen.uiScaleFactor();
        this.barStyle = LiquidKnob.bevelKnobStyle(BAR_LAYER)
            .refractionReach(BAR_LENS_REACH)
            .refractionDispersion(0f)
            .softLens(true)
            .bakesText(false);
        this.mouldStyle = this.barStyle.copy().smoothing(MOULD_SMOOTHING);
        this.restStyle = new GlassStyle()
            .blurRadius(LiquidGlassConstants.BLUR_CLEAR)
            .refractionStrength(0f)
            .refractionThickness(0f)
            .refractionFactor(1f)
            .refractionDispersion(0f)
            .shadow(0f, 0f, 0f, 0f)
            .shadowColor(Theme.get().shadowColor(), 0f)
            .bevel(0f)
            .lensFold(0f)
            .layer(HIGHLIGHT_LAYER)
            .tint(Theme.get().pillTint(), Theme.get().pillTintAlpha())
            .opacity(1f)
            .refractionStrength(0f)
            .fresnelRange(0f)
            .fresnelHardness(0f)
            .fresnelFactor(0f)
            .glareRange(0f)
            .glareHardness(0f)
            .glareFactor(0f)
            .brightEdge(0f)
            .edgeDarken(Theme.get().edgeDarkenColor(), 0f)
            .rimLight(Theme.get().rimLightColor(), 0f)
            .innerShadow(0f)
            .outline(BLOB_OUTLINE_WIDTH(), Theme.get().shadowColor(), 0f);
        this.hoverStyle = WLButton.defaultStyle()
            .layer(HIGHLIGHT_LAYER)
            .blurRadius(LiquidGlassConstants.BLUR_CLEAR)
            .opacity(1f)
            .tint(Theme.get().controlTint(), 0f)
            .bevel(5.6f)
            .refractionStrength(1f)
            .refractionThickness(9f)
            .refractionFactor(2.1f)
            .refractionDispersion(0f)
            .fresnelFactor(0f)
            .fresnelHardness(26f)
            .fresnelRange(0f)
            .glareRange(0f)
            .glareHardness(0f)
            .brightEdge(LiquidGlassConstants.CHROME_BRIGHT_EDGE)
            .rimLight(Theme.get().rimLightColor(), 0f)
            .edgeDarken(Theme.get().edgeDarkenColor(), 0.40f)
            .innerShadow(Theme.get().innerShadow() * SMALL_SURFACE_INNER_SHADOW)
            .refractionReach(0f)
            .refractionShift(2f)
            .textScale(BLOB_TEXT_SCALE)
            .outlineWithShadow(BLOB_OUTLINE_WIDTH(), Theme.get().shadowColor(), Theme.get().outlineAlpha(), Theme.get().shadowAlpha() * 0.6f * Theme.get().shadowFactor(), Theme.get().shadowExpand(), Theme.get().shadowOffsetY());
        this.segmentX = new float[categories.size()];
        this.segmentW = new float[categories.size()];
        this.layoutSegments();
    }

    public boolean matches(final List<Category> categories, final int x, final int y, final int height) {
        return this.categories.equals(categories) && this.getX() == x && this.getY() == y && this.getHeight() == height && this.uiScale == WLScreen.uiScaleFactor();
    }

    public static float measureWidth(final List<Category> categories) {
        final float uiScale = WLScreen.uiScaleFactor();
        final float size = TEXT_SIZE * uiScale;
        float total = OUTER_PADDING_X * uiScale * 2f;
        for (final Category category : categories) {
            total += Fonts.SFPRO_DISPLAY_MEDIUM.width(category.name(), size) + SEGMENT_PADDING_X * 2f * uiScale;
        }
        return total;
    }

    private void layoutSegments() {
        final float size = TEXT_SIZE * this.uiScale;
        float cursor = this.getX() + OUTER_PADDING_X * this.uiScale;
        for (int i = 0; i < this.categories.size(); i++) {
            final float textWidth = Fonts.SFPRO_DISPLAY_MEDIUM.width(this.categories.get(i).name(), size);
            final float segW = textWidth + SEGMENT_PADDING_X * 2f * this.uiScale;
            this.segmentX[i] = cursor;
            this.segmentW[i] = segW;
            cursor += segW;
        }
        cursor += OUTER_PADDING_X * this.uiScale;
        this.setWidth(Math.round(cursor - this.getX()));
    }

    private int selectedIndex() {
        final Category selected = this.selectedSupplier == null ? null : this.selectedSupplier.get();
        return this.categories.indexOf(selected);
    }

    private int segmentAt(final double mouseX, final double mouseY) {
        if (!this.isMouseOver(mouseX, mouseY)) {
            return -1;
        }
        for (int i = 0; i < this.segmentX.length; i++) {
            if (mouseX >= this.segmentX[i] && mouseX < this.segmentX[i] + this.segmentW[i]) {
                return i;
            }
        }
        return -1;
    }

    private int nearestSegmentIndex(final double mouseX) {
        int best = 0;
        float bestDist = Float.MAX_VALUE;
        for (int i = 0; i < this.segmentX.length; i++) {
            final float center = this.segmentX[i] + this.segmentW[i] / 2f;
            final float dist = Math.abs((float) mouseX - center);
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        return best;
    }

    @Override
    protected void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float a) {
        float dt = WLRenderSystem.frameDeltaSeconds();
        if (!Float.isFinite(dt) || dt < 0f) {
            dt = 0f;
        }
        final float alpha = Math.max(0f, Math.min(1f, this.getAlpha()));
        if (alpha <= 0.001f) {
            return;
        }
        final boolean blockedByPopover = this.activePopover != null && this.activePopover.containsPoint(mouseX, mouseY);
        WLRenderSystem.cursors().requestIf(!blockedByPopover && this.segmentAt(mouseX, mouseY) >= 0, Cursors.Cursor.POINTER);
        final boolean overBar = this.pressed || (!blockedByPopover && this.isMouseOver(mouseX, mouseY));
        this.barHoverAnim = MathUtils.approach(this.barHoverAnim, overBar ? 1f : 0f, BAR_HOVER_SPEED, dt);
        final float barPop = this.barHoverAnim * BAR_HOVER_POP * this.uiScale;
        graphics.glass()
            .dimensions(this.getX() - barPop, this.getY() - barPop, this.getWidth() + barPop * 2f, this.getHeight() + barPop * 2f)
            .cornerRadius(this.getHeight() / 2f + barPop)
            .style(this.barStyle.scaled(this.uiScale).refractionThickness(GlassStyle.centreLensBand(graphics.pose(), this.getHeight() + barPop * 2f, LiquidGlassConstants.HOVER_SCALE_PX * this.barHoverAnim)).opacity(alpha))
            .hover(this.barHoverAnim)
            .extract();
        final float mouseDelta = this.mouseTracked ? mouseX - this.prevMouseX : 0f;
        this.prevMouseX = mouseX;
        this.mouseTracked = true;
        this.velocityWindowTime += dt;
        this.velocityWindowDelta += mouseDelta;
        float stopKick = 0f;
        if (this.velocityWindowTime >= VELOCITY_SAMPLE_WINDOW) {
            final float newSample = this.velocityWindowDelta / this.velocityWindowTime;
            final boolean suddenStop = this.pressed && Math.abs(this.sampleVelocity) > STOP_VELOCITY_THRESHOLD * this.uiScale && Math.abs(newSample) < Math.abs(this.sampleVelocity) * STOP_DECEL_RATIO;
            if (suddenStop) {
                stopKick = Math.abs(this.sampleVelocity) * STOP_BOUNCE_KICK;
            }
            this.sampleVelocity = newSample;
            this.velocityWindowTime = 0f;
            this.velocityWindowDelta = 0f;
        }
        this.velocitySmoothed = MathUtils.approach(this.velocitySmoothed, this.pressed ? this.sampleVelocity : 0f, VELOCITY_SMOOTH_SPEED, dt);
        final int selected = this.selectedIndex();
        this.prevSelected = selected;
        final int target = this.pressed ? this.nearestSegmentIndex(mouseX) : selected;
        if (target >= 0) {
            final float segCenter = this.segmentX[target] + this.segmentW[target] / 2f;
            final float desiredCenter = this.pressed ? Math.max(this.getX(), Math.min(this.getX() + this.getWidth(), (float) mouseX)) : segCenter;
            if (this.followerX == null) {
                this.followerX = new Spring1D(desiredCenter);
                this.stretchSpring = new Spring1D(0f);
                this.smoothedBaseW = this.segmentW[target];
            }
            this.followerX.step(desiredCenter, FOLLOW_STIFFNESS, FOLLOW_DAMPING, dt);
            if (this.pressed) {
                this.completing = true;
            }
            final float glassTarget = this.pressed || this.completing ? 1f : 0f;
            final float glowSpeed = glassTarget > this.clickGlow ? CLICK_GLOW_RISE_SPEED : CLICK_GLOW_DECAY_SPEED;
            this.clickGlow = MathUtils.approach(this.clickGlow, glassTarget, glowSpeed, dt);
            this.materialAmount = this.clickGlow;
            this.textSizeAmount = this.pressed ? this.materialAmount : MathUtils.approach(this.textSizeAmount, 0f, TEXT_SIZE_DECAY_SPEED, dt);
            this.pressHoldTime = this.pressed ? this.pressHoldTime + dt : 0f;
            if (this.pressed) {
                final float holdT = Math.min(1f, this.pressHoldTime / HOLD_GROW_TIME);
                this.earnedSize = TAP_SIZE_AMOUNT + (1f - TAP_SIZE_AMOUNT) * holdT;
            }
            final float sizeTarget = this.pressed || this.completing ? this.earnedSize : 0f;
            final float sizeSpeed = sizeTarget > this.sizeAmount ? CLICK_GLOW_RISE_SPEED : CLICK_GLOW_DECAY_SPEED;
            this.sizeAmount = MathUtils.approach(this.sizeAmount, sizeTarget, sizeSpeed, dt);
            if (this.completing && !this.pressed && this.clickGlow >= COMPLETE_THRESHOLD && this.sizeAmount >= this.earnedSize * COMPLETE_THRESHOLD) {
                this.completing = false;
            }
            this.smoothedBaseW = MathUtils.approach(this.smoothedBaseW, this.segmentW[target], WIDTH_LERP_SPEED, dt);
            if (stopKick > 0f) {
                this.stretchSpring.velocity += stopKick;
            }
            final float targetStretch = Math.min(MAX_STRETCH * this.uiScale, Math.abs(this.followerX.velocity) * STRETCH_PER_VELOCITY);
            this.stretchSpring.step(targetStretch, STRETCH_STIFFNESS, STRETCH_DAMPING, dt);
            if (!Float.isFinite(this.stretchSpring.value)) {
                this.stretchSpring.value = 0f;
                this.stretchSpring.velocity = 0f;
            } else {
                this.stretchSpring.value = Math.min(this.stretchSpring.value, STRETCH_OVERSHOOT_CAP * this.uiScale);
            }
            final float inset = (HIGHLIGHT_INSET + (GLASS_INSET - HIGHLIGHT_INSET) * this.sizeAmount) * this.uiScale;
            final float baseW = this.smoothedBaseW - inset * 2f;
            final float baseH = this.getHeight() - inset * 2f;
            final float stretch = Math.max(0f, this.stretchSpring.value);
            final float w = baseW + stretch;
            final float heightScale = Math.max(MIN_HEIGHT_SCALE, baseW / w);
            final float h = baseH * heightScale;
            final float cx = this.followerX.value;
            final float cy = this.getY() + this.getHeight() / 2f;
            float x0 = cx - w / 2f;
            final float y0 = cy - h / 2f;
            final float overflow = EDGE_OVERFLOW_ALLOWANCE * this.uiScale;
            final float barLeft = this.getX() - overflow;
            final float barRight = this.getX() + this.getWidth() + overflow;
            final float edgeOverflowAmt = Math.max(0f, barLeft - x0) + Math.max(0f, x0 - (barRight - w));
            if (edgeOverflowAmt > 0.01f && this.prevEdgeOverflow <= 0.01f) {
                final float excessSpeed = Math.abs(this.velocitySmoothed) - EDGE_BOUNCE_MIN_SPEED * this.uiScale;
                if (excessSpeed > 0f) {
                    this.stretchSpring.velocity += excessSpeed * EDGE_BOUNCE_KICK_PER_VELOCITY;
                }
            }
            this.prevEdgeOverflow = edgeOverflowAmt;
            x0 = Math.max(barLeft, Math.min(x0, barRight - w));
            GlassStyle blended = GlassStyle.lerp(this.restStyle, this.hoverStyle, this.materialAmount);
            final float reachGateStart = 0.85f;
            final float reachEase = this.materialAmount <= reachGateStart ? 0f : (this.materialAmount - reachGateStart) / (1f - reachGateStart);
            blended.refractionReach(this.hoverStyle.getRefractionReach() * reachEase);
            blended.refractionShift(this.hoverStyle.getRefractionShift() * reachEase);
            final float lensEase = this.sizeAmount * this.sizeAmount * this.sizeAmount;
            blended.refractionStrength(this.hoverStyle.getRefStrength() * lensEase);
            blended.refractionFactor(1f + (this.hoverStyle.getRefFactor() - 1f) * lensEase);
            blended.refractionDispersion(this.hoverStyle.getRefDispersion() * lensEase);
            final float textEase = this.textSizeAmount <= TEXT_SCALE_DEAD_ZONE ? 0f : this.textSizeAmount;
            blended.textRefract(this.pressed ? 1f : 0f);
            blended.textScale(1f + (this.hoverStyle.getTextScale() - 1f) * textEase);
            blended = blended.scaled(this.uiScale).opacity(blended.getOpacity() * alpha);
            blended.refractionThickness(this.hoverStyle.getRefThickness() * this.uiScale * lensEase);
            blended.bevel(this.hoverStyle.getBevelHeight() * this.uiScale * reachEase);
            final float bulge = this.mouldBulgePx();
            if (bulge > 0.01f) {
                final float mh = this.getHeight() + bulge * 2f;
                graphics.glass()
                    .dimensions(x0, cy - mh / 2f, w, mh)
                    .cornerRadius(mh / 2f)
                    .style(this.mouldStyle.scaled(this.uiScale).opacity(alpha))
                    .hover(this.barHoverAnim)
                    .extract();
            }
            graphics.glass().dimensions(x0, y0, w, h).cornerRadius(h / 2f).style(blended).extract();
        }
    }

    public void extractEarlyText(final WLGraphics2D graphics) {
        final float alpha = Math.max(0f, Math.min(1f, this.getAlpha()));
        if (alpha <= 0.001f) {
            return;
        }
        final TextQueueMark mark = WLRenderSystem.textMark();
        this.extractLabels(graphics, alpha);
        WLRenderSystem.markEarlyText(mark);
    }

    private void extractLabels(final WLGraphics2D graphics, final float alpha) {
        final float size = TEXT_SIZE * this.uiScale;
        final float capHeight = size * CAP_HEIGHT_RATIO;
        final float centerY = this.getY() + this.getHeight() / 2f;
        final float top = Fonts.SFPRO_DISPLAY_MEDIUM.topForBaseline(centerY + capHeight * 0.5f, size);
        for (int i = 0; i < this.categories.size(); i++) {
            final String name = this.categories.get(i).name();
            final float textWidth = Fonts.SFPRO_DISPLAY_MEDIUM.width(name, size);
            final float textX = this.segmentX[i] + (this.segmentW[i] - textWidth) / 2f;
            Fonts.SFPRO_DISPLAY_MEDIUM.extract(graphics, name, textX, top, size, ColorUtils.withAlpha(LABEL_COLOR(), alpha));
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !this.isMouseOver(event.x(), event.y())) {
            return false;
        }
        if (UiStyle.minecraft()) {
            if (this.onSelect != null) {
                this.onSelect.accept(this.categories.get(this.nearestSegmentIndex(event.x())));
            }
            return true;
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
        final int landed = this.nearestSegmentIndex(event.x());
        if (this.onSelect != null) {
            this.onSelect.accept(this.categories.get(landed));
        }
        return true;
    }

    private float mouldBulgePx() {
        return MOULD_BULGE * this.uiScale * this.sizeAmount;
    }
}
