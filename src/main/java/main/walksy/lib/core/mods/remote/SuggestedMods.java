package main.walksy.lib.core.mods.remote;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.WalksyLibConstants;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SuggestedMods {
    private volatile State state = State.IDLE;
    private volatile String error = "";
    private volatile List<ModSuggestion> suggestions = List.of();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Map<String, Install> installState = new ConcurrentHashMap<>();
    private final Map<String, String> installError = new ConcurrentHashMap<>();
    private final Map<String, Identifier> icons = new ConcurrentHashMap<>();
    private final Set<String> iconsRequested = ConcurrentHashMap.newKeySet();

    public enum State {
        IDLE,
        LOADING,
        READY,
        FAILED
    }

    public enum Install {
        NONE,
        DOWNLOADING,

        INSTALLED,
        FAILED
    }

    public State state() {
        return this.state;
    }

    public String error() {
        return this.error;
    }

    public List<ModSuggestion> suggestions() {
        return this.suggestions;
    }

    public Install installState(final ModSuggestion suggestion) {
        return this.installState.getOrDefault(suggestion.projectId(), Install.NONE);
    }

    public String installError(final ModSuggestion suggestion) {
        return this.installError.getOrDefault(suggestion.projectId(), "");
    }

    public void ensureLoaded() {
        if (this.state == State.READY || this.state == State.LOADING) {
            return;
        }
        this.refresh();
    }

    public void refresh() {
        if (!this.running.compareAndSet(false, true)) {
            return;
        }
        this.state = State.LOADING;
        this.error = "";
        final Thread thread = new Thread(this::load, "WalksyLib Modrinth");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    private void load() {
        try {
            this.suggestions = this.resolve();
            this.state = State.READY;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.state = State.FAILED;
            this.error = "Interrupted";
        } catch (final Exception e) {
            WalksyLib.getLogger().err("Failed to load Modrinth suggestions: " + e);
            this.state = State.FAILED;
            this.error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        } finally {
            this.running.set(false);
        }
    }

    private List<ModSuggestion> resolve() throws IOException, InterruptedException {
        final List<JsonObject> projects = ModrinthApi.authorProjects();
        if (projects.isEmpty()) {
            return List.of();
        }
        final String libraryId = this.findLibraryProjectId(projects);
        if (libraryId == null) {
            throw new IOException("Could not find the WalksyLib project on Modrinth");
        }
        final Set<String> installed = this.installedNames();
        final String gameVersion = ModrinthApi.gameVersion();
        final String libraryVersion = this.installedLibraryVersion();
        final List<ModSuggestion> found = new ArrayList<>();
        for (final JsonObject project : projects) {
            final String id = ModrinthApi.string(project, "id", "");
            final String slug = ModrinthApi.string(project, "slug", id);
            final String title = ModrinthApi.string(project, "title", slug);
            if (id.isEmpty() || id.equals(libraryId)) {
                continue;
            }
            if (!"mod".equals(ModrinthApi.string(project, "project_type", ""))) {
                continue;
            }
            if (installed.contains(ModrinthApi.normalize(slug)) || installed.contains(ModrinthApi.normalize(title))) {
                continue;
            }
            final List<JsonObject> versions = ModrinthApi.projectVersions(id);
            if (versions.stream().noneMatch(v -> this.dependsOn(v, libraryId))) {
                continue;
            }
            final String summary = ModrinthApi.string(project, "description", "");
            final String iconUrl = ModrinthApi.stringOrNull(project, "icon_url");
            found.add(this.pick(id, slug, title, summary, iconUrl, versions, libraryId, libraryVersion, gameVersion));
        }
        found.sort((a, b) -> a.title().compareToIgnoreCase(b.title()));
        return List.copyOf(found);
    }

    private ModSuggestion pick(final String id, final String slug, final String title, final String summary, final @Nullable String iconUrl, final List<JsonObject> versions, final String libraryId, final String libraryVersion, final String gameVersion) throws IOException, InterruptedException {
        final List<JsonObject> forThisClient = new ArrayList<>();
        for (final JsonObject version : versions) {
            if (ModrinthApi.isForThisClient(version, gameVersion)) {
                forThisClient.add(version);
            }
        }
        if (forThisClient.isEmpty()) {
            return ModSuggestion.notUpdated(id, slug, title, summary, iconUrl, "This mod hasn't been updated for Minecraft " + gameVersion + " yet.");
        }
        forThisClient.sort((a, b) -> ModrinthApi.string(b, "date_published", "").compareTo(ModrinthApi.string(a, "date_published", "")));
        for (final JsonObject version : forThisClient) {
            final String pinned = this.pinnedLibraryVersion(version, libraryId);
            if (pinned != null && !pinned.equalsIgnoreCase(libraryVersion)) {
                continue;
            }
            final JsonObject file = ModrinthApi.primaryFile(version);
            if (file == null) {
                continue;
            }
            final String url = ModrinthApi.stringOrNull(file, "url");
            final String fileName = ModrinthApi.string(file, "filename", slug + ".jar");
            if (url == null) {
                continue;
            }
            return ModSuggestion.available(id, slug, title, summary, iconUrl, ModrinthApi.string(version, "version_number", "?"), fileName, url, ModrinthApi.sha1(file));
        }
        return ModSuggestion.notUpdated(id, slug, title, summary, iconUrl, "This mod hasn't been updated for WalksyLib " + libraryVersion + " yet.");
    }

    private @Nullable String pinnedLibraryVersion(final JsonObject version, final String libraryId) throws IOException, InterruptedException {
        for (final JsonElement element : ModrinthApi.array(version, "dependencies")) {
            if (!element.isJsonObject()) {
                continue;
            }
            final JsonObject dependency = element.getAsJsonObject();
            if (!libraryId.equals(ModrinthApi.stringOrNull(dependency, "project_id"))) {
                continue;
            }
            final String versionId = ModrinthApi.stringOrNull(dependency, "version_id");
            if (versionId == null) {
                return null;
            }
            final JsonObject pinned = ModrinthApi.version(versionId);
            return pinned == null ? null : ModrinthApi.string(pinned, "version_number", "");
        }
        return null;
    }

    private boolean dependsOn(final JsonObject version, final String projectId) {
        for (final JsonElement element : ModrinthApi.array(version, "dependencies")) {
            if (element.isJsonObject() && projectId.equals(ModrinthApi.stringOrNull(element.getAsJsonObject(), "project_id"))) {
                return true;
            }
        }
        return false;
    }

    private @Nullable String findLibraryProjectId(final List<JsonObject> projects) {
        final String wanted = ModrinthApi.normalize(WalksyLibConstants.NAMESPACE);
        for (final JsonObject project : projects) {
            final String slug = ModrinthApi.normalize(ModrinthApi.string(project, "slug", ""));
            final String title = ModrinthApi.normalize(ModrinthApi.string(project, "title", ""));
            if (wanted.equals(slug) || wanted.equals(title)) {
                return ModrinthApi.string(project, "id", null);
            }
        }
        return null;
    }

    private Set<String> installedNames() {
        final Set<String> names = new HashSet<>();
        for (final ModContainer container : FabricLoader.getInstance().getAllMods()) {
            names.add(ModrinthApi.normalize(container.getMetadata().getId()));
            names.add(ModrinthApi.normalize(container.getMetadata().getName()));
        }
        return names;
    }

    private String installedLibraryVersion() {
        return FabricLoader.getInstance()
            .getModContainer(WalksyLibConstants.NAMESPACE)
            .map(c -> c.getMetadata().getVersion().getFriendlyString())
            .orElse("");
    }

    public @Nullable Identifier icon(final ModSuggestion suggestion) {
        final Identifier ready = this.icons.get(suggestion.projectId());
        if (ready != null) {
            return ready;
        }
        final String url = suggestion.iconUrl();
        if (url == null || url.isEmpty() || !this.iconsRequested.add(suggestion.projectId())) {
            return null;
        }
        final Thread thread = new Thread(() -> this.fetchIcon(suggestion, url), "WalksyLib icon " + suggestion.slug());
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
        return null;
    }

    private void fetchIcon(final ModSuggestion suggestion, final String url) {
        NativeImage image = null;
        for (final String candidate : this.iconCandidates(url)) {
            try {
                image = this.decode(ModrinthApi.bytes(candidate));
            } catch (final Exception e) {
                continue;
            }
            if (image != null) {
                break;
            }
        }
        if (image == null) {
            WalksyLib.getLogger().err("No usable icon for " + suggestion.slug() + " (" + url + ")");
            return;
        }
        final NativeImage decoded = image;
        Minecraft.getInstance().execute(() -> {
            try {
                final Identifier id = Identifier.fromNamespaceAndPath(WalksyLibConstants.NAMESPACE, "modrinth/" + ModrinthApi.normalize(suggestion.slug()));
                Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, decoded));
                this.icons.put(suggestion.projectId(), id);
            } catch (final Exception e) {
                decoded.close();
                WalksyLib.getLogger().err("Failed to register icon for " + suggestion.slug() + ": " + e);
            }
        });
    }

    private List<String> iconCandidates(final String url) {
        final Matcher thumbnail = Pattern.compile("^(.*)_[0-9]+\\.webp$").matcher(url);
        if (!thumbnail.matches()) {
            return List.of(url);
        }
        final String base = thumbnail.group(1);
        return List.of(base + ".png", base + ".jpeg", base + ".jpg", base + ".gif", url);
    }

    private @Nullable NativeImage decode(final byte[] bytes) {
        if (this.isPng(bytes)) {
            try {
                return NativeImage.read(bytes);
            } catch (final IOException ignored) {
            }
        }
        try {
            final BufferedImage buffered = ImageIO.read(new ByteArrayInputStream(bytes));
            if (buffered == null) {
                return null;
            }
            final int width = buffered.getWidth();
            final int height = buffered.getHeight();
            final NativeImage image = new NativeImage(width, height, false);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    image.setPixel(x, y, buffered.getRGB(x, y));
                }
            }
            return image;
        } catch (final IOException | RuntimeException e) {
            return null;
        }
    }

    private boolean isPng(final byte[] bytes) {
        return bytes.length > 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G';
    }

    public void install(final ModSuggestion suggestion) {
        if (!suggestion.isAvailable() || this.installState(suggestion) == Install.DOWNLOADING) {
            return;
        }
        this.installState.put(suggestion.projectId(), Install.DOWNLOADING);
        this.installError.remove(suggestion.projectId());
        final Thread thread = new Thread(() -> {
            try {
                this.download(suggestion);
                this.installState.put(suggestion.projectId(), Install.INSTALLED);
            } catch (final Exception e) {
                WalksyLib.getLogger().err("Failed to install " + suggestion.slug() + ": " + e);
                this.installState.put(suggestion.projectId(), Install.FAILED);
                this.installError.put(suggestion.projectId(), e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            }
        }, "WalksyLib install " + suggestion.slug());
        thread.setDaemon(true);
        thread.start();
    }

    private void download(final ModSuggestion suggestion) throws Exception {
        final String url = suggestion.downloadUrl();
        if (url == null) {
            throw new IOException("No download for this version");
        }
        final byte[] bytes = ModrinthDownload.fetchVerified(url, suggestion.sha1());
        ModrinthDownload.writeToMods(bytes, suggestion.fileName() == null ? suggestion.slug() + ".jar" : suggestion.fileName());
    }

    public void reset() {
        this.state = State.IDLE;
        this.error = "";
        this.suggestions = List.of();
        this.installState.clear();
        this.installError.clear();
    }
}
