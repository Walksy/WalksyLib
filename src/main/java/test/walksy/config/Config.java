package test.walksy.config;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.config.impl.LocalConfig;
import main.walksy.lib.core.config.WalksyLibConfig;
import main.walksy.lib.core.config.local.Category;
import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.OptionDescription;
import main.walksy.lib.core.config.local.builders.LocalConfigBuilder;
import main.walksy.lib.core.config.local.options.*;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.config.local.options.type.PixelGrid;
import main.walksy.lib.core.config.local.options.type.PixelGridAnimation;
import main.walksy.lib.core.config.local.options.type.WalksyLibColor;
import main.walksy.lib.core.utils.IdentifierWrapper;
import main.walksy.lib.core.utils.PathUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.util.List;

public class Config implements WalksyLibConfig {

    public static boolean modEnabled = false;
    public static double featureDouble = 0;
    public static WalksyLibColor color = new WalksyLibColor(255, 0, 0, 200);
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
            .build());

    public static List<String> stringList = List.of();
    public static IdentifierWrapper shieldTexture = new IdentifierWrapper(Identifier.ofVanilla("textures/entity/shield_base.png"));
    public static String string = "Default Value!";

    @Override
    public LocalConfig define() {
        return LocalConfig.createBuilder("Walksy's Test Config")
                .path(PathUtils.ofConfigDir("walksytestconfig"))
                .category(createGeneralCategory())
                .category(createDebugCategory())
                .category(createUICategory())
                .build();
    }

    private Category createGeneralCategory() {
        Option<Boolean> toggle = BooleanOption.createBuilder("Mod Enabled", () -> modEnabled, modEnabled, val -> modEnabled = val)
                .addWarning(new BooleanOption.Warning("Warning", "Are you sure you want to enable this feature?", null, null))
                .build();

        Option<Double> slider = NumericalOption.createBuilder("Feature Intensity", () -> featureDouble, featureDouble, val -> featureDouble = val)
                .values(0.0, 10.0, 0.1)
                .availability(() -> modEnabled)
                .build();

        Option<Boolean> experimental = BooleanOption.createBuilder("Experimental Feature", () -> experimentalFeatureEnabled, experimentalFeatureEnabled, val -> experimentalFeatureEnabled = val)
                .build();

        return Category.createBuilder("General")
                .option(toggle)
                .group(OptionGroup.createBuilder("Main Settings")
                        .addOption(toggle)
                        .addOption(slider)
                        .addOption(experimental)
                        .build())
                .build();
    }

    private Category createDebugCategory() {
        Option<WalksyLibColor> debugColor = ColorOption.createBuilder("Debug Color", () -> color, color, val -> color = val)
                .build();

        Option<PixelGridAnimation> animation = PixelGridAnimationOption
                .createBuilder("Pixel Grid Animation", () -> testGrid, testGrid, val -> testGrid = val)
                .position(new Point((MinecraftClient.getInstance().getWindow().getScaledWidth() / 2) - 5, (MinecraftClient.getInstance().getWindow().getScaledHeight() / 2) + 3))
                .build();

        Option<Runnable> debugButton = ButtonOption.createBuilder("Debug Print", () -> System.out.println("WOOO")).build();

        return Category.createBuilder("Debug")
                .group(OptionGroup.createBuilder("Debug Tools")
                        .addOption(debugColor)
                        .addOption(animation)
                        .addOption(debugButton)
                        .build())
                .build();
    }

    private Category createUICategory() {
        Option<List<String>> listOption = StringListOption.createBuilder("String List", () -> stringList, stringList, newValue -> stringList = newValue)
                .build();

        Option<IdentifierWrapper> texture = SpriteOption
                .createBuilder("Shield Texture", () -> shieldTexture, shieldTexture, newValue -> shieldTexture = newValue)
                .build();

        Option<String> input = StringOption
                .createBuilder("Custom Text", () -> string, string, newValue -> string = newValue)
                .build();

        return Category.createBuilder("UI")
                .group(OptionGroup.createBuilder("Interface Settings")
                        .addOption(listOption)
                        .addOption(texture)
                        .addOption(input)
                        .build())
                .build();
    }
}
