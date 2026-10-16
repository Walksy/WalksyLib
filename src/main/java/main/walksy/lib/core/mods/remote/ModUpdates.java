package main.walksy.lib.core.mods.remote;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.WalksyLibConstants;
import com.google.gson.JsonObject;
import main.walksy.lib.core.mods.Mod;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.fabricmc.loader.api.metadata.Person;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class ModUpdates {
    public static final String LIBRARY_PAGE_URL = "https://modrinth.com/mod/" + WalksyLibConstants.NAMESPACE;
    private final Map<String, ModUpdate> registered = new ConcurrentHashMap<>();
    private final Set<String> candidates = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile boolean enabled = true;
    private volatile @Nullable Consumer<ModUpdate> installListener;

    public void register(final String modId, final @Nullable String modrinthPageUrl) {
        if (modId == null || modrinthPageUrl == null || modrinthPageUrl.isBlank()) {
            return;
        }
        final String slug = slugOf(modrinthPageUrl);
        if (slug == null) {
            WalksyLib.getLogger().err("\"" + modrinthPageUrl + "\" is not a Modrinth project page, so " + modId + " will not be checked for updates. It should look like " + "https://modrinth.com/mod/your-mod.");
            return;
        }
        this.registered.putIfAbsent(modId, new ModUpdate(modId, slug, modrinthPageUrl));
    }

    public void registerIfPublished(final String modId) {
        if (modId != null && !this.registered.containsKey(modId)) {
            this.candidates.add(modId);
        }
    }

    private void resolveCandidates() {
        final List<String> pending = this.candidates.stream().filter(modId -> !this.registered.containsKey(modId)).toList();
        if (pending.isEmpty()) {
            return;
        }
        final Map<String, String> hashes = new HashMap<>();
        for (final String modId : pending) {
            final String sha1 = jarHash(modId);
            if (sha1 != null) {
                hashes.put(modId, sha1);
            }
        }
        final List<JsonObject> projects;
        try {
            projects = ModrinthApi.authorProjects();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        } catch (final IOException e) {
            WalksyLib.getLogger().err("Could not look up " + ModrinthApi.AUTHOR + "'s Modrinth projects for update checks: " + e);
            return;
        }
        Map<String, JsonObject> versions = Map.of();
        try {
            versions = ModrinthApi.versionsOfFiles(hashes.values());
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        } catch (final IOException e) {
            WalksyLib.getLogger().err("Could not look up installed mod files on Modrinth, falling back to matching by name: " + e);
        }
        for (final String modId : pending) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            try {
                final JsonObject version = hashes.containsKey(modId) ? versions.get(hashes.get(modId)) : null;
                final String slug = this.publishedSlug(modId, version == null ? null : ModrinthApi.stringOrNull(version, "project_id"), projects);
                if (slug != null) {
                    this.register(modId, "https://modrinth.com/mod/" + slug);
                }
            } catch (final RuntimeException e) {
                WalksyLib.getLogger().err("Could not find the Modrinth page for " + modId + ", so it will not be checked for updates: " + e);
            }
        }
    }

    private @Nullable String publishedSlug(final String modId, final @Nullable String projectId, final List<JsonObject> projects) {
        final ModContainer container = FabricLoader.getInstance().getModContainer(modId).orElse(null);
        if (container == null) {
            return null;
        }
        if (projectId != null) {
            for (final JsonObject project : projects) {
                if (projectId.equals(ModrinthApi.string(project, "id", ""))) {
                    return ModrinthApi.string(project, "slug", projectId);
                }
            }
            return projectId;
        }
        if (!isByAuthor(container)) {
            return null;
        }
        final Set<String> names = new HashSet<>(List.of(ModrinthApi.normalize(modId), ModrinthApi.normalize(container.getMetadata().getName())));
        for (final JsonObject project : projects) {
            final String slug = ModrinthApi.string(project, "slug", "");
            if (!slug.isEmpty() && "mod".equals(ModrinthApi.string(project, "project_type", "")) && (names.contains(ModrinthApi.normalize(slug)) || names.contains(ModrinthApi.normalize(ModrinthApi.string(project, "title", ""))))) {
                return slug;
            }
        }
        return null;
    }

    private static @Nullable String jarHash(final String modId) {
        final ModContainer container = FabricLoader.getInstance().getModContainer(modId).orElse(null);
        if (container == null || container.getOrigin().getKind() != ModOrigin.Kind.PATH) {
            return null;
        }
        for (final Path path : container.getOrigin().getPaths()) {
            if (!Files.isRegularFile(path)) {
                continue;
            }
            try {
                return ModrinthDownload.sha1(Files.readAllBytes(path));
            } catch (final IOException e) {
                WalksyLib.getLogger().err("Could not read " + path.getFileName() + " to look it up on Modrinth: " + e);
            }
        }
        return null;
    }

    private static boolean isByAuthor(final ModContainer container) {
        for (final Person author : container.getMetadata().getAuthors()) {
            if (ModrinthApi.AUTHOR.equalsIgnoreCase(author.getName())) {
                return true;
            }
        }
        return false;
    }

    static @Nullable String slugOf(final String url) {
        final String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (!trimmed.contains("/") && !trimmed.contains(":")) {
            return trimmed;
        }
        final URI uri;
        try {
            uri = URI.create(trimmed);
        } catch (final RuntimeException e) {
            return null;
        }
        final String host = uri.getHost() == null ? null : uri.getHost().toLowerCase(Locale.ROOT);
        if (host == null || !(host.equals("modrinth.com") || host.endsWith(".modrinth.com"))) {
            return null;
        }
        final String path = uri.getPath() == null ? "" : uri.getPath();
        final List<String> segments = new ArrayList<>();
        for (final String segment : path.split("/")) {
            if (!segment.isBlank()) {
                segments.add(segment);
            }
        }
        return segments.size() >= 2 ? segments.get(1) : null;
    }

    public @Nullable ModUpdate of(final String modId) {
        return modId == null ? null : this.registered.get(modId);
    }

    public @Nullable ModUpdate of(final @Nullable Mod mod) {
        if (mod == null || mod.isSuggestion()) {
            return null;
        }
        final ModContainer container = mod.getContainer();
        return container == null ? null : this.of(container.getMetadata().getId());
    }

    public ModUpdate library() {
        this.register(WalksyLibConstants.NAMESPACE, LIBRARY_PAGE_URL);
        return this.registered.get(WalksyLibConstants.NAMESPACE);
    }

    public Set<String> trackedIds() {
        final Set<String> ids = new HashSet<>(this.registered.keySet());
        ids.addAll(this.candidates);
        return Set.copyOf(ids);
    }

    public boolean anyOutdated() {
        for (final ModUpdate update : this.registered.values()) {
            if (update.isOutdated()) {
                return true;
            }
        }
        return false;
    }

    public void onInstallFinished(final @Nullable Consumer<ModUpdate> listener) {
        this.installListener = listener;
    }

    void installFinished(final ModUpdate update) {
        final Consumer<ModUpdate> listener = this.installListener;
        if (listener != null) {
            listener.accept(update);
        }
    }

    public void stopChecking() {
        this.enabled = false;
        for (final ModUpdate update : this.registered.values()) {
            update.clearCheck();
        }
    }

    public void checkAll(final Consumer<ModUpdate> onOutdated) {
        this.enabled = true;
        if ((this.registered.isEmpty() && this.candidates.isEmpty()) || !this.running.compareAndSet(false, true)) {
            return;
        }
        final Thread thread = new Thread(() -> {
            try {
                this.resolveCandidates();
                for (final ModUpdate update : List.copyOf(this.registered.values())) {
                    if (!this.enabled) {
                        return;
                    }
                    update.check();
                    if (!this.enabled) {
                        update.clearCheck();
                        return;
                    }
                    if (update.isOutdated() && onOutdated != null) {
                        onOutdated.accept(update);
                    }
                }
            } finally {
                this.running.set(false);
            }
        }, "WalksyLib update check");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }
}
