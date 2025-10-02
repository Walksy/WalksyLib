package main.walksy.lib.core.config.serialization;

import com.google.gson.JsonElement;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.manager.WalksyLibConfigManager;


@SuppressWarnings("unchecked")
public class OptionConverter {

    public static SerializableOption fromOption(Option<?> opt) {
        SerializableOption s = new SerializableOption();
        s.name = opt.getName();
        s.type = opt.getType().getSimpleName().toLowerCase();

        s.value = WalksyLibConfigManager.GSON.toJsonTree(opt.getValue(), opt.getType());
        s.min = WalksyLibConfigManager.GSON.toJsonTree(opt.getMin());
        s.max = WalksyLibConfigManager.GSON.toJsonTree(opt.getMax());
        s.increment = WalksyLibConfigManager.GSON.toJsonTree(opt.getIncrement());

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

    public static void setOptionValue(Option<?> option, JsonElement valueElement) {
        Class<?> type = option.getType();

        Object value;
        try {
            value = WalksyLibConfigManager.GSON.fromJson(valueElement, type);
            if (value != null) {
                ((Option<Object>) option).setValue(value);
            }
        } catch (Exception e) {
            System.err.println("Failed to deserialize value for option " + option.getName() +
                    " to type " + type.getSimpleName() + ": " + e.getMessage());
        }
    }

}
