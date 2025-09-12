package test.walksy.config;

import main.walksy.lib.core.config.impl.LocalConfig;
import main.walksy.lib.core.config.WalksyLibConfig;
import main.walksy.lib.core.config.local.Category;
import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.builders.LocalConfigBuilder;
import main.walksy.lib.core.config.local.options.*;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.config.local.options.type.PixelGrid;
import main.walksy.lib.core.config.local.options.type.PixelGridAnimation;
import main.walksy.lib.core.utils.PathUtils;
import net.minecraft.client.MinecraftClient;

import java.awt.*;

public class Config implements WalksyLibConfig {

    public static boolean modEnabled = false;
    public static double featureDouble = 0;
    public static Color color = new Color(255, 0, 0, 200);
    public static boolean experimentalFeatureEnabled = false;
    public static PixelGridAnimation testGrid = new PixelGridAnimation(PixelGrid.create(15, 15)
            .set(1, 4).set(1, 5).set(1, 6).set(1, 7)
            .set(2, 3).set(2, 8).set(2, 9)
            .set(3, 3).set(3, 10).set(3, 11)
            .set(4, 2).set(4, 11)
            .set(5, 2).set(5, 10)
            .set(6, 2).set(6, 3).set(6, 4).set(6, 5).set(6, 6).set(6, 7).set(6, 8).set(6, 9)
            .set(7, 2).set(7, 3)
            .set(8, 2).set(8, 3).set(8, 4).set(8, 5).set(8, 6).set(8, 7).set(8, 8).set(8, 9)
            .set(9, 2).set(9, 10)
            .set(10, 2).set(10, 11)
            .set(11, 3).set(11, 10).set(11, 11)
            .set(12, 3).set(12, 8).set(12, 9)
            .set(13, 4).set(13, 5).set(13, 6).set(13, 7)
            .build()); //ex dee


    @Override
    public LocalConfig define() {
        LocalConfigBuilder builder = LocalConfig.createBuilder("Walksy's Test Config")
                .path(PathUtils.ofConfigDir("walksytestconfig"));

        builder.category(createCategory("General"));
        return builder.build();
    }

    private Category createCategory(String name) {
        Option<Boolean> mainToggle = BooleanOption.createBuilder("Test 1", () -> modEnabled, modEnabled, val -> modEnabled = val)
                .addWarning(new BooleanOption.Warning("Test Warning", "Are you sure you want to enable this feature?", null, null))
                .build();
        Option<Double> featureToggle = NumericalOption.createBuilder("Test 2", () -> featureDouble, featureDouble, val -> featureDouble = val)
                .values(0.0, 10.0, 0.1)
                .build();
        Option<Color> debugToggle = ColorOption.createBuilder("Test 3", () -> color, color, val -> color = val).build();
        Option<Boolean> experimentalToggle = BooleanOption.createBuilder("Test 4", () -> experimentalFeatureEnabled, experimentalFeatureEnabled, val -> experimentalFeatureEnabled = val).build();

        Option<PixelGridAnimation> gridOption = PixelGridAnimationOption
                .createBuilder("Test 4", () -> testGrid, testGrid, val -> testGrid = val)
                .position(new Point(MinecraftClient.getInstance().getWindow().getScaledWidth() / 2, MinecraftClient.getInstance().getWindow().getScaledHeight() / 2)) //TODO Make this a runnable or a consumer to that it dynamically changes the position of the icon, instead of just setting it once
                .build();

        OptionGroup primaryGroup = OptionGroup.createBuilder("Group Test")
                .addOption(mainToggle)
                .addOption(featureToggle)
                .build();

        OptionGroup debugGroup = OptionGroup.createBuilder("Group Test 2")
                .addOption(debugToggle)
                .addOption(gridOption)
                .build();

        OptionGroup miscGroup = OptionGroup.createBuilder("Group Test 3")
                .addOption(experimentalToggle)
                .build();

        return Category.createBuilder(name)
                .option(mainToggle)
                .group(primaryGroup)
                .group(debugGroup)
                .group(miscGroup)
                .build();
    }
}
