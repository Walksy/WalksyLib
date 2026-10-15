package main.walksy.lib.core;

import main.walksy.lib.core.platform.compat.NeoForgeBootstrap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = "walksylib", dist = Dist.CLIENT)
public final class WalksyLibNeoForge {
    public WalksyLibNeoForge(final IEventBus modBus, final ModContainer container) {
        NeoForgeBootstrap.install(modBus);
    }
}
