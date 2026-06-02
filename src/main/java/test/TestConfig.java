package test;

import main.walksy.lib.api.WalksyLibConfig;
import main.walksy.lib.core.config.impl.ModConfig;
import main.walksy.lib.core.config.local.builders.LocalConfigBuilder;
import main.walksy.lib.core.utils.PathUtils;

public class TestConfig implements WalksyLibConfig {

    @Override
    public ModConfig define() {
        LocalConfigBuilder builder = ModConfig.createBuilder()
                .path(PathUtils.ofConfigDir("test"));

        return builder.build();
    }
}
