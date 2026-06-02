package main.walksy.lib.core.config.local.options.type;

public class WalksyLibColor implements Tickable {

    private int value;
    private float hue = 0f;
    private float saturation;
    private float brightness;
    private boolean rainbow = false;
    private int rainbowSpeed = 5;
    private int pulseSpeed = 5;
    private boolean pulse = false;

    private float pulseTime = 0;

    public WalksyLibColor(final int r, final int g, final int b) {
        this(r, g, b, 255);
    }

    public WalksyLibColor(final int r, final int g, final int b, final int a) {
        this.value = ((a & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8)  |
                ((b & 0xFF) << 0);
        final float[] hsb = RGBtoHSB(r, g, b, null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
    }

    public WalksyLibColor(final int rgb) {
        this.value = 0xff000000 | rgb;
        final int r = (rgb >> 16) & 0xFF;
        final int g = (rgb >> 8) & 0xFF;
        final int b = rgb & 0xFF;

        final float[] hsb = RGBtoHSB(r, g, b, null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
    }

    public boolean isRainbow() {
        return this.rainbow;
    }

    public void setHue(final float hue) {
        this.hue = Math.max(0f, Math.min(1f, hue));
    }

    public float getHue() {
        return this.hue;
    }

    public void setSaturation(final float saturation) {
        this.saturation = Math.max(0f, Math.min(1f, saturation));
    }

    public float getSaturation() {
        return this.saturation;
    }

    public void setBrightness(final float brightness) {
        this.brightness = Math.max(0f, Math.min(1f, brightness));
    }

    public float getBrightness() {
        return this.brightness;
    }

    public int getRainbowSpeed() {
        return this.rainbowSpeed;
    }

    public void setRainbow(final boolean rainbow) {
        this.rainbow = rainbow;
    }

    public void setRainbowSpeed(final int rainbowSpeed) {
        this.rainbowSpeed = rainbowSpeed;
    }

    public int getPulseSpeed() {
        return this.pulseSpeed;
    }

    public void setPulseSpeed(final int pulseSpeed) {
        this.pulseSpeed = pulseSpeed;
    }

    public boolean isPulse() {
        return this.pulse;
    }

    public void setPulse(final boolean pulse) {
        this.pulse = pulse;
    }

    public void setAlpha(final int alpha) {
        final WalksyLibColor newColor = new WalksyLibColor(this.getRed(), this.getGreen(), this.getBlue(), alpha);
        this.value = newColor.getRGB();
    }

    public void resetHSB() {
        final float[] hsb = RGBtoHSB(this.getRed(), this.getGreen(), this.getBlue(), null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
    }

    public void resetAdditions() {
        this.pulse = false;
        this.rainbow = false;
        this.rainbowSpeed = 5;
        this.pulseSpeed = 5;
    }

    public Additions getAdditions() {
        return new Additions(this.hue, this.saturation, this.brightness, this.rainbow, this.rainbowSpeed, this.pulse, this.pulseSpeed);
    }

    public void setAdditions(final Additions additions) {
        this.setHue(additions.hue());
        this.setSaturation(additions.saturation());
        this.setBrightness(additions.brightness());
        this.setRainbow(additions.rainbow());
        this.setRainbowSpeed(additions.rainbowSpeed());
        this.setPulse(additions.pulse());
        this.setPulseSpeed(additions.pulseSpeed());
    }

    @Override
    public void tick() {
        if (this.rainbow) {
            final float speed = (float) this.rainbowSpeed / 1000;
            this.hue += speed;
            if (this.hue > 1f) this.hue = 0f;

            final WalksyLibColor newColor = getHSBColor(this.hue, this.saturation, this.brightness);
            final WalksyLibColor newColorAlpha = new WalksyLibColor(newColor.getRed(), newColor.getGreen(), newColor.getBlue(), this.getAlpha());
            this.value = newColorAlpha.getRGB();
        }
        this.handlePulse();
    }

    private void handlePulse() {
        if (!this.pulse) return;

        this.pulseTime += (float) this.pulseSpeed / 1000f;
        this.brightness = (float) ((Math.sin(this.pulseTime * 2 * Math.PI) + 1) / 2);
        final WalksyLibColor newColor = getHSBColor(this.hue, this.saturation, this.brightness);
        final WalksyLibColor newColorAlpha = new WalksyLibColor(newColor.getRed(), newColor.getGreen(), newColor.getBlue(), this.getAlpha());
        this.value = newColorAlpha.getRGB();
    }

    public static WalksyLibColor getHSBColor(final float h, final float s, final float b) {
        return new WalksyLibColor(HSBtoRGB(h, s, b));
    }

    public int getRGB() {
        return this.value;
    }

    public int getRed() {
        return (this.getRGB() >> 16) & 0xFF;
    }

    public int getGreen() {
        return (this.getRGB() >> 8) & 0xFF;
    }

    public int getBlue() {
        return (this.getRGB() >> 0) & 0xFF;
    }

    public int getAlpha() {
        return (this.getRGB() >> 24) & 0xff;
    }

    public static int HSBtoRGB(final float hue, final float saturation, final float brightness) {
        int r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            final float h = (hue - (float) Math.floor(hue)) * 6.0f;
            final float f = h - (float) Math.floor(h);
            final float p = brightness * (1.0f - saturation);
            final float q = brightness * (1.0f - saturation * f);
            final float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
                case 0:
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (t * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                    break;
                case 1:
                    r = (int) (q * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                    break;
                case 2:
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (t * 255.0f + 0.5f);
                    break;
                case 3:
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (q * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                    break;
                case 4:
                    r = (int) (t * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                    break;
                case 5:
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (q * 255.0f + 0.5f);
                    break;
            }
        }
        return 0xff000000 | (r << 16) | (g << 8) | (b << 0);
    }

    public static float[] RGBtoHSB(final int r, final int g, final int b, float[] hsbvals) {
        float hue, saturation, brightness;
        if (hsbvals == null) {
            hsbvals = new float[3];
        }
        int cmax = (r > g) ? r : g;
        if (b > cmax) cmax = b;
        int cmin = (r < g) ? r : g;
        if (b < cmin) cmin = b;

        brightness = ((float) cmax) / 255.0f;
        if (cmax != 0)
            saturation = ((float) (cmax - cmin)) / ((float) cmax);
        else
            saturation = 0;
        if (saturation == 0)
            hue = 0;
        else {
            final float redc = ((float) (cmax - r)) / ((float) (cmax - cmin));
            final float greenc = ((float) (cmax - g)) / ((float) (cmax - cmin));
            final float bluec = ((float) (cmax - b)) / ((float) (cmax - cmin));
            if (r == cmax)
                hue = bluec - greenc;
            else if (g == cmax)
                hue = 2.0f + redc - bluec;
            else
                hue = 4.0f + greenc - redc;
            hue = hue / 6.0f;
            if (hue < 0)
                hue = hue + 1.0f;
        }
        hsbvals[0] = hue;
        hsbvals[1] = saturation;
        hsbvals[2] = brightness;
        return hsbvals;
    }

    public WalksyLibColor copy() {
        final WalksyLibColor copy = new WalksyLibColor(this.getRed(), this.getGreen(), this.getBlue(), this.getAlpha());

        copy.hue = this.hue;
        copy.saturation = this.saturation;
        copy.brightness = this.brightness;
        copy.rainbow = this.rainbow;
        copy.rainbowSpeed = this.rainbowSpeed;
        copy.pulse = this.pulse;
        copy.pulseSpeed = this.pulseSpeed;
        copy.pulseTime = this.pulseTime;

        return copy;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof WalksyLibColor other)) return false;
        if (this.rainbowSpeed != other.rainbowSpeed) return false;
        if (this.rainbow != other.rainbow) return false;
        if (this.pulseSpeed != other.pulseSpeed) return false;
        if (this.pulse != other.pulse) return false;
        if (this.brightness != other.brightness && !this.pulse) return false;
        if (this.saturation != other.saturation) return false;
        if (this.value != other.value && !this.rainbow && !this.pulse) return false;
        return true;
    }


    public record Additions(float hue, float saturation, float brightness, boolean rainbow, int rainbowSpeed, boolean pulse, int pulseSpeed) {}
}
