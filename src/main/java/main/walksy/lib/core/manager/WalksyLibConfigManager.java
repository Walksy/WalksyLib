package main.walksy.lib.core.manager;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import main.walksy.lib.core.config.Config;
import main.walksy.lib.core.config.impl.APIConfig;
import main.walksy.lib.core.config.impl.LocalConfig;
import main.walksy.lib.core.config.local.Category;
import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.config.serialization.*;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class WalksyLibConfigManager {

    public LocalConfig localConfig;
    public APIConfig apiConfig;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Color.class, new ColorAdapter())
            .setPrettyPrinting()
            .create();

    public WalksyLibConfigManager(LocalConfig localConfig) {
        this.localConfig = localConfig;
        this.apiConfig = new APIConfig();
    }

    public void save(@Nullable Config config) {
        if (config == null || config instanceof LocalConfig) {
            saveLocal(this.localConfig);
        }
    }

    public void load(@Nullable Config config) {
        if (config == null || config instanceof LocalConfig) {
            loadLocal(this.localConfig);
        }
    }

    private void saveLocal(LocalConfig config) {
        Path path = config.path();
        List<Category> categories = config.getCategories();
        List<SerializableCategory> serializable = new ArrayList<>();

        for (Category category : categories) {
            SerializableCategory sc = new SerializableCategory();
            sc.name = category.name();
            sc.options = new ArrayList<>();
            for (Option<?> opt : category.options()) {
                sc.options.add(OptionConverter.fromOption(opt));
            }

            sc.groups = new ArrayList<>();
            for (OptionGroup group : category.optionGroups()) {
                SerializableGroup sg = new SerializableGroup();
                sg.name = group.getName();
                sg.expanded = group.isExpanded();
                sg.options = new ArrayList<>();
                for (Option<?> opt : group.getOptions()) {
                    sg.options.add(OptionConverter.fromOption(opt));
                }
                sc.groups.add(sg);
            }

            serializable.add(sc);
        }

        try {
            Files.createDirectories(path.getParent());
            String json = gson.toJson(serializable);
            Files.writeString(path, json);
        } catch (IOException e) {
            System.err.println("Failed to save config to " + path + ": " + e.getMessage());
        }
    }

    private void loadLocal(LocalConfig config) {
        Path path = config.path();
        if (!Files.exists(path)) {
            saveLocal(config);
            return;
        }

        List<SerializableCategory> loaded;
        try {
            String json = Files.readString(path);
            Type type = new TypeToken<List<SerializableCategory>>() {}.getType();
            loaded = gson.fromJson(json, type);
        } catch (IOException | JsonParseException e) {
            System.err.println("Failed to read or parse config from " + path + ": " + e.getMessage());
            return;
        }

        for (Category existing : config.getCategories()) {
            for (SerializableCategory sc : loaded) {
                if (!existing.name().equals(sc.name)) continue;

                for (Option<?> opt : existing.options()) {
                    for (SerializableOption so : sc.options) {
                        if (!opt.getName().equals(so.name)) continue;
                        applyOptionValue(opt, so);
                        break;
                    }
                }

                for (OptionGroup group : existing.optionGroups()) {
                    for (SerializableGroup sg : sc.groups) {
                        if (!group.getName().equals(sg.name)) continue;

                        group.setExpanded(sg.expanded);

                        for (Option<?> opt : group.getOptions()) {
                            for (SerializableOption so : sg.options) {
                                if (!opt.getName().equals(so.name)) continue;
                                applyOptionValue(opt, so);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private void applyOptionValue(Option<?> opt, SerializableOption so) {
        try {
            OptionConverter.setOptionValue(opt, so.value);
        } catch (Exception e) {
            System.err.println("Failed to set value for option '" + opt.getName() + "': " + e.getMessage());
            return;
        }

        opt.setRainbow(so.rainbow);
        opt.setHue(so.hue);
        opt.setSaturation(so.saturation);
        opt.setBrightness(so.brightness);
        opt.setAlpha(so.alpha);
        opt.setRainbowSpeed(so.rainbowSpeed);
        opt.setPulseSpeed(so.pulseSpeed);
        opt.setPulseValue(so.pulseValue);
    }
}
