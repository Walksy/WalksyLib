package main.walksy.lib.core.config.internal;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.api.config.Config;
import main.walksy.lib.api.config.option.AvailabilityMode;
import main.walksy.lib.api.config.option.Category;
import main.walksy.lib.api.config.option.OptionDescription;
import main.walksy.lib.api.config.option.OptionGroup;
import main.walksy.lib.api.config.option.Options;
import main.walksy.lib.core.mods.remote.ModUpdate;
import main.walksy.lib.api.config.PathUtils;
import main.walksy.lib.core.platform.WLClient;

public final class UpdateConfig {
    public static final String CATEGORY = "Update";
    private Config config;

    public Config config() {
        if (this.config == null) {
            final ModUpdate update = WalksyLib.getInstance().getModUpdates().library();
            final String blocked = update.blockedReason();
            this.config = Config.builder()
                .path(PathUtils.ofLibraryDir("walksylib"))
                .sharedFile()
                .category(Category.builder(CATEGORY)
                    .library()
                    .group(OptionGroup.builder("WalksyLib " + update.latestVersion() + " is out")
                        .option(Options.ofButton("Install " + update.latestVersion(), update::installLatest)
                            .availability(() -> this.canInstall(update), blocked == null ? "Already installed" : blocked, AvailabilityMode.GRAY_OUT)
                            .description(OptionDescription.ofText(() -> this.installDescription(update)))
                            .build())
                        .option(Options.ofButton("View On Modrinth", () -> WLClient.openUri(update.pageUrl()))
                            .description(OptionDescription.ofText(() -> "Opens " + update.pageUrl() + " in your browser, to read the" + " full changelog or download it yourself."))
                            .build())
                        .build())
                    .build())
                .build();
        }
        return this.config;
    }

    boolean canInstall(final ModUpdate update) {
        return update.blockedReason() == null && update.installState() != ModUpdate.Install.DOWNLOADING && update.installState() != ModUpdate.Install.DONE;
    }

    String statusText(final ModUpdate update) {
        return switch (update.installState()) {
            case DOWNLOADING -> "Downloading...";
            case DONE -> "";
            case FAILED -> "That did not work: " + update.installError();
            case NONE -> "";
        };
    }

    String installDescription(final ModUpdate update) {
        final String status = this.statusText(update);
        if (!status.isEmpty()) {
            return status;
        }
        final String blocked = update.blockedReason();
        if (blocked != null) {
            return blocked;
        }
        final StringBuilder text = new StringBuilder("You are running ").append(update.currentVersion()).append('.');
        if (!update.changelog().isEmpty()) {
            text.append(' ').append(update.changelog());
        }
        text.append(" Downloads it into your mods folder; it loads the next time you start the game.");
        return text.toString();
    }
}
