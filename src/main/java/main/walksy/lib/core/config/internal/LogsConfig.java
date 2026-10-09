package main.walksy.lib.core.config.internal;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.api.config.Config;
import main.walksy.lib.api.config.option.AvailabilityMode;
import main.walksy.lib.api.config.option.Category;
import main.walksy.lib.api.config.option.OptionDescription;
import main.walksy.lib.api.config.option.OptionGroup;
import main.walksy.lib.api.config.option.Options;
import main.walksy.lib.api.config.PathUtils;
import main.walksy.lib.core.utils.log.InternalLog;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public final class LogsConfig {
    public static final String CATEGORY = "WalksyLib";

    public static final String PAGE = "Logs";
    private static final String MODS_CATEGORY = "Mods";
    private Config config;
    private int builtRevision = -1;

    public Config config() {
        final int revision = WalksyLib.getLogger().revision();
        if (this.config != null && this.builtRevision == revision) {
            return this.config;
        }
        this.builtRevision = revision;
        this.config = this.build(WalksyLib.getLogger().changes());
        return this.config;
    }

    private Config build(final List<InternalLog> changes) {
        final List<InternalLog> mods = new ArrayList<>();
        final List<InternalLog> library = new ArrayList<>();
        for (final InternalLog change : changes) {
            (change.library() ? library : mods).add(change);
        }
        return Config.builder()
            .path(PathUtils.ofLibraryDir("walksylib"))
            .sharedFile()
            .category(this.category(MODS_CATEGORY, mods, "Anything you change in a mod's options is listed here"))
            .category(this.category(CATEGORY, library, "Changes to WalksyLib's own pages"))
            .build();
    }

    private Category category(final String name, final List<InternalLog> changes, final String empty) {
        final OptionGroup.Builder group = OptionGroup.builder(this.heading(changes.size()));
        if (changes.isEmpty()) {
            group.option(Options.ofButton("Nothing changed yet", () -> {
            }).availability(() -> false, "", AvailabilityMode.GRAY_OUT).description(OptionDescription.ofText(() -> empty)).build());
        } else {
            for (final InternalLog change : changes) {
                group.option(Options.ofButton(change.summary(), () -> this.copy(change))
                    .description(OptionDescription.ofText(change::detail))
                    .build());
            }
        }
        return Category.builder(name).library().group(group.build()).build();
    }

    private String heading(final int count) {
        if (count == 0) {
            return "This Session";
        }
        return count == 1 ? "1 Change This Session" : count + " Changes This Session";
    }

    private void copy(final InternalLog change) {
        final Minecraft client = Minecraft.getInstance();
        if (client != null && client.keyboardHandler != null) {
            client.keyboardHandler.setClipboard(change.detail() + "  " + change.summary());
        }
    }
}
