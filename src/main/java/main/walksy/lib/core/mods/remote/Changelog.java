package main.walksy.lib.core.mods.remote;

import com.google.gson.JsonObject;
import main.walksy.lib.core.WalksyLib;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class Changelog {
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger revision = new AtomicInteger();
    private volatile State state = State.IDLE;
    private volatile List<Release> releases = List.of();
    private volatile String error = "";

    public enum State {
        IDLE,
        LOADING,
        LOADED,
        FAILED
    }

    public record Release(String id, String versionNumber, String type, Instant published, List<String> gameVersions, String changelog) {
    }

    public State state() {
        return this.state;
    }

    public List<Release> releases() {
        return this.releases;
    }

    public String error() {
        return this.error;
    }

    public int revision() {
        return this.revision.get();
    }

    public String versionUrl(final Release release) {
        return ModUpdates.LIBRARY_PAGE_URL + "/version/" + release.id();
    }

    public String changelogUrl() {
        return ModUpdates.LIBRARY_PAGE_URL + "/changelog";
    }

    public void ensureLoaded() {
        if (this.state == State.IDLE) {
            this.refresh();
        }
    }

    public void refresh() {
        if (!this.running.compareAndSet(false, true)) {
            return;
        }
        this.state = State.LOADING;
        this.error = "";
        this.revision.incrementAndGet();
        final Thread thread = new Thread(() -> {
            try {
                this.releases = this.fetch();
                this.state = State.LOADED;
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                this.error = "Interrupted";
                this.state = State.FAILED;
            } catch (final Exception e) {
                WalksyLib.getLogger().err("Could not load the WalksyLib changelog from Modrinth: " + e);
                this.error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                this.state = State.FAILED;
            } finally {
                this.revision.incrementAndGet();
                this.running.set(false);
            }
        }, "WalksyLib changelog");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    private List<Release> fetch() throws IOException, InterruptedException {
        final String slug = ModUpdates.slugOf(ModUpdates.LIBRARY_PAGE_URL);
        final List<Release> all = new ArrayList<>();
        for (final JsonObject version : ModrinthApi.projectVersions(slug)) {
            final String id = ModrinthApi.string(version, "id", "");
            if (id.isEmpty()) {
                continue;
            }
            all.add(new Release(id, ModrinthApi.string(version, "version_number", ""), ModrinthApi.string(version, "version_type", "release"), this.published(ModrinthApi.string(version, "date_published", "")), List.copyOf(ModrinthApi.strings(version, "game_versions")), ModrinthApi.string(version, "changelog", "")));
        }
        all.sort(Comparator.comparing(Release::published).reversed());
        return List.copyOf(all);
    }

    private Instant published(final String raw) {
        try {
            return Instant.parse(raw);
        } catch (final DateTimeParseException e) {
            return Instant.EPOCH;
        }
    }
}
