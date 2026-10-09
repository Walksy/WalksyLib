package main.walksy.lib.core.integration;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class WalksyLibMixinPlugin implements IMixinConfigPlugin {
    private static final String MODMENU_MIXINS = "main.walksy.lib.core.mixin.modmenu.";
    private boolean modMenuLoaded;

    @Override
    public void onLoad(final String mixinPackage) {
        this.modMenuLoaded = FabricLoader.getInstance().isModLoaded("modmenu");
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(final String targetClassName, final String mixinClassName) {
        return this.modMenuLoaded || !mixinClassName.startsWith(MODMENU_MIXINS);
    }

    @Override
    public void acceptTargets(final Set<String> myTargets, final Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(final String targetClassName, final ClassNode targetClass, final String mixinClassName, final IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(final String targetClassName, final ClassNode targetClass, final String mixinClassName, final IMixinInfo mixinInfo) {
    }
}
