package main.walksy.lib.core.config.serialization;

import com.google.gson.JsonElement;
import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.manager.WalksyLibConfigManager;
import main.walksy.lib.core.utils.log.WalksyLibLogger;


@SuppressWarnings("unchecked")
public class OptionConverter {

    public static SerializableOption fromOption(final Option<?> opt) {
        final SerializableOption s = new SerializableOption();
        s.name = opt.getName();
        s.type = opt.getType().getSimpleName().toLowerCase();

        s.value = WalksyLibConfigManager.GSON.toJsonTree(opt.getValue(), opt.getType());
        s.min = WalksyLibConfigManager.GSON.toJsonTree(opt.getMin());
        s.max = WalksyLibConfigManager.GSON.toJsonTree(opt.getMax());
        s.increment = WalksyLibConfigManager.GSON.toJsonTree(opt.getIncrement());

        return s;
    }

    public static void setOptionValue(final Option<?> option, final JsonElement valueElement) {
        final Class<?> type = option.getType();

        try {
            final Object value = WalksyLibConfigManager.GSON.fromJson(valueElement, type);
            if (value != null) {
                ((Option<Object>) option).setValue(value);
            }
        } catch (Exception e) {
            WalksyLibLogger.err("Failed to deserialize value for option " + option.getName() +
                    " to type " + type.getSimpleName() + ": " + e.getMessage());
        }
    }

}
