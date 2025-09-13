package main.walksy.lib.core.manager;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonParseException;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.config.Config;
import main.walksy.lib.core.config.impl.APIConfig;
import main.walksy.lib.core.config.impl.LocalConfig;
import main.walksy.lib.core.config.local.Category;
import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.config.serialization.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class WalksyLibConfigManager {

    private final LocalConfig localConfig;
    private final APIConfig apiConfig;

    public WalksyLibConfigManager(LocalConfig localConfig) {
        this.localConfig = localConfig;
        this.apiConfig = new APIConfig();
    }

    public LocalConfig getLocal()
    {
        return localConfig;
    }

    public APIConfig getAPI()
    {
        return apiConfig;
    }

    public static SerializableCategory serializeCategory(Category category) {
        SerializableCategory serialized = new SerializableCategory();
        serialized.name = category.name();
        serialized.options = new ArrayList<>();
        serialized.groups = new ArrayList<>();

        for (Option<?> option : category.options()) {
            serialized.options.add(OptionConverter.fromOption(option));
        }

        for (OptionGroup group : category.optionGroups()) {
            SerializableGroup serializedGroup = new SerializableGroup();
            serializedGroup.name = group.getName();
            serializedGroup.expanded = group.isExpanded();
            serializedGroup.options = new ArrayList<>();

            for (Option<?> option : group.getOptions()) {
                serializedGroup.options.add(OptionConverter.fromOption(option));
            }

            serialized.groups.add(serializedGroup);
        }

        return serialized;
    }

    public static void applyCategoryValues(Category category, SerializableCategory serializedCategory) {
        for (Option<?> option : category.options()) {
            serializedCategory.options.stream()
                    .filter(serialized -> serialized.name.equals(option.getName()))
                    .findFirst()
                    .ifPresent(serialized -> applyOptionValues(option, serialized));
        }

        for (OptionGroup group : category.optionGroups()) {
            serializedCategory.groups.stream()
                    .filter(serialized -> serialized.name.equals(group.getName()))
                    .findFirst()
                    .ifPresent(serializedGroup -> {
                        group.setExpanded(serializedGroup.expanded);

                        for (Option<?> option : group.getOptions()) {
                            serializedGroup.options.stream()
                                    .filter(serialized -> serialized.name.equals(option.getName()))
                                    .findFirst()
                                    .ifPresent(serialized -> applyOptionValues(option, serialized));
                        }
                    });
        }
    }

    public static void applyOptionValues(Option<?> option, SerializableOption serialized) {
        try {
            OptionConverter.setOptionValue(option, serialized.value);
        } catch (Exception e) {
            System.err.println("Failed to set value for option '" + option.getName() + "': " + e.getMessage());
            return;
        }

        /*
        option.setRainbow(serialized.rainbow);
        option.setHue(serialized.hue);
        option.setSaturation(serialized.saturation);
        option.setBrightness(serialized.brightness);
        option.setAlpha(serialized.alpha);
        option.setRainbowSpeed(serialized.rainbowSpeed);
        option.setPulseSpeed(serialized.pulseSpeed);
        option.setPulse(serialized.pulse);
        
         */
    }
}
