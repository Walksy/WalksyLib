package main.walksy.lib.core.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import main.walksy.lib.api.WalksyLibApi;

import java.util.Optional;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        Optional<WalksyLibApi> api = FabricLoader.getInstance()
            .getEntrypointContainers("walksylib", WalksyLibApi.class).stream()
            .map(EntrypointContainer::getEntrypoint)
            .filter(e -> e.getConfigScreen() != null)
            .findFirst();


        return api.<ConfigScreenFactory<?>>map(walksyLibApi -> parent -> walksyLibApi.getConfigScreen().apply(parent))
            .orElse(null);
    }
}
