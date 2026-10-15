package main.walksy.lib.core.mods.remote;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.WalksyLibConstants;
import main.walksy.lib.core.platform.WLLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.ModOrigin;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class SupersededJars {

    private SupersededJars() {
    }

    public static void sweepAsync(final Collection<String> modIds) {
        if (modIds.isEmpty() || FabricLoader.getInstance().isDevelopmentEnvironment()) {
            return;
        }
        final Set<String> tracked = Set.copyOf(modIds);
        final Thread thread = new Thread(() -> sweep(tracked), "WalksyLib old jar sweep");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    static void sweep(final Set<String> modIds) {
        final Path mods;
        try {
            mods = ModrinthDownload.modsDirectory();
        } catch (final IOException e) {
            return;
        }
        final List<Path> jars;
        try (Stream<Path> listing = Files.list(mods)) {
            jars = listing.filter(SupersededJars::isJar).toList();
        } catch (final IOException e) {
            WalksyLib.getLogger().err("Could not read the mods folder to clear out old jars: " + e);
            return;
        }
        for (final Path jar : jars) {
            try {
                sweepOne(jar, modIds);
            } catch (final RuntimeException e) {
                WalksyLib.getLogger().err("Skipped " + jar.getFileName() + " while clearing out old jars: " + e);
            }
        }
    }

    private static void sweepOne(final Path jar, final Set<String> modIds) {
        final JsonObject metadata = readModJson(jar);
        if (metadata == null) {
            return;
        }
        final String id = string(metadata, "id");
        if (id == null || !modIds.contains(id) || !isWalksyLibMod(metadata, id)) {
            return;
        }
        final Version onDisk = ModUpdate.parse(string(metadata, "version"));
        if (onDisk == null) {
            return;
        }
        final ModContainer container = FabricLoader.getInstance().getModContainer(id).orElse(null);
        if (container == null) {
            return;
        }
        final Version loaded = container.getMetadata().getVersion();
        if (onDisk.compareTo(loaded) >= 0 || isLoadedFrom(container, jar)) {
            return;
        }
        try {
            Files.delete(jar);
            WalksyLib.getLogger().info("Removed " + jar.getFileName() + ", superseded by " + id + " " + loaded.getFriendlyString());
        } catch (final IOException e) {
            WalksyLib.getLogger().err("Could not remove the superseded " + jar.getFileName() + ": " + e);
        }
    }

    private static boolean isWalksyLibMod(final JsonObject metadata, final String id) {
        if (WalksyLibConstants.NAMESPACE.equals(id)) {
            return true;
        }
        final JsonElement depends = metadata.get("depends");
        return depends != null && depends.isJsonObject() && depends.getAsJsonObject().has(WalksyLibConstants.NAMESPACE);
    }

    private static boolean isLoadedFrom(final ModContainer container, final Path jar) {
        if (container.getOrigin().getKind() != ModOrigin.Kind.PATH) {
            return true;
        }
        for (final Path path : container.getOrigin().getPaths()) {
            try {
                if (Files.isSameFile(path, jar)) {
                    return true;
                }
            } catch (final IOException e) {
                if (path.toAbsolutePath().normalize().equals(jar.toAbsolutePath().normalize())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final Pattern NEOFORGE_MOD = Pattern.compile("\\[\\[mods\\]\\][\\s\\S]*?modId\\s*=\\s*\"([^\"]+)\"[\\s\\S]*?version\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern NEOFORGE_LIBRARY_DEPENDENCY = Pattern.compile("\\[\\[dependencies\\.[^\\]]+\\]\\]\\s*modId\\s*=\\s*\"" + WalksyLibConstants.NAMESPACE + "\"");

    private static @Nullable JsonObject readModJson(final Path jar) {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            final ZipEntry entry = zip.getEntry(WLLoader.isNeoForge() ? "META-INF/neoforge.mods.toml" : "fabric.mod.json");
            if (entry == null) {
                return null;
            }
            try (InputStream in = zip.getInputStream(entry)) {
                final String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                if (WLLoader.isNeoForge()) {
                    return neoForgeSummary(text);
                }
                final JsonElement root = JsonParser.parseString(text);
                return root.isJsonObject() ? root.getAsJsonObject() : null;
            }
        } catch (final IOException | RuntimeException e) {
            return null;
        }
    }

    private static @Nullable JsonObject neoForgeSummary(final String toml) {
        final Matcher mod = NEOFORGE_MOD.matcher(toml);
        if (!mod.find()) {
            return null;
        }
        final JsonObject summary = new JsonObject();
        summary.addProperty("id", mod.group(1));
        summary.addProperty("version", mod.group(2));
        if (NEOFORGE_LIBRARY_DEPENDENCY.matcher(toml).find()) {
            final JsonObject depends = new JsonObject();
            depends.addProperty(WalksyLibConstants.NAMESPACE, "*");
            summary.add("depends", depends);
        }
        return summary;
    }

    private static @Nullable String string(final JsonObject json, final String key) {
        final JsonElement element = json.get(key);
        return element == null || !element.isJsonPrimitive() ? null : element.getAsString();
    }

    private static boolean isJar(final Path path) {
        return Files.isRegularFile(path) && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar");
    }
}
