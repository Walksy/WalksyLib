package main.walksy.lib.core.gui.liquid;

import main.walksy.lib.core.gui.style.UiStyle;
import main.walksy.lib.core.gui.animation.ScreenAnimation;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public final class LiquidTransition {
    private static final float CLOSE_SPEED = 1.5f;
    private static final float TWO_PI = (float) (Math.PI * 2.0);
    private static long NEXT_ID = 1L;
    private final long id = NEXT_ID++;
    private final LiquidProfile profile;
    private LiquidRect from;
    private LiquidRect to;
    private float progress;
    private float target;
    private float openSpeed;
    private float speed;
    private boolean skipNextUpdate;
    private boolean reshaping;

    public LiquidTransition(final LiquidProfile profile, final float openSpeed) {
        this.profile = profile;
        this.openSpeed = openSpeed;
        this.speed = openSpeed;
    }

    public LiquidProfile profile() {
        return this.profile;
    }

    public void reset() {
        this.progress = 0f;
        this.target = 0f;
        this.speed = this.openSpeed;
        this.skipNextUpdate = true;
        this.reshaping = false;
    }

    public LiquidTransition open(final LiquidRect panel) {
        this.to = panel;
        if (this.from == null) {
            this.from = LiquidRect.seed(panel, this.profile.seedSize);
        }
        this.target = 1f;
        this.speed = this.openSpeed;
        return this;
    }

    public void close() {
        this.target = 0f;
        this.speed = CLOSE_SPEED;
        this.reshaping = false;
    }

    public void closeSymmetric() {
        this.target = 0f;
        this.speed = this.openSpeed;
        this.reshaping = false;
    }

    public void reshape(final LiquidRect from, final LiquidRect to) {
        this.from = from;
        this.to = to;
        this.progress = 0f;
        this.target = 1f;
        this.speed = this.openSpeed;
        this.reshaping = true;
    }

    public LiquidTransition setEndpoints(final LiquidRect from, final LiquidRect to) {
        this.from = from;
        this.to = to;
        return this;
    }

    public void expand() {
        this.target = 1f;
        this.speed = this.openSpeed;
    }

    public void collapse() {
        this.close();
    }

    public void morphTo(final LiquidRect next) {
        this.from = this.currentPrimary();
        this.to = next;
        this.progress = 0f;
        this.target = 1f;
        this.speed = this.openSpeed;
    }

    public void retarget(final LiquidRect to) {
        this.to = to;
    }

    public boolean isClosing() {
        return this.target < 0.5f;
    }

    public boolean isDoneClosing() {
        return this.isClosing() && this.progress < 0.01f;
    }

    public boolean isSettled() {
        return Math.abs(this.progress - this.target) < 0.001f;
    }

    public float value() {
        if (this.reshaping) {
            return 1f;
        }
        return ScreenAnimation.ease(this.profile.easing, Mth.clamp(this.progress, 0f, 1f));
    }

    public void update(final float delta) {
        if (UiStyle.minecraft()) {
            this.skipNextUpdate = false;
            this.progress = this.target;
            return;
        }
        if (this.skipNextUpdate) {
            this.skipNextUpdate = false;
            return;
        }
        final float t = 1.0f - (float) Math.exp(-this.speed * delta);
        this.progress = this.progress + (this.target - this.progress) * t;
    }

    public void setProgress(final float p) {
        this.progress = Mth.clamp(p, 0f, 1f);
    }

    public LiquidRect currentPrimary() {
        if (this.to == null) {
            return this.from;
        }
        if (this.from == null) {
            return this.to;
        }
        final float t = ScreenAnimation.ease(this.profile.easing, Mth.clamp(this.progress, 0f, 1f));
        final LiquidRect r = LiquidRect.lerp(this.from, this.to, t);
        return new LiquidRect(r.x(), r.y(), r.w(), r.h(), r.clampedRadius());
    }

    public LiquidShape compute() {
        if (this.to == null || this.from == null) {
            return new LiquidShape(List.of(), this.profile.restSmoothing);
        }
        final float raw = Mth.clamp(this.progress, 0f, 1f);
        final float t = ScreenAnimation.ease(this.profile.easing, raw);
        final float melt = (float) Math.sin(raw * Math.PI);
        final LiquidRect base = LiquidRect.lerp(this.from, this.to, t);
        float squash = 0f;
        if (this.profile.wobble != 0f && this.profile.wobbleCycles > 0) {
            squash = this.profile.wobble * (float) Math.sin(raw * TWO_PI * this.profile.wobbleCycles) * (1f - raw);
        }
        final float pcx = base.centerX();
        final float pcy = base.centerY();
        final float pw = Math.max(1f, base.w() * (1f + squash));
        final float ph = Math.max(1f, base.h() * (1f - squash));
        final List<LiquidRect> blobs = new ArrayList<>(3 + this.profile.droplets);
        float radius = Math.min(base.cornerRadius(), 0.5f * Math.min(pw, ph));
        if (this.profile.roundnessSwell > 0f) {
            final float floor = this.profile.roundnessSwell * (1f - t) * 0.5f * Math.min(pw, ph);
            radius = Math.max(radius, Math.min(floor, 0.5f * Math.min(pw, ph)));
        }
        final LiquidRect primary = new LiquidRect(pcx - 0.5f * pw, pcy - 0.5f * ph, pw, ph, radius);
        blobs.add(primary);
        if (UiStyle.minecraft()) {
            return new LiquidShape(blobs, 0f);
        }
        if (this.profile.anchorResidue > 0f) {
            float s = 1f - Mth.clamp(raw / this.profile.anchorResidue, 0f, 1f);
            s = s * s * (3f - 2f * s);
            final float rw = this.from.w() * s;
            final float rh = this.from.h() * s;
            if (rw > 1.5f && rh > 1.5f) {
                blobs.add(new LiquidRect(this.from.centerX() - 0.5f * rw, this.from.centerY() - 0.5f * rh, rw, rh, Math.min(this.from.clampedRadius(), 0.5f * Math.min(rw, rh))));
            }
        }
        if (this.profile.bridge > 0f && melt > 0.001f) {
            final float bx = Mth.lerp(t, this.from.centerX(), this.to.centerX());
            final float by = Mth.lerp(t, this.from.centerY(), this.to.centerY());
            final float bs = this.profile.bridge * Math.min(pw, ph) * melt;
            if (bs > 1.5f) {
                blobs.add(LiquidRect.circle(bx, by, bs));
            }
        }
        if (this.profile.droplets > 0 && melt > 0.001f) {
            final float targetMin = Math.min(this.to.w(), this.to.h());
            final float spread = this.profile.dropletSpread * targetMin * (1f - raw);
            final float dsize = this.profile.dropletSize * targetMin * melt;
            if (dsize > 1.5f) {
                for (int i = 0; i < this.profile.droplets; i++) {
                    final float ang = (i * TWO_PI / this.profile.droplets) + raw * 1.2f;
                    final float dx = pcx + (float) Math.cos(ang) * spread;
                    final float dy = pcy + (float) Math.sin(ang) * spread;
                    blobs.add(LiquidRect.circle(dx, dy, dsize));
                }
            }
        }
        final float k = Mth.lerp(melt, this.profile.restSmoothing, this.profile.meltSmoothing);
        return new LiquidShape(blobs, k);
    }

    public void emit(final WLGraphics2D ctx, final GlassStyle baseStyle, final int layer) {
        this.emit(ctx, baseStyle, layer, -1f);
    }

    public void emit(final WLGraphics2D ctx, final GlassStyle baseStyle, final int layer, final float minecraftAmount) {
        final LiquidShape shape = this.compute();
        final List<LiquidRect> blobs = shape.blobs();
        for (int i = 0; i < blobs.size(); i++) {
            final LiquidRect b = blobs.get(i);
            if (b.w() < 0.5f || b.h() < 0.5f) {
                continue;
            }
            final GlassStyle s = baseStyle.copy().layer(layer).smoothing(shape.smoothing()).fadeKey(this.id * 64L + i);
            ctx.glass().dimensions(b.x(), b.y(), b.w(), b.h()).cornerRadius(b.clampedRadius()).style(s).minecraftAmount(minecraftAmount).extract();
        }
    }
}
