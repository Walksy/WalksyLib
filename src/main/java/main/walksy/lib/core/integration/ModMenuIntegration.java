package main.walksy.lib.core.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import main.walksy.lib.api.WalksyLibApi;
import main.walksy.lib.core.config.impl.LocalConfig;
import main.walksy.lib.core.gui.impl.APIScreen;
import main.walksy.lib.core.gui.impl.BaseScreen;
import main.walksy.lib.core.gui.impl.ConflictedConfigScreen;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return APIScreen::new;
    }

    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        return FabricLoader.getInstance()
                .getEntrypointContainers("walksylib", WalksyLibApi.class)
                .stream()
                .map(c -> {
                    WalksyLibApi entryPoint = c.getEntrypoint();
                    BaseScreen overridableScreen = entryPoint.getOverridableScreen(null);
                    LocalConfig config = entryPoint.getConfig();
                    if (overridableScreen == null && config == null) {
                        return null;
                    }

                    return Map.entry(c.getProvider().getMetadata().getId(), (ConfigScreenFactory<?>) parent -> { //TODO overriableconfigscreen
                                BaseScreen screen = entryPoint.getOverridableScreen(parent);
                                if (screen != null && config != null) {
                                    WalksyLibConfigScreen configScreen = new WalksyLibConfigScreen(parent, config, c.getProvider().getMetadata().getName());
                                    return new ConflictedConfigScreen("Choose Screen", parent, screen, configScreen, entryPoint.getConflictedConfigButtonTitles());
                                }
                                if (screen == null && config != null) {
                                    return new WalksyLibConfigScreen(parent, config, c.getProvider().getMetadata().getName());
                                }
                                return screen;
                            }
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
