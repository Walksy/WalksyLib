package main.walksy.lib.core.config.internal;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.WalksyLibConstants;
import main.walksy.lib.api.config.Config;
import main.walksy.lib.api.config.option.AvailabilityMode;
import main.walksy.lib.api.config.option.Category;
import main.walksy.lib.api.config.option.OptionDescription;
import main.walksy.lib.api.config.option.OptionGroup;
import main.walksy.lib.api.config.option.Options;
import main.walksy.lib.core.mods.remote.Changelog;
import main.walksy.lib.api.config.PathUtils;
import net.fabricmc.loader.api.FabricLoader;
import main.walksy.lib.core.platform.WLClient;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ChangelogConfig {
    public static final String PAGE = "Changelog";
    private static final String GROUP = "Recent Updates";
    private static final int MAX_CHANGELOG_LINES = 12;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH).withZone(ZoneId.systemDefault());
    private Config config;
    private int builtRevision = -1;

    public record Note(boolean heading, boolean bullet, String text) {}

    public List<Note> notes(final String markdown) {
        final List<Note> notes = new ArrayList<>();
        for (final String raw : markdown.split("\\R")) {
            String line = raw.strip();
            if (line.matches("[-*_\\s]{3,}")) {
                continue;
            }
            final boolean heading = line.matches("#{1,6}\\s+.*");
            line = line.replaceFirst("^#{1,6}\\s*", "").replaceFirst("^>\\s*", "");
            boolean bullet = false;
            if (line.matches("[-*+]\\s+.*")) {
                line = line.replaceFirst("^[-*+]\\s+", "");
                bullet = true;
            }
            line = line.replaceAll("!\\[[^\\]]*]\\([^)]*\\)", "")
                .replaceAll("\\[([^\\]]*)]\\([^)]*\\)", "$1")
                .replaceAll("<[^>]+>", "")
                .replaceAll("\\*\\*|__|~~|`", "");
            line = this.ascii(line).strip();
            if (!line.isEmpty()) {
                notes.add(new Note(heading && !bullet, bullet, line));
            }
        }
        return notes;
    }

    public String formatDate(final Changelog.Release release) {
        return DATE.format(release.published());
    }

    public Config config() {
        WalksyLib.getInstance().getChangelog().ensureLoaded();
        final int revision = WalksyLib.getInstance().getChangelog().revision();
        if (this.config != null && this.builtRevision == revision) {
            return this.config;
        }
        this.builtRevision = revision;
        this.config = this.build();
        return this.config;
    }

    public boolean isStale() {
        return this.config != null && this.builtRevision != WalksyLib.getInstance().getChangelog().revision();
    }

    private Config build() {
        final OptionGroup.Builder group = OptionGroup.builder(GROUP);
        switch (WalksyLib.getInstance().getChangelog().state()) {
            case IDLE, LOADING -> group.option(Options.ofButton("Loading...", () -> {
            }).availability(() -> false, "", AvailabilityMode.GRAY_OUT).description(OptionDescription.ofText(() -> "Fetching WalksyLib's recent updates from Modrinth.")).build());
            case FAILED -> group.option(Options.ofButton("Try Again", WalksyLib.getInstance().getChangelog()::refresh)
                .description(OptionDescription.ofText(() -> "Could not reach Modrinth: " + WalksyLib.getInstance().getChangelog().error()))
                .build());
            case LOADED -> this.addReleases(group, WalksyLib.getInstance().getChangelog().releases());
        }
        return Config.builder()
            .path(PathUtils.ofLibraryDir("walksylib"))
            .sharedFile()
            .category(Category.builder(PAGE).library().group(group.build()).build())
            .build();
    }

    private void addReleases(final OptionGroup.Builder group, final List<Changelog.Release> releases) {
        if (releases.isEmpty()) {
            group.option(Options.ofButton("No updates yet", () -> {
            }).availability(() -> false, "", AvailabilityMode.GRAY_OUT).description(OptionDescription.ofText(() -> "WalksyLib has not published anything on Modrinth.")).build());
            return;
        }
        final String installed = this.installedVersion();
        for (final Changelog.Release release : releases) {
            final String label = release.versionNumber().equals(installed) ? release.versionNumber() + "  (Installed)" : release.versionNumber();
            final String description = this.describe(release);
            group.option(Options.ofButton(label, () -> WLClient.openUri(WalksyLib.getInstance().getChangelog().versionUrl(release)))
                .description(OptionDescription.ofText(() -> description))
                .build());
        }
        group.option(Options.ofButton("View Full Changelog", () -> WLClient.openUri(WalksyLib.getInstance().getChangelog().changelogUrl()))
            .build());
    }

    private String describe(final Changelog.Release release) {
        final StringBuilder text = new StringBuilder(this.typeName(release.type()))
            .append(", ")
            .append(DATE.format(release.published()));
        if (!release.gameVersions().isEmpty()) {
            text.append(", for Minecraft ").append(String.join(", ", release.gameVersions()));
        }
        final List<String> lines = this.plainText(release.changelog());
        if (lines.isEmpty()) {
            return text.append("\nNo changelog was written for this version.").toString();
        }
        final int shown = Math.min(lines.size(), MAX_CHANGELOG_LINES);
        for (int i = 0; i < shown; i++) {
            text.append('\n').append(lines.get(i));
        }
        if (lines.size() > shown) {
            text.append("\nClick to read the rest on Modrinth.");
        }
        return text.toString();
    }

    public String typeName(final String type) {
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "beta" -> "Beta";
            case "alpha" -> "Alpha";
            default -> "Release";
        };
    }

    private List<String> plainText(final String markdown) {
        final List<String> lines = new ArrayList<>();
        for (final String raw : markdown.split("\\R")) {
            String line = raw.strip();
            if (line.matches("[-*_\\s]{3,}")) {
                continue;
            }
            boolean bullet = false;
            line = line.replaceFirst("^#{1,6}\\s*", "").replaceFirst("^>\\s*", "");
            if (line.matches("[-*+]\\s+.*")) {
                line = line.replaceFirst("^[-*+]\\s+", "");
                bullet = true;
            }
            line = line.replaceAll("!\\[[^\\]]*]\\([^)]*\\)", "")
                .replaceAll("\\[([^\\]]*)]\\([^)]*\\)", "$1")
                .replaceAll("<[^>]+>", "")
                .replaceAll("\\*\\*|__|~~|`", "");
            line = this.ascii(line).strip();
            if (!line.isEmpty()) {
                lines.add(bullet ? "- " + line : line);
            }
        }
        return lines;
    }

    private String ascii(final String text) {
        final StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            final int c = text.charAt(i);
            switch (c) {
                case 0x2018, 0x2019 -> out.append("'");
                case 0x201C, 0x201D -> out.append('"');
                case 0x2013, 0x2014, 0x2022 -> out.append('-');
                case 0x2026 -> out.append("...");
                case 0x09, 0xA0 -> out.append(' ');
                default -> {
                    if (c >= 32 && c < 127) {
                        out.append((char) c);
                    }
                }
            }
        }
        return out.toString();
    }

    public String installedVersion() {
        return FabricLoader.getInstance()
            .getModContainer(WalksyLibConstants.NAMESPACE)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("");
    }
}
