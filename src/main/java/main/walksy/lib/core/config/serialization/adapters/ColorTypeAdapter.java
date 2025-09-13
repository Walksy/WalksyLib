package main.walksy.lib.core.config.serialization.adapters;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import main.walksy.lib.core.config.local.options.type.WalksyLibColor;

import java.io.IOException;

public class ColorTypeAdapter extends TypeAdapter<WalksyLibColor> {
    @Override
    public void write(JsonWriter out, WalksyLibColor color) throws IOException {
        if (color == null) {
            out.nullValue();
            return;
        }

        out.beginObject();
        out.name("r").value(color.getRed());
        out.name("g").value(color.getGreen());
        out.name("b").value(color.getBlue());
        out.name("a").value(color.getAlpha());

        out.name("value").value(color.getRGB());
        out.name("hue").value(color.getHue());
        out.name("saturation").value(color.getSaturation());
        out.name("brightness").value(color.getBrightness());
        out.name("rainbow").value(color.isRainbow());
        out.name("rainbowSpeed").value(color.getRainbowSpeed());
        out.name("pulse").value(color.isPulse());
        out.name("pulseSpeed").value(color.getPulseSpeed());
        out.endObject();
    }

    @Override
    public WalksyLibColor read(JsonReader in) throws IOException {
        int r = 0, g = 0, b = 0, a = 255;
        int value = 0;
        float hue = 0f, saturation = 0f, brightness = 0f;
        boolean rainbow = false, pulse = false;
        int rainbowSpeed = 5, pulseSpeed = 5;

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "r" -> r = in.nextInt();
                case "g" -> g = in.nextInt();
                case "b" -> b = in.nextInt();
                case "a" -> a = in.nextInt();
                case "value" -> value = in.nextInt();
                case "hue" -> hue = (float) in.nextDouble();
                case "saturation" -> saturation = (float) in.nextDouble();
                case "brightness" -> brightness = (float) in.nextDouble();
                case "rainbow" -> rainbow = in.nextBoolean();
                case "rainbowSpeed" -> rainbowSpeed = in.nextInt();
                case "pulse" -> pulse = in.nextBoolean();
                case "pulseSpeed" -> pulseSpeed = in.nextInt();
                default -> in.skipValue();
            }
        }
        in.endObject();

        WalksyLibColor color = new WalksyLibColor(r, g, b, a);
        color.setHue(hue);
        color.setSaturation(saturation);
        color.setBrightness(brightness);
        color.setRainbow(rainbow);
        color.setRainbowSpeed(rainbowSpeed);
        color.setPulse(pulse);
        color.setPulseSpeed(pulseSpeed);

        return color;
    }
}
