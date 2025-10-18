package main.walksy.lib.core.config.local.options.type;

public class WalksyLibColor {

    private int value;
    private float hue = 0f;
    private float saturation;
    private float brightness;
    private boolean rainbow = false;
    private int rainbowSpeed = 5;
    private int pulseSpeed = 5;
    private boolean pulse = false;

    private float pulseTime = 0;

    public WalksyLibColor(int r, int g, int b) {
        this(r, g, b, 255);
    }

    public WalksyLibColor(int r, int g, int b, int a) {
        value = ((a & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8)  |
                ((b & 0xFF) << 0);
        float[] hsb = RGBtoHSB(r, g, b, null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
    }

    public WalksyLibColor(int rgb) {
        value = 0xff000000 | rgb;
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        float[] hsb = RGBtoHSB(r, g, b, null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
    }

    public boolean isRainbow() {
        return rainbow;
    }

    public void setHue(float hue) {
        this.hue = Math.max(0f, Math.min(1f, hue));
    }

    public float getHue() {
        return this.hue;
    }

    public void setSaturation(float saturation) {
        this.saturation = Math.max(0f, Math.min(1f, saturation));
    }

    public float getSaturation() {
        return this.saturation;
    }

    public void setBrightness(float brightness) {
        this.brightness = Math.max(0f, Math.min(1f, brightness));
    }

    public float getBrightness() {
        return this.brightness;
    }

    public int getRainbowSpeed()
    {
        return this.rainbowSpeed;
    }

    public void setRainbow(boolean rainbow) {
        this.rainbow = rainbow;
    }

    public void setRainbowSpeed(int rainbowSpeed) {
        this.rainbowSpeed = rainbowSpeed;
    }

    public int getPulseSpeed() {
        return pulseSpeed;
    }

    public void setPulseSpeed(int pulseSpeed) {
        this.pulseSpeed = pulseSpeed;
    }

    public boolean isPulse()
    {
        return pulse;
    }

    public void setPulse(boolean pulse)
    {
        this.pulse = pulse;
    }

    public void setAlpha(int alpha) {
        WalksyLibColor newColor = new WalksyLibColor(this.getRed(), this.getGreen(), this.getBlue(), alpha);
        this.value = newColor.getRGB();
    }

    public void resetHSB()
    {
        float[] hsb = RGBtoHSB(this.getRed(), this.getGreen(), this.getBlue(), null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
    }

    public void resetAdditions()
    {
        this.pulse = false;
        this.rainbow = false;
        this.rainbowSpeed = 5;
        this.pulseSpeed = 5;
    }

    public Additions getAdditions() {
        return new Additions(hue, saturation, brightness, rainbow, rainbowSpeed, pulse, pulseSpeed);
    }

    public void setAdditions(Additions additions) {
        setHue(additions.hue);
        setSaturation(additions.saturation);
        setBrightness(additions.brightness);
        setRainbow(additions.rainbow);
        setRainbowSpeed(additions.rainbowSpeed);
        setPulse(additions.pulse);
        setPulseSpeed(additions.pulseSpeed);
    }

    public void tick()
    {
        if (!rainbow) return;

        float speed = (float) this.rainbowSpeed / 1000;
        hue += speed;
        if (hue > 1f) hue = 0f;

        WalksyLibColor newColor = getHSBColor(hue, saturation, brightness);
        WalksyLibColor newColorAlpha = new WalksyLibColor(newColor.getRed(), newColor.getGreen(), newColor.getBlue(), getAlpha());
        this.value = newColorAlpha.getRGB();
        this.handlePulse();
    }

    private void handlePulse() {
        if (!this.pulse) return;

        pulseTime += (float) this.pulseSpeed / 1000f;
        brightness = (float) ((Math.sin(pulseTime * 2 * Math.PI) + 1) / 2);
        WalksyLibColor newColor = getHSBColor(hue, saturation, brightness);
        WalksyLibColor newColorAlpha = new WalksyLibColor(newColor.getRed(), newColor.getGreen(), newColor.getBlue(), getAlpha());
        this.value = newColorAlpha.getRGB();
    }

    public static WalksyLibColor getHSBColor(float h, float s, float b) {
        return new WalksyLibColor(HSBtoRGB(h, s, b));
    }

    public int getRGB()
    {
        return value;
    }

    public int getRed()
    {
        return (getRGB() >> 16) & 0xFF;
    }

    public int getGreen() {
        return (getRGB() >> 8) & 0xFF;
    }

    public int getBlue() {
        return (getRGB() >> 0) & 0xFF;
    }

    public int getAlpha() {
        return (getRGB() >> 24) & 0xff;
    }

    public static int HSBtoRGB(float hue, float saturation, float brightness) {
        int r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float)Math.floor(hue)) * 6.0f;
            float f = h - (float)java.lang.Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
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

    public static float[] RGBtoHSB(int r, int g, int b, float[] hsbvals) {
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
            float redc = ((float) (cmax - r)) / ((float) (cmax - cmin));
            float greenc = ((float) (cmax - g)) / ((float) (cmax - cmin));
            float bluec = ((float) (cmax - b)) / ((float) (cmax - cmin));
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
        WalksyLibColor copy = new WalksyLibColor(this.getRed(), this.getGreen(), this.getBlue(), this.getAlpha());

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
    public boolean equals(Object obj) {
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


    public static class Additions {
        public float hue;
        public float saturation;
        public float brightness;
        public boolean rainbow;
        public int rainbowSpeed;
        public boolean pulse;
        public int pulseSpeed;

        public Additions(float hue, float saturation, float brightness, boolean rainbow, int rainbowSpeed, boolean pulse, int pulseSpeed) {
            this.hue = hue;
            this.saturation = saturation;
            this.brightness = brightness;
            this.rainbow = rainbow;
            this.rainbowSpeed = rainbowSpeed;
            this.pulse = pulse;
            this.pulseSpeed = pulseSpeed;
        }
    }
}
