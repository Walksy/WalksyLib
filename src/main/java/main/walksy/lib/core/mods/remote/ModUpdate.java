package main.walksy.lib.core.mods.remote;

import main.walksy.lib.core.WalksyLib;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ModUpdate {
    private static final int CHANGELOG_LIMIT = 120;
    private static final long INSTALL_VERIFY_TIMEOUT_MS = 10_000L;
    private static final long INSTALL_VERIFY_POLL_MS = 200L;
    private final String modId;
    private final String slug;
    private final String pageUrl;
    private volatile State state = State.IDLE;
    private volatile Install install = Install.NONE;
    private volatile String error = "";
    private volatile String installError = "";
    private volatile String latestVersion = "";
    private volatile String changelog = "";
    private volatile @Nullable String downloadUrl;
    private volatile @Nullable String downloadSha1;
    private volatile @Nullable String downloadFileName;

    public enum State {
        IDLE,
        CHECKING,

        UP_TO_DATE,
        OUTDATED,
        FAILED
    }

    public enum Install {
        NONE,
        DOWNLOADING,
        DONE,
        FAILED
    }

    public ModUpdate(final String modId, final String slug, final String pageUrl) {
        this.modId = modId;
        this.slug = slug;
        this.pageUrl = pageUrl;
    }

    public String modId() {
        return this.modId;
    }

    public String pageUrl() {
        return this.pageUrl;
    }

    public State state() {
        return this.state;
    }

    public boolean isOutdated() {
        return this.state == State.OUTDATED;
    }

    public Install installState() {
        return this.install;
    }

    public String latestVersion() {
        return this.latestVersion;
    }

    public String currentVersion() {
        return installedVersionString(this.modId);
    }

    public String changelog() {
        return this.changelog;
    }

    public String error() {
        return this.error;
    }

    public String installError() {
        return this.installError;
    }

    public String displayName() {
        return FabricLoader.getInstance()
            .getModContainer(this.modId)
            .map(container -> container.getMetadata().getName())
            .orElse(this.modId);
    }

    void check() {
        if (this.state == State.CHECKING) {
            return;
        }
        this.state = State.CHECKING;
        this.error = "";
        try {
            this.state = this.resolve();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.state = State.FAILED;
            this.error = "Interrupted";
        } catch (final Exception e) {
            WalksyLib.getLogger().err("Could not check " + this.modId + " for updates: " + e);
            this.state = State.FAILED;
            this.error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        }
    }

    void clearCheck() {
        if (this.state == State.CHECKING || this.install != Install.NONE) {
            return;
        }
        this.state = State.IDLE;
        this.error = "";
    }

    private State resolve() throws IOException, InterruptedException {
        final Version installed = parse(installedVersionString(this.modId));
        if (installed == null) {
            this.error = "This build's version number is not comparable";
            return State.FAILED;
        }
        final String gameVersion = ModrinthApi.gameVersion();
        final List<JsonObject> candidates = new ArrayList<>();
        for (final JsonObject version : ModrinthApi.projectVersions(this.slug)) {
            if (ModrinthApi.isForThisClient(version, gameVersion)) {
                candidates.add(version);
            }
        }
        JsonObject newest = null;
        Version newestVersion = null;
        for (final JsonObject candidate : candidates) {
            final Version parsed = parse(ModrinthApi.string(candidate, "version_number", ""));
            if (parsed != null && (newestVersion == null || parsed.compareTo(newestVersion) > 0)) {
                newest = candidate;
                newestVersion = parsed;
            }
        }
        if (newest == null || newestVersion.compareTo(installed) <= 0) {
            return State.UP_TO_DATE;
        }
        this.latestVersion = ModrinthApi.string(newest, "version_number", "");
        this.changelog = summarise(ModrinthApi.string(newest, "changelog", ""));
        final JsonObject file = ModrinthApi.primaryFile(newest);
        if (file != null) {
            this.downloadUrl = ModrinthApi.stringOrNull(file, "url");
            this.downloadFileName = ModrinthApi.string(file, "filename", this.slug + ".jar");
            this.downloadSha1 = ModrinthApi.sha1(file);
        }
        return State.OUTDATED;
    }

    private static String summarise(final String markdown) {
        for (final String line : markdown.split("\\R")) {
            final String trimmed = line.replaceAll("^[#\\-*>\\s]+", "").trim();
            if (!trimmed.isEmpty()) {
                return trimmed.length() <= CHANGELOG_LIMIT ? trimmed : trimmed.substring(0, CHANGELOG_LIMIT - 3) + "...";
            }
        }
        return "";
    }

    public static @Nullable Version parse(final String raw) {
        final String cleaned = raw.startsWith("v") || raw.startsWith("V") ? raw.substring(1) : raw;
        if (cleaned.isBlank()) {
            return null;
        }
        try {
            return Version.parse(cleaned);
        } catch (final VersionParsingException e) {
            return null;
        }
    }

    private static String installedVersionString(final String modId) {
        return FabricLoader.getInstance()
            .getModContainer(modId)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("");
    }

    public @Nullable String blockedReason() {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            return "This is a development run, so the loaded mod does not come from your mods folder.";
        }
        if (this.downloadUrl == null) {
            return "Modrinth did not publish a downloadable file for that version.";
        }
        return null;
    }

    public void installLatest() {
        if (this.install == Install.DOWNLOADING || this.state != State.OUTDATED || this.blockedReason() != null) {
            return;
        }
        this.install = Install.DOWNLOADING;
        this.installError = "";
        final Thread thread = new Thread(() -> {
            try {
                this.swapIn();
                this.install = Install.DONE;
            } catch (final Exception e) {
                WalksyLib.getLogger().err("Failed to install the " + this.modId + " update: " + e);
                this.installError = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                this.install = Install.FAILED;
            }
            WalksyLib.getInstance().getModUpdates().installFinished(this);
        }, "WalksyLib update install " + this.modId);
        thread.setDaemon(true);
        thread.start();
    }

    private void swapIn() throws IOException, InterruptedException {
        final String url = this.downloadUrl;
        if (url == null) {
            throw new IOException("No download for that version");
        }
        final byte[] bytes = ModrinthDownload.fetchVerified(url, this.downloadSha1);
        final String fileName = ModrinthDownload.safeFileName(this.downloadFileName == null ? this.slug + "-" + this.latestVersion + ".jar" : this.downloadFileName);
        final Path target = ModrinthDownload.modsDirectory().resolve(fileName);
        final boolean ours = !Files.isRegularFile(target);
        if (ours) {
            ModrinthDownload.writeToMods(bytes, fileName);
        }
        awaitPresent(target, ours ? bytes.length : -1L);
    }

    private static void awaitPresent(final Path target, final long expectedSize) throws IOException, InterruptedException {
        final long deadline = System.currentTimeMillis() + INSTALL_VERIFY_TIMEOUT_MS;
        while (true) {
            if (Files.isRegularFile(target) && (expectedSize < 0L || Files.size(target) == expectedSize)) {
                return;
            }
            if (System.currentTimeMillis() >= deadline) {
                throw new IOException("The download did not stay in your mods folder - " + target.getFileName() + " is not there. Antivirus removing new jars is the" + " usual cause.");
            }
            Thread.sleep(INSTALL_VERIFY_POLL_MS);
        }
    }
}
