package main.walksy.lib.api.config;

import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class PathUtils {
    public static final String LIBRARY_DIR = "WalksyLib";
    private static final String UNIX_DIR = "walksylib";

    private PathUtils() {
    }

    public static Path ofConfigDir(final String fileName) {
        return FabricLoader.getInstance().getConfigDir().resolve(fileName + ".json");
    }

    public static Path ofLibraryDir(final String fileName) {
        return FabricLoader.getInstance().getConfigDir().resolve(LIBRARY_DIR).resolve(fileName + ".json");
    }

    public static Path ofGlobalDir(final String fileName) {
        final Path root = writableGlobalRoot();
        return root == null ? ofLibraryDir(fileName) : root.resolve(fileName + ".json");
    }

    private static @Nullable Path writableGlobalRoot() {
        final Path root = globalRoot();
        if (root == null) {
            return null;
        }
        try {
            Files.createDirectories(root);
            return Files.isWritable(root) ? root : null;
        } catch (final IOException | SecurityException e) {
            return null;
        }
    }

    private static @Nullable Path globalRoot() {
        final String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            final String appData = System.getenv("APPDATA");
            return appData == null || appData.isBlank() ? null : Path.of(appData, LIBRARY_DIR);
        }
        final String home = System.getProperty("user.home");
        if (home == null || home.isBlank()) {
            return null;
        }
        if (os.contains("mac")) {
            return Path.of(home, "Library", "Application Support", LIBRARY_DIR);
        }
        final String xdg = System.getenv("XDG_CONFIG_HOME");
        return (xdg == null || xdg.isBlank() ? Path.of(home, ".config") : Path.of(xdg)).resolve(UNIX_DIR);
    }
}
