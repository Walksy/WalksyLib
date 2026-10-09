package main.walksy.lib.api.config.option;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.api.config.value.CroppedTexture;
import main.walksy.lib.api.config.value.PixelGridAnimation;
import main.walksy.lib.api.config.value.WLColor;
import main.walksy.lib.core.utils.log.InternalLog;

import java.util.Objects;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class OptionChangeLogger {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private boolean suppressed;

    public void suppress(final Runnable action) {
        final boolean previous = this.suppressed;
        this.suppressed = true;
        try {
            action.run();
        } finally {
            this.suppressed = previous;
        }
    }

    void logChange(final Option<?> option, final Object oldVal, final Object newVal) {
        if (this.suppressed || Objects.equals(oldVal, newVal)) {
            return;
        }
        final String configName = option.sourceName();
        if (oldVal instanceof WLColor oldColor && newVal instanceof WLColor newColor) {
            this.logColorChange(configName, option, oldColor, newColor);
        } else if (oldVal instanceof PixelGridAnimation oldAnim && newVal instanceof PixelGridAnimation newAnim) {
            this.logAnimationChange(configName, option, oldAnim, newAnim);
        } else if (oldVal instanceof final CroppedTexture oldTexture && newVal instanceof final CroppedTexture newTexture && Objects.equals(oldTexture.texture(), newTexture.texture())) {
            this.logField(configName, option, option.getName() + "'s Crop", this.crop(oldTexture), this.crop(newTexture));
        } else {
            this.logField(configName, option, option.getName(), oldVal, newVal);
        }
    }

    private void logColorChange(final String configName, final Option<?> option, final WLColor oldColor, final WLColor newColor) {
        final String name = option.getName();
        if (oldColor.getRGB() != newColor.getRGB()) {
            this.logField(configName, option, name, this.hex(oldColor.getRGB()), this.hex(newColor.getRGB()));
        }
        if (oldColor.isRainbow() != newColor.isRainbow()) {
            this.logField(configName, option, name + "'s Rainbow", oldColor.isRainbow(), newColor.isRainbow());
        }
        if (oldColor.getRainbowSpeed() != newColor.getRainbowSpeed()) {
            this.logField(configName, option, name + "'s Rainbow Speed", oldColor.getRainbowSpeed(), newColor.getRainbowSpeed());
        }
        if (oldColor.isPulse() != newColor.isPulse()) {
            this.logField(configName, option, name + "'s Pulse", oldColor.isPulse(), newColor.isPulse());
        }
        if (oldColor.getPulseSpeed() != newColor.getPulseSpeed()) {
            this.logField(configName, option, name + "'s Pulse Speed", oldColor.getPulseSpeed(), newColor.getPulseSpeed());
        }
        if (Float.compare(oldColor.getSaturation(), newColor.getSaturation()) != 0) {
            this.logField(configName, option, name + "'s Saturation", InternalLog.percent(oldColor.getSaturation()), InternalLog.percent(newColor.getSaturation()));
        }
        if (Float.compare(oldColor.getBrightness(), newColor.getBrightness()) != 0) {
            this.logField(configName, option, name + "'s Brightness", InternalLog.percent(oldColor.getBrightness()), InternalLog.percent(newColor.getBrightness()));
        }
    }

    private void logAnimationChange(final String configName, final Option<?> option, final PixelGridAnimation oldAnim, final PixelGridAnimation newAnim) {
        final String name = option.getName();
        if (oldAnim.getAnimationSpeed() != newAnim.getAnimationSpeed()) {
            this.logField(configName, option, name + "'s Speed", oldAnim.getAnimationSpeed(), newAnim.getAnimationSpeed());
        }
        if (Double.compare(oldAnim.getOffsetX(), newAnim.getOffsetX()) != 0) {
            this.logField(configName, option, name + "'s X Pos", oldAnim.getOffsetX(), newAnim.getOffsetX());
        }
        if (Double.compare(oldAnim.getOffsetY(), newAnim.getOffsetY()) != 0) {
            this.logField(configName, option, name + "'s Y Pos", oldAnim.getOffsetY(), newAnim.getOffsetY());
        }
    }

    private String crop(final CroppedTexture texture) {
        return InternalLog.percent(texture.u1() - texture.u0()) + " x " + InternalLog.percent(texture.v1() - texture.v0());
    }

    private String hex(final int argb) {
        return String.format("#%06X", argb & 0xFFFFFF);
    }

    private <V> void logField(final String configName, final Option<?> option, final String name, final V oldVal, final V newVal) {
        if (Objects.equals(oldVal, newVal)) {
            return;
        }
        final String warning = option.getWarning() == null ? null : option.getWarning().message();
        final String time = LocalTime.now().format(TIME_FORMAT);
        WalksyLib.getLogger().change(InternalLog.of(time, configName, name, oldVal, newVal, warning, option.sourceIsLibrary()));
    }
}
