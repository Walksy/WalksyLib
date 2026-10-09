package main.walksy.lib.core.mods.remote;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.WalksyLibConstants;
import main.walksy.lib.core.mods.Mod;
import net.fabricmc.loader.api.ModContainer;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
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
    private final AtomicBoolean running = new AtomicBoolean(false);
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
        return Set.copyOf(this.registered.keySet());
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

    public void checkAll(final Consumer<ModUpdate> onOutdated) {
        if (this.registered.isEmpty() || !this.running.compareAndSet(false, true)) {
            return;
        }
        final List<ModUpdate> pending = new ArrayList<>(this.registered.values());
        final Thread thread = new Thread(() -> {
            try {
                for (final ModUpdate update : pending) {
                    update.check();
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
