package main.walksy.lib.core.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import main.walksy.lib.api.WalksyLibApi;
import main.walksy.lib.api.config.Config;
import main.walksy.lib.core.gui.WalksyLibPanelScreen;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new WalksyLibPanelScreen(parent, "WalksyLib").openOnAllMods();
    }

    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        return FabricLoader.getInstance().getEntrypointContainers("walksylib", WalksyLibApi.class).stream().map(c -> {
            final Config config = c.getEntrypoint().getConfig();
            if (config == null) {
                return null;
            }
            final String id = c.getProvider().getMetadata().getId();
            final String name = c.getProvider().getMetadata().getName();
            return Map.entry(id, (ConfigScreenFactory<?>) parent -> new WalksyLibPanelScreen(parent, name).preselect(id));
        }).filter(Objects::nonNull).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
