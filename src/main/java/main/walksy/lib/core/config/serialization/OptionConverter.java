package main.walksy.lib.core.config.serialization;

import main.walksy.lib.core.config.local.Option;

import java.awt.*;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class OptionConverter {

    public static SerializableOption fromOption(Option<?> opt) {
        SerializableOption s = new SerializableOption();
        s.name = opt.getName();
        s.type = opt.getType().getSimpleName().toLowerCase();
        s.value = opt.getValue();
        s.min = opt.getMin();
        s.max = opt.getMax();
        s.increment = opt.getIncrement();
        s.rainbow = opt.isRainbow();
        s.hue = opt.getHue();
        s.saturation = opt.getSaturation();
        s.brightness = opt.getBrightness();
        s.alpha = opt.getAlpha();
        s.rainbowSpeed = opt.getRainbowSpeed();
        s.pulseSpeed = opt.getPulseSpeed();
        s.pulseValue = opt.getPulseValue();
        return s;
    }
    /*
    public static Option<?> toOption(SerializableOption s, Consumer<Object> setter) {
        Class<?> clazz = switch (s.type) {
            case "boolean" -> Boolean.class;
            case "integer", "int" -> Integer.class;
            case "float" -> Float.class;
            case "double" -> Double.class;
            case "color" -> Color.class;
            default -> throw new IllegalArgumentException("Unsupported type: " + s.type);
        };

        Supplier<Object> getter = () -> s.value;

        Option<Object> opt = new Option(s.name, null, getter, setter, clazz,
                s.min, s.max, s.increment);
        opt.setRainbow(s.rainbow);
        opt.setHue(s.hue);
        opt.setSaturation(s.saturation);
        opt.setBrightness(s.brightness);
        opt.setAlpha(s.alpha);
        opt.setRainbowSpeed(s.rainbowSpeed);
        opt.setPulseSpeed(s.pulseSpeed);
        return opt;
    }

     */

    @SuppressWarnings("unchecked")
    public static void setOptionValue(Option<?> option, Object value) {
        Class<?> type = option.getType();

        if (type == Color.class) {
            if (value instanceof Color color) {
                option.setValue(color);
                return;
            }

            if (value instanceof Map<?, ?> map) {
                try {
                    int r = ((Number) map.get("r")).intValue();
                    int g = ((Number) map.get("g")).intValue();
                    int b = ((Number) map.get("b")).intValue();
                    int a = map.containsKey("a") ? ((Number) map.get("a")).intValue() : 255;
                    option.setValue(new Color(r, g, b, a));
                    return;
                } catch (Exception e) {
                    System.err.println("Failed to convert map to Color: " + e.getMessage());
                }
            }

            throw new IllegalArgumentException("Invalid color value: " + value);
        }

        if (type.isInstance(value)) {
            option.setValue(value);
        } else {
            throw new IllegalArgumentException("Invalid value type: " + value.getClass().getName() +
                    ", expected: " + type.getName());
        }
    }

}
