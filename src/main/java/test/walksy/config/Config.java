package test.walksy.config;

import walksy.lib.core.config.ConfigClass;
import walksy.lib.core.config.WalksyLibConfig;
import walksy.lib.core.config.impl.Category;
import walksy.lib.core.config.impl.Option;
import walksy.lib.core.config.impl.builders.ConfigClassBuilder;
import walksy.lib.core.config.impl.options.BooleanOption;
import walksy.lib.core.config.impl.options.NumericalOption;
import walksy.lib.core.config.impl.options.groups.OptionGroup;
import walksy.lib.core.utils.PathUtils;


public class Config implements WalksyLibConfig {

    public boolean modEnabled = false;


    @Override
    public ConfigClass define() {
        ConfigClassBuilder builder = ConfigClass.createBuilder("Walksy's Test Config")
            .path(PathUtils.ofConfigDir("walksytestconfig"));

        for (int x = 0; x < 3; x++) {
            Option<Boolean> booleanOption = BooleanOption.createBuilder("Test Option", () -> modEnabled, value -> modEnabled = value)
                .build();

            OptionGroup optionGroup = OptionGroup.createBuilder("Test Group")
                .addOption(booleanOption)
                .build();

            OptionGroup optionGroup1 = OptionGroup.createBuilder("Test Group 2")
                .addOption(booleanOption)
                .build();

            Category generalCategory = Category.createBuilder("General")
                .option(booleanOption)
                .group(optionGroup)
                .build();

            Category general2Category = Category.createBuilder("General 2")
                .option(booleanOption)
                .group(optionGroup1)
                .build();

            builder.category(generalCategory);
            builder.category(general2Category);

        }
        return builder.build();
    }
}
