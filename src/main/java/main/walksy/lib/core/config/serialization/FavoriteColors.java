package main.walksy.lib.core.config.serialization;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.api.config.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class FavoriteColors {
    private static final int MAX_FAVORITES = 11;
    private static final Gson GSON = new Gson();
    private static final String KEY = "favoriteColors";
    private List<Integer> favorites;

    private Path file() {
        return PathUtils.ofGlobalDir("walksylib");
    }

    private Path previousFile() {
        return PathUtils.ofLibraryDir("walksylib").resolveSibling("favorite_colors.json");
    }

    public synchronized List<Integer> get() {
        this.ensureLoaded();
        return Collections.unmodifiableList(this.favorites);
    }

    public synchronized boolean isFavorite(final int argb) {
        this.ensureLoaded();
        return this.favorites.contains(argb);
    }

    public synchronized void add(final int argb) {
        this.ensureLoaded();
        this.favorites.remove((Integer) argb);
        this.favorites.add(0, argb);
        while (this.favorites.size() > MAX_FAVORITES) {
            this.favorites.remove(this.favorites.size() - 1);
        }
        this.save();
    }

    public synchronized void remove(final int argb) {
        this.ensureLoaded();
        if (this.favorites.remove((Integer) argb)) {
            this.save();
        }
    }

    private void ensureLoaded() {
        if (this.favorites != null) {
            return;
        }
        this.favorites = new ArrayList<>();
        if (this.readFrom(this.file())) {
            return;
        }
        final Path instanceFile = PathUtils.ofLibraryDir("walksylib");
        if (!instanceFile.equals(this.file()) && this.readFrom(instanceFile)) {
            this.save();
            return;
        }
        this.importPrevious();
    }

    private boolean readFrom(final Path file) {
        final JsonObject root = JsonFiles.read(file);
        final JsonElement saved = root == null ? null : root.get(KEY);
        if (saved == null || !saved.isJsonArray()) {
            return false;
        }
        for (final JsonElement element : saved.getAsJsonArray()) {
            if (element != null && element.isJsonPrimitive()) {
                this.favorites.add(element.getAsInt());
            }
        }
        return true;
    }

    private void importPrevious() {
        final Path previous = this.previousFile();
        if (!Files.exists(previous)) {
            return;
        }
        try {
            final Integer[] saved = GSON.fromJson(Files.readString(previous), Integer[].class);
            if (saved != null) {
                this.favorites.addAll(Arrays.asList(saved));
            }
            this.save();
        } catch (final IOException | JsonSyntaxException e) {
            WalksyLib.getLogger().err("Failed to load favorite colors: " + e.getMessage());
            return;
        }
        try {
            Files.move(previous, previous.resolveSibling(previous.getFileName() + ".moved.bak"), StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException e) {
            WalksyLib.getLogger().err("Failed to retire " + previous + ": " + e.getMessage());
        }
    }

    private void save() {
        JsonObject root = JsonFiles.read(this.file());
        if (root == null) {
            root = new JsonObject();
        }
        final JsonArray array = new JsonArray();
        for (final int argb : this.favorites) {
            array.add(argb);
        }
        root.add(KEY, array);
        JsonFiles.write(this.file(), root, "favorite colors");
    }
}
