package main.walksy.lib.core.config.serialization;

import main.walksy.lib.core.WalksyLib;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class JsonFiles {
    private JsonFiles() {
    }

    public static JsonObject read(final Path path) {
        if (!Files.exists(path)) {
            return null;
        }
        try {
            final JsonElement root = JsonParser.parseString(Files.readString(path));
            return root.isJsonObject() ? root.getAsJsonObject() : null;
        } catch (final JsonParseException | IOException e) {
            WalksyLib.getLogger().err("Failed to read " + path + ": " + e.getMessage());
            return null;
        }
    }

    public static void write(final Path path, final JsonObject root, final String what) {
        final Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(temp, ConfigSerializer.GSON.toJson(root));
            moveIntoPlace(temp, path);
        } catch (final IOException e) {
            WalksyLib.getLogger().err("Failed to save " + what + " to " + path + ": " + e.getMessage());
            try {
                Files.deleteIfExists(temp);
            } catch (final IOException ignored) {
            }
        }
    }

    static void moveIntoPlace(final Path temp, final Path target) throws IOException {
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (final AtomicMoveNotSupportedException unsupported) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
