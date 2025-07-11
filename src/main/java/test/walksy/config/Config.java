package test.walksy.config;

import walksy.lib.core.config.ConfigClass;
import walksy.lib.core.config.WalksyLibConfig;
import walksy.lib.core.config.impl.Category;
import walksy.lib.core.config.impl.Option;
import walksy.lib.core.config.impl.builders.ConfigClassBuilder;
import walksy.lib.core.config.impl.options.BooleanOption;
import walksy.lib.core.config.impl.options.groups.OptionGroup;
import walksy.lib.core.utils.PathUtils;

public class Config implements WalksyLibConfig {

    public boolean modEnabled = false;
    public boolean featureEnabled = false;
    public boolean debugEnabled = false;
    public boolean experimentalFeatureEnabled = false;

    @Override
    public ConfigClass define() {
        ConfigClassBuilder builder = ConfigClass.createBuilder("Walksy's Test Config")
                .path(PathUtils.ofConfigDir("walksytestconfig"));

        builder.category(createCategory("General"));
        builder.category(createCategory("Too Long Category Test"));
        builder.category(createCategory("Another General"));

        return builder.build();
    }

    private Category createCategory(String name) {
        Option<Boolean> mainToggle = BooleanOption.createBuilder("Main Toggle", () -> modEnabled, val -> modEnabled = val).build();
        Option<Boolean> featureToggle = BooleanOption.createBuilder("Feature Toggle", () -> featureEnabled, val -> featureEnabled = val).build();
        Option<Boolean> debugToggle = BooleanOption.createBuilder("Debug Mode", () -> debugEnabled, val -> debugEnabled = val).build();
        Option<Boolean> experimentalToggle = BooleanOption.createBuilder("Experimental Feature", () -> experimentalFeatureEnabled, val -> experimentalFeatureEnabled = val).build();

        OptionGroup primaryGroup = OptionGroup.createBuilder("Primary Features")
                .addOption(mainToggle)
                .addOption(featureToggle)
                .build();

        OptionGroup debugGroup = OptionGroup.createBuilder("Debug Settings")
                .addOption(debugToggle)
                .build();

        OptionGroup miscGroup = OptionGroup.createBuilder("Miscellaneous")
                .addOption(experimentalToggle)
                .build();

        return Category.createBuilder(name)
                .option(mainToggle) // optionally add the mainToggle to the category as an option too
                .group(primaryGroup)
                .group(debugGroup)
                .group(miscGroup)
                .build();
    }
}
