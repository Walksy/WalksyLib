package main.walksy.lib.core.hud;

import main.walksy.lib.api.hud.HudElement;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.api.config.Config;
import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.core.config.serialization.JsonFiles;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.api.config.PathUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HudSpaces {
    private static final String KEY = "hud";
    private final Map<String, HudSpace> spaces = new LinkedHashMap<>();
    private final List<HudSpace> created = new ArrayList<>();
    private boolean dirty;
    private boolean loaded;
    private JsonObject saved;

    private static Path file() {
        return PathUtils.ofLibraryDir("walksylib");
    }

    public HudSpace create(final String name, final HudElement element) {
        final HudSpace space = new HudSpace(null, name, element);
        synchronized (this) {
            this.created.add(space);
        }
        return space;
    }

    public synchronized void warnUnregistered() {
        for (final HudSpace space : this.created) {
            if (!space.isBound()) {
                WalksyLib.getLogger().err("HUD element \"" + space.name() + "\" was created with " + "HudSpaces.create(...) but never given to an option, so nothing registered it: " + "it is not in Edit HUD, it is not drawn and its position is not saved. " + "Pass it to Options.ofHudSpace(space), or to OptionBuilder#hudSpace(space) " + "on an option you already have.");
            }
        }
        this.created.clear();
    }

    public synchronized void renderAll(final WLGraphics2D graphics) {
        if (HudEditScreen.isOpen()) {
            return;
        }
        this.ensureLoaded();
        for (final HudSpace space : this.spaces.values()) {
            if (space.element().visible() && !WalksyLib.getInstance().getServerOptOuts().isModDisabled(ownerOf(space.id()))) {
                space.render(graphics, false);
            }
        }
    }

    private static String ownerOf(final String id) {
        if (id == null) {
            return "";
        }
        final int colon = id.indexOf(':');
        return colon < 0 ? id : id.substring(0, colon);
    }

    public synchronized HudSpace register(final String id, final String name, final HudElement element) {
        this.ensureLoaded();
        final HudSpace existing = this.spaces.get(id);
        final HudSpace space = new HudSpace(id, name, element);
        if (existing != null) {
            space.setAnchored(existing.anchorX(), existing.anchorY(), existing.offsetX(), existing.offsetY());
            space.setScale(existing.scale());
        } else {
            this.applySaved(space);
        }
        this.spaces.put(id, space);
        return space;
    }

    public void registerFrom(final String modId, final Config config) {
        if (config == null) {
            return;
        }
        config.options().forEach(option -> this.registerOption(modId, option));
    }

    private synchronized void registerOption(final String modId, final Option<?> option) {
        final HudElement element = option.getHudElement();
        if (element == null) {
            return;
        }
        final String id = modId + ":" + option.getSaveKey() + ":" + option.getHudName();
        final HudSpace held = option.getHudSpace();
        if (held == null) {
            option.bindHudSpace(this.register(id, option.getHudName(), element));
            return;
        }
        if (held.isBound()) {
            return;
        }
        this.ensureLoaded();
        held.bind(id);
        this.applySaved(held);
        this.spaces.put(id, held);
    }

    public synchronized List<HudSpace> all() {
        this.ensureLoaded();
        return List.copyOf(this.spaces.values());
    }

    public List<HudSpace> forMod(final String modId) {
        final String prefix = modId + ":";
        final List<HudSpace> mine = new ArrayList<>();
        for (final HudSpace space : this.all()) {
            if (space.id() != null && space.id().startsWith(prefix)) {
                mine.add(space);
            }
        }
        return mine;
    }

    public List<HudSpace> visible() {
        final List<HudSpace> shown = new ArrayList<>();
        for (final HudSpace space : this.all()) {
            if (space.element().visible()) {
                shown.add(space);
            }
        }
        return shown;
    }

    public boolean isEmpty() {
        return this.all().isEmpty();
    }

    synchronized void markDirty() {
        this.dirty = true;
    }

    public synchronized void save() {
        if (!this.dirty) {
            return;
        }
        this.dirty = false;
        JsonObject root = JsonFiles.read(file());
        if (root == null) {
            root = new JsonObject();
        }
        final JsonObject hud = new JsonObject();
        for (final HudSpace space : this.spaces.values()) {
            final JsonObject entry = new JsonObject();
            if (space.isLegacy()) {
                entry.addProperty("x", space.legacyX());
                entry.addProperty("y", space.legacyY());
            } else {
                entry.addProperty("anchorX", space.anchorX());
                entry.addProperty("anchorY", space.anchorY());
                entry.addProperty("offsetX", space.offsetX());
                entry.addProperty("offsetY", space.offsetY());
            }
            entry.addProperty("scale", space.scale());
            hud.add(space.id(), entry);
        }
        root.add(KEY, hud);
        JsonFiles.write(file(), root, "hud layout");
    }

    private void ensureLoaded() {
        if (this.loaded) {
            return;
        }
        this.loaded = true;
        this.saved = JsonFiles.read(file());
    }

    private void applySaved(final HudSpace space) {
        final JsonElement hud = this.saved == null ? null : this.saved.get(KEY);
        if (hud == null || !hud.isJsonObject()) {
            return;
        }
        final JsonElement entry = hud.getAsJsonObject().get(space.id());
        if (entry == null || !entry.isJsonObject()) {
            return;
        }
        final JsonObject object = entry.getAsJsonObject();
        if (object.has("anchorX") || object.has("offsetX")) {
            space.setAnchored(number(object, "anchorX", 0.5f), number(object, "anchorY", 0.5f), number(object, "offsetX", 0f), number(object, "offsetY", 0f));
        } else {
            space.setLegacyFraction(number(object, "x", 0.5f), number(object, "y", 0.5f));
        }
        space.setScale(number(object, "scale", 1f));
        this.dirty = false;
    }

    private static float number(final JsonObject object, final String key, final float fallback) {
        final JsonElement element = object.get(key);
        try {
            return element == null || element.isJsonNull() ? fallback : element.getAsFloat();
        } catch (final RuntimeException e) {
            return fallback;
        }
    }

    public synchronized void reset() {
        this.spaces.clear();
        this.created.clear();
        this.loaded = false;
        this.dirty = false;
        this.saved = null;
    }
}
