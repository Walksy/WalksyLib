package main.walksy.lib.core.mods.remote;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.WalksyLibConstants;
import main.walksy.lib.core.platform.WLLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ModrinthApi {
    private static final String API = "https://api.modrinth.com/v2";
    public static final String AUTHOR = "Walksy";
    private static final String USER_AGENT = "Walksy/WalksyLib/" + libraryVersion() + " (+https://modrinth.com/user/" + AUTHOR + ")";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(TIMEOUT)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private ModrinthApi() {
    }

    private static String libraryVersion() {
        return FabricLoader.getInstance()
            .getModContainer(WalksyLibConstants.NAMESPACE)
            .map(c -> c.getMetadata().getVersion().getFriendlyString())
            .orElse("dev");
    }

    public static String gameVersion() {
        return SharedConstants.getCurrentVersion().name();
    }

    static List<JsonObject> authorProjects() throws IOException, InterruptedException {
        final JsonElement root = get(API + "/user/" + AUTHOR + "/projects");
        final List<JsonObject> projects = new ArrayList<>();
        if (root != null && root.isJsonArray()) {
            for (final JsonElement element : root.getAsJsonArray()) {
                if (element.isJsonObject()) {
                    projects.add(element.getAsJsonObject());
                }
            }
        }
        return projects;
    }

    static List<JsonObject> projectVersions(final String projectId) throws IOException, InterruptedException {
        final JsonElement root = get(API + "/project/" + projectId + "/version");
        final List<JsonObject> versions = new ArrayList<>();
        if (root != null && root.isJsonArray()) {
            for (final JsonElement element : root.getAsJsonArray()) {
                if (element.isJsonObject()) {
                    versions.add(element.getAsJsonObject());
                }
            }
        }
        return versions;
    }

    static @Nullable JsonObject version(final String versionId) throws IOException, InterruptedException {
        final JsonElement root = get(API + "/version/" + versionId);
        return root != null && root.isJsonObject() ? root.getAsJsonObject() : null;
    }

    static byte[] bytes(final String url) throws IOException, InterruptedException {
        final HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .header("User-Agent", USER_AGENT)
            .timeout(TIMEOUT)
            .GET()
            .build();
        final HttpResponse<byte[]> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("HTTP " + response.statusCode() + " for " + url);
        }
        return response.body();
    }

    private static @Nullable JsonElement get(final String url) throws IOException, InterruptedException {
        final HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .timeout(TIMEOUT)
            .GET()
            .build();
        final HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("HTTP " + response.statusCode() + " for " + url);
        }
        try {
            return JsonParser.parseString(response.body());
        } catch (final RuntimeException e) {
            WalksyLib.getLogger().err("Modrinth returned unparseable JSON for " + url + ": " + e.getMessage());
            return null;
        }
    }

    static String string(final JsonObject object, final String key, final String fallback) {
        final JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsString();
    }

    static @Nullable String stringOrNull(final JsonObject object, final String key) {
        final JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? null : element.getAsString();
    }

    static List<String> strings(final JsonObject object, final String key) {
        final List<String> values = new ArrayList<>();
        final JsonElement element = object.get(key);
        if (element != null && element.isJsonArray()) {
            for (final JsonElement item : element.getAsJsonArray()) {
                if (item != null && item.isJsonPrimitive()) {
                    values.add(item.getAsString());
                }
            }
        }
        return values;
    }

    static boolean isForThisClient(final JsonObject version, final String gameVersion) {
        return strings(version, "game_versions").contains(gameVersion) && strings(version, "loaders").stream().anyMatch(WLLoader.modrinthLoader()::equalsIgnoreCase);
    }

    static @Nullable JsonObject primaryFile(final JsonObject version) {
        JsonObject first = null;
        for (final JsonElement element : array(version, "files")) {
            if (!element.isJsonObject()) {
                continue;
            }
            final JsonObject file = element.getAsJsonObject();
            if (first == null) {
                first = file;
            }
            final JsonElement primary = file.get("primary");
            if (primary != null && !primary.isJsonNull() && primary.getAsBoolean()) {
                return file;
            }
        }
        return first;
    }

    static @Nullable String sha1(final JsonObject file) {
        final JsonElement hashes = file.get("hashes");
        return hashes != null && hashes.isJsonObject() ? stringOrNull(hashes.getAsJsonObject(), "sha1") : null;
    }

    static JsonArray array(final JsonObject object, final String key) {
        final JsonElement element = object.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : new JsonArray();
    }

    static String normalize(final String text) {
        final StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            final char c = Character.toLowerCase(text.charAt(i));
            if (Character.isLetterOrDigit(c)) {
                out.append(c);
            }
        }
        return out.toString().toLowerCase(Locale.ROOT);
    }
}
