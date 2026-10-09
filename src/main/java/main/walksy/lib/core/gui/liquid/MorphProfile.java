package main.walksy.lib.core.gui.liquid;

import net.minecraft.util.Mth;

public final class MorphProfile {
    public float roundnessSwell = 1f;
    public float roundSetAt = 0.62f;
    public float roundSetTicks = 4f;
    public float anchorResidue = 0.65f;
    public float bridge = 0.50f;
    public float closeSpeed = 1f;

    public static MorphProfile apple() {
        return new MorphProfile();
    }

    public static MorphProfile blobby() {
        final MorphProfile p = new MorphProfile();
        p.roundSetAt = 1f;
        p.roundSetTicks = 5.5f;
        p.anchorResidue = 0.75f;
        return p;
    }

    public static MorphProfile crisp() {
        final MorphProfile p = new MorphProfile();
        p.roundnessSwell = 0f;
        p.anchorResidue = 0.35f;
        p.bridge = 0.30f;
        return p;
    }

    public static MorphProfile dropdown() {
        final MorphProfile p = new MorphProfile();
        p.roundnessSwell = 0f;
        p.anchorResidue = 0f;
        p.bridge = 0f;
        return p;
    }

    public MorphProfile roundnessSwell(final float v) {
        this.roundnessSwell = Mth.clamp(v, 0f, 1f);
        return this;
    }

    public MorphProfile roundSetAt(final float v) {
        this.roundSetAt = Mth.clamp(v, 0f, 1f);
        return this;
    }

    public MorphProfile roundSetTicks(final float v) {
        this.roundSetTicks = Math.max(0.01f, v);
        return this;
    }

    public MorphProfile anchorResidue(final float v) {
        this.anchorResidue = Mth.clamp(v, 0f, 1f);
        return this;
    }

    public MorphProfile bridge(final float v) {
        this.bridge = Math.max(0f, v);
        return this;
    }

    public MorphProfile closeSpeed(final float v) {
        this.closeSpeed = Math.max(0.1f, v);
        return this;
    }

    public MorphProfile copy() {
        final MorphProfile p = new MorphProfile();
        p.roundnessSwell = this.roundnessSwell;
        p.roundSetAt = this.roundSetAt;
        p.roundSetTicks = this.roundSetTicks;
        p.anchorResidue = this.anchorResidue;
        p.bridge = this.bridge;
        p.closeSpeed = this.closeSpeed;
        return p;
    }
}
