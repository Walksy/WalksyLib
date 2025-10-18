package main.walksy.lib.core.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import main.walksy.lib.api.WalksyLibApi;
import main.walksy.lib.core.gui.impl.APIScreen;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Map;
import java.util.stream.Collectors;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return APIScreen::new;
    }

    /**
     * Provides config screens for mods that use WalksyLib as a dependency
     */
    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        return FabricLoader.getInstance().getEntrypointContainers("walksylib", WalksyLibApi.class).stream()
                .filter(c -> c.getEntrypoint().getConfigScreen() != null)
                .collect(Collectors.toMap(
                        c -> c.getProvider().getMetadata().getId(),
                        c -> parent -> c.getEntrypoint().getConfigScreen().apply(parent)
                ));
    }
}
