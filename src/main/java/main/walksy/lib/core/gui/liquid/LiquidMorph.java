package main.walksy.lib.core.gui.liquid;

import java.util.ArrayList;
import java.util.List;

import main.walksy.lib.core.gui.style.UiStyle;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import net.minecraft.util.Mth;

public final class LiquidMorph {
    private static long NEXT_ID = 1L;
    private static final float MAX_STEP = 0.25f;
    private static final float SWELL_TICKS = 1.2f;
    private static final float SWELL_FACTOR = 2.0f;
    private static final float SWELL_BIAS = 0.25f;
    private static final float SWELL_OMEGA = 1.3f;
    private static final float SWELL_ZETA = 0.95f;
    private static final float FLIGHT_OMEGA_X = 1.0f;
    private static final float FLIGHT_ZETA_X = 0.78f;
    private static final float FLIGHT_OMEGA_Y = 0.86f;
    private static final float FLIGHT_ZETA_Y = 0.68f;
    private static final float DAMPED_OMEGA = 0.95f;
    private static final float DAMPED_ZETA = 1.0f;
    private static final float UNFOLD_LEAD_OMEGA = 0.95f;
    private static final float UNFOLD_LEAD_ZETA = 0.62f;
    private static final float COLLAPSE_OMEGA_X = 1.05f;
    private static final float COLLAPSE_ZETA_X = 0.74f;
    private static final float COLLAPSE_OMEGA_Y = 0.92f;
    private static final float COLLAPSE_ZETA_Y = 0.66f;
    private static final float COLLAPSE_TICKS = 2.4f;
    private static final float CONTRACT_OMEGA = 1.0f;
    private static final float CONTRACT_ZETA = 0.95f;
    private static final float RADIUS_OMEGA = 0.9f;
    private static final float ROUND_LAND_SPAN = 0.30f;
    private static final float REST_SMOOTHING = 0.003f;
    private static final float MELT_GAIN = 0.040f;
    private static final float MELT_CAP = 0.045f;
    private final long id = NEXT_ID++;
    private final Spring x0 = new Spring();
    private final Spring y0 = new Spring();
    private final Spring x1 = new Spring();
    private final Spring y1 = new Spring();
    private final Spring radius = new Spring();
    private LiquidRect anchor;
    private LiquidRect panel;
    private boolean closing;
    private boolean swelling;
    private float swellClock;
    private boolean springy;
    private boolean contracting;
    private float collapseClock;
    private float roundness;
    private MorphProfile profile = MorphProfile.apple();
    private boolean bridged = true;

    private static final class Spring {
        float value;
        float velocity;
        float target;
        float omega;
        float zeta;

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

        boolean settled(final float eps) {
            return Math.abs(this.target - this.value) < eps && Math.abs(this.velocity) < eps;
        }

        void snap() {
            this.value = this.target;
            this.velocity = 0f;
        }
}

    public LiquidMorph profile(final MorphProfile v) {
        this.profile = v == null ? MorphProfile.apple() : v;
        return this;
    }

    public MorphProfile profile() {
        return this.profile;
    }

    public LiquidMorph bridged(final boolean v) {
        this.bridged = v;
        return this;
    }

    public void open(final LiquidRect anchor, final LiquidRect panel, final boolean springy) {
        this.anchor = anchor;
        this.panel = panel;
        this.closing = false;
        this.contracting = false;
        this.roundness = 1f;
        this.x0.reset(anchor.x());
        this.y0.reset(anchor.y());
        this.x1.reset(anchor.x() + anchor.w());
        this.y1.reset(anchor.y() + anchor.h());
        this.radius.reset(anchor.clampedRadius());
        this.radius.configure(RADIUS_OMEGA, 1f);
        this.springy = springy && !UiStyle.minecraft();
        if (this.springy) {
            this.swelling = true;
            this.swellClock = 0f;
            this.configureAll(SWELL_OMEGA, SWELL_ZETA, SWELL_OMEGA, SWELL_ZETA);
            this.targetRect(this.swollenAnchor());
        } else {
            this.swelling = false;
            this.configureUnfold();
            this.targetRect(this.panel);
        }
    }

    private void release() {
        this.swelling = false;
        this.configureAll(FLIGHT_OMEGA_X, FLIGHT_ZETA_X, FLIGHT_OMEGA_Y, FLIGHT_ZETA_Y);
        this.targetRect(this.panel);
    }

    public void close() {
        this.closing = true;
        this.swelling = false;
        final float k = this.profile.closeSpeed;
        this.radius.configure(RADIUS_OMEGA * k, 1f);
        if (!this.springy) {
            this.contracting = true;
            this.configureAll(DAMPED_OMEGA * k, DAMPED_ZETA, DAMPED_OMEGA * k, DAMPED_ZETA);
            final Spring lead = this.leadingEdgeIsBottom() ? this.y1 : this.y0;
            lead.configure(UNFOLD_LEAD_OMEGA * k, DAMPED_ZETA);
            this.targetRect(this.anchorRect());
            return;
        }
        final LiquidRect swollen = this.swollenAnchor();
        final LiquidRect cur = this.current();
        if (cur.w() + cur.h() <= swollen.w() + swollen.h()) {
            this.contract();
            return;
        }
        this.contracting = false;
        this.collapseClock = 0f;
        this.configureAll(COLLAPSE_OMEGA_X * k, COLLAPSE_ZETA_X, COLLAPSE_OMEGA_Y * k, COLLAPSE_ZETA_Y);
        this.targetRect(swollen);
    }

    private void contract() {
        this.contracting = true;
        final float k = this.profile.closeSpeed;
        this.configureAll(CONTRACT_OMEGA * k, CONTRACT_ZETA, CONTRACT_OMEGA * k, CONTRACT_ZETA);
        this.targetRect(this.anchorRect());
    }

    private LiquidRect swollenAnchor() {
        final float cx = Mth.lerp(SWELL_BIAS, this.anchor.centerX(), this.panel.centerX());
        final float cy = Mth.lerp(SWELL_BIAS, this.anchor.centerY(), this.panel.centerY());
        final float hw = 0.5f * this.anchor.w() * SWELL_FACTOR;
        final float hh = 0.5f * this.anchor.h() * SWELL_FACTOR;
        return new LiquidRect(cx - hw, cy - hh, 2f * hw, 2f * hh, Math.min(hw, hh));
    }

    private LiquidRect anchorRect() {
        return new LiquidRect(this.anchor.x(), this.anchor.y(), this.anchor.w(), this.anchor.h(), this.anchor.clampedRadius());
    }

    private void configureUnfold() {
        this.configureAll(DAMPED_OMEGA, DAMPED_ZETA, DAMPED_OMEGA, DAMPED_ZETA);
        final Spring lead = this.leadingEdgeIsBottom() ? this.y1 : this.y0;
        lead.configure(UNFOLD_LEAD_OMEGA, UNFOLD_LEAD_ZETA);
    }

    private boolean leadingEdgeIsBottom() {
        final float topTravel = Math.abs(this.panel.y() - this.anchor.y());
        final float bottomTravel = Math.abs((this.panel.y() + this.panel.h()) - (this.anchor.y() + this.anchor.h()));
        return bottomTravel >= topTravel;
    }

    private void configureAll(final float omegaX, final float zetaX, final float omegaY, final float zetaY) {
        this.x0.configure(omegaX, zetaX);
        this.x1.configure(omegaX, zetaX);
        this.y0.configure(omegaY, zetaY);
        this.y1.configure(omegaY, zetaY);
    }

    private void targetRect(final LiquidRect r) {
        this.x0.target = r.x();
        this.y0.target = r.y();
        this.x1.target = r.x() + r.w();
        this.y1.target = r.y() + r.h();
        this.radius.target = r.cornerRadius();
    }

    public LiquidMorph followAnchor(final LiquidRect anchor) {
        if (anchor == null || anchor.equals(this.anchor)) {
            return this;
        }
        this.anchor = anchor;
        if (this.closing) {
            this.targetRect(this.contracting ? this.anchorRect() : this.swollenAnchor());
        }
        return this;
    }

    public void retarget(final LiquidRect next) {
        if (this.closing) {
            return;
        }
        this.panel = next;
        if (!this.swelling) {
            this.targetRect(this.panel);
        }
    }

    public void update(final float delta) {
        if (UiStyle.minecraft()) {
            this.swelling = false;
            this.roundness = 0f;
            if (this.closing && !this.contracting) {
                this.contract();
            }
            this.x0.snap();
            this.y0.snap();
            this.x1.snap();
            this.y1.snap();
            this.radius.snap();
            return;
        }
        if (this.swelling && !this.closing) {
            this.swellClock += Math.max(0f, delta);
            if (this.swellClock >= SWELL_TICKS) {
                this.release();
            }
        }
        if (this.closing && !this.contracting) {
            this.collapseClock += Math.max(0f, delta);
            if (this.collapseClock >= COLLAPSE_TICKS / this.profile.closeSpeed) {
                this.contract();
            }
        }
        float dt = Math.max(0f, delta);
        while (dt > 0f) {
            final float h = Math.min(MAX_STEP, dt);
            this.x0.step(h);
            this.y0.step(h);
            this.x1.step(h);
            this.y1.step(h);
            this.radius.step(h);
            dt -= h;
        }
        if (this.closing) {
            this.roundness = Math.max(this.roundness, 1f - this.value());
        } else if (this.value() >= this.profile.roundSetAt) {
            final float ticks = Math.max(0.01f, this.profile.roundSetTicks);
            this.roundness = Math.max(0f, this.roundness - Math.max(0f, delta) / ticks);
        }
    }

    public boolean isClosing() {
        return this.closing;
    }

    public boolean isDone() {
        return this.closing && this.contracting && this.x0.settled(0.35f) && this.y0.settled(0.35f) && this.x1.settled(0.35f) && this.y1.settled(0.35f) && this.radius.settled(0.35f);
    }

    public float value() {
        if (this.anchor == null || this.panel == null) {
            return 0f;
        }
        final float total = Math.abs(this.panel.x() - this.anchor.x()) + Math.abs(this.panel.y() - this.anchor.y()) + Math.abs(this.panel.x() + this.panel.w() - this.anchor.x() - this.anchor.w()) + Math.abs(this.panel.y() + this.panel.h() - this.anchor.y() - this.anchor.h());
        if (total < 1f) {
            return this.closing ? 0f : 1f;
        }
        final float remaining = Math.abs(this.panel.x() - this.x0.value) + Math.abs(this.panel.y() - this.y0.value) + Math.abs(this.panel.x() + this.panel.w() - this.x1.value) + Math.abs(this.panel.y() + this.panel.h() - this.y1.value);
        return Mth.clamp(1f - remaining / total, 0f, 1f);
    }

    private float anchorSeparation() {
        final float gap = Math.abs(this.x0.value - this.anchor.x()) + Math.abs(this.y0.value - this.anchor.y()) + Math.abs(this.x1.value - (this.anchor.x() + this.anchor.w())) + Math.abs(this.y1.value - (this.anchor.y() + this.anchor.h()));
        final float span = Math.max(1f, 0.5f * (this.anchor.w() + this.anchor.h()));
        final float t = Mth.clamp(gap / span, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    public float anchorDeviation() {
        if (this.anchor == null) {
            return Float.MAX_VALUE;
        }
        final float left = Math.abs(this.x0.value - this.anchor.x());
        final float top = Math.abs(this.y0.value - this.anchor.y());
        final float right = Math.abs(this.x1.value - (this.anchor.x() + this.anchor.w()));
        final float bottom = Math.abs(this.y1.value - (this.anchor.y() + this.anchor.h()));
        return Math.max(Math.max(left, top), Math.max(right, bottom));
    }

    public float anchorSize() {
        return this.anchor == null ? 0f : Math.min(this.anchor.w(), this.anchor.h());
    }

    public LiquidRect current() {
        final float w = Math.max(1f, this.x1.value - this.x0.value);
        final float h = Math.max(1f, this.y1.value - this.y0.value);
        return new LiquidRect(this.x0.value, this.y0.value, w, h, this.bodyRadius(Math.min(w, h)));
    }

    private float bodyRadius(final float minDim) {
        float r = Math.max(0f, this.radius.value);
        final float swell = this.profile.roundnessSwell * this.roundness * this.landingRelease();
        if (swell > 0f) {
            r = Math.max(r, swell * 0.5f * minDim);
        }
        return Math.min(r, 0.5f * minDim);
    }

    private float landingRelease() {
        if (!this.closing) {
            return 1f;
        }
        final float t = Mth.clamp(this.value() / ROUND_LAND_SPAN, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    public LiquidShape compute() {
        if (this.anchor == null || this.panel == null) {
            return new LiquidShape(List.of(), REST_SMOOTHING);
        }
        final float w = Math.max(1f, this.x1.value - this.x0.value);
        final float h = Math.max(1f, this.y1.value - this.y0.value);
        final float minDim = Math.min(w, h);
        final float g = this.value();
        final List<LiquidRect> blobs = new ArrayList<>(3);
        blobs.add(new LiquidRect(this.x0.value, this.y0.value, w, h, this.bodyRadius(minDim)));
        if (UiStyle.minecraft()) {
            return new LiquidShape(blobs, 0f);
        }
        if (this.profile.anchorResidue > 0f) {
            float s = 1f - Mth.clamp(g / this.profile.anchorResidue, 0f, 1f);
            s = s * s * (3f - 2f * s);
            s *= this.anchorSeparation();
            final float rw = this.anchor.w() * s;
            final float rh = this.anchor.h() * s;
            if (rw > 1.5f && rh > 1.5f) {
                blobs.add(new LiquidRect(this.anchor.centerX() - 0.5f * rw, this.anchor.centerY() - 0.5f * rh, rw, rh, Math.min(this.anchor.clampedRadius(), 0.5f * Math.min(rw, rh))));
            }
        }
        if (this.bridged && this.profile.bridge > 0f) {
            final float envelope = (float) Math.sin(g * Math.PI);
            final float bs = this.profile.bridge * minDim * envelope;
            if (bs > 1.5f) {
                final float bx = Mth.lerp(g, this.anchor.centerX(), this.panel.centerX());
                final float by = Mth.lerp(g, this.anchor.centerY(), this.panel.centerY());
                blobs.add(LiquidRect.circle(bx, by, bs));
            }
        }
        final float speed = Math.abs(this.x0.velocity) + Math.abs(this.y0.velocity) + Math.abs(this.x1.velocity) + Math.abs(this.y1.velocity);
        final float speedNorm = speed / Math.max(8f, minDim);
        final float k = REST_SMOOTHING + Math.min(MELT_CAP, MELT_GAIN * speedNorm);
        return new LiquidShape(blobs, k);
    }

    private static LiquidRect snapped(final LiquidRect r) {
        final int x0 = Math.round(r.x());
        final int y0 = Math.round(r.y());
        final int x1 = Math.round(r.x() + r.w());
        final int y1 = Math.round(r.y() + r.h());
        return new LiquidRect(x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0), 0f);
    }

    public void emit(final WLGraphics2D ctx, final GlassStyle baseStyle, final int layer) {
        final LiquidShape shape = this.compute();
        final List<LiquidRect> blobs = shape.blobs();
        for (int i = 0; i < blobs.size(); i++) {
            final LiquidRect b = UiStyle.minecraft() ? snapped(blobs.get(i)) : blobs.get(i);
            if (b.w() < 0.5f || b.h() < 0.5f) {
                continue;
            }
            final GlassStyle s = baseStyle.copy().layer(layer).smoothing(shape.smoothing()).fadeKey(this.id * 64L + i);
            ctx.glass().dimensions(b.x(), b.y(), b.w(), b.h()).cornerRadius(b.clampedRadius()).style(s).extract();
        }
    }
}
