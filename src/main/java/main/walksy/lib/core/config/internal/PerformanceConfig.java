package main.walksy.lib.core.config.internal;

import main.walksy.lib.api.config.Config;
import main.walksy.lib.api.config.option.Category;
import main.walksy.lib.api.config.option.OptionGroup;
import main.walksy.lib.api.config.option.Options;
import main.walksy.lib.core.render.perf.PerformanceMonitorWindow;
import main.walksy.lib.api.config.PathUtils;

public final class PerformanceConfig {
    public static final String CATEGORY = "Performance";
    private Config config;

    public Config config() {
        if (this.config == null) {
            this.config = Config.builder()
                .path(PathUtils.ofLibraryDir("walksylib"))
                .sharedFile()
                .category(Category.builder(CATEGORY)
                    .library()
                    .group(OptionGroup.builder("Profiling")
                        .option(Options.ofButton("Performance Monitor", () -> PerformanceMonitorWindow.get().toggle()).build())
                        .build())
                    .build())
                .build();
        }
        return this.config;
    }
}
