package main.walksy.lib.core.mixin.core.render.item;

import main.walksy.lib.core.render.texture.ItemModels;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(GuiRenderer.class)
public abstract class GuiItemAtlasMixin {
    @Shadow @Final private GuiRenderState renderState;
    @Shadow private GuiItemAtlas itemAtlas;

    @Inject(method = "prepareItemElements", at = @At("HEAD"))
    private void walksylib$resizeAtlasForOurItems(final CallbackInfo ci) {
        if (ItemModels.consumeAtlasSlotSizeChanged() && this.itemAtlas != null) {
            this.itemAtlas.close();
            this.itemAtlas = null;
        }
    }

    @ModifyArg(method = "prepareItemElements", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;prepareItemAtlas(Ljava/util/Set;I)Lnet/minecraft/client/gui/render/GuiItemAtlas;"), index = 1)
    private int walksylib$atlasSlotSize(final int vanillaSlotSize) {
        return Math.max(vanillaSlotSize, ItemModels.demandedAtlasSlotSize());
    }

    @Redirect(method = "prepareItemElements", at = @At(value = "INVOKE", target = "Ljava/util/Set;isEmpty()Z"))
    private boolean walksylib$keepGoingForOurItems(final Set<Object> vanillaIdentities) {
        return vanillaIdentities.isEmpty() && ItemModels.noFadedQueued();
    }

    @ModifyArg(method = "prepareItemElements", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;prepareItemAtlas(Ljava/util/Set;I)Lnet/minecraft/client/gui/render/GuiItemAtlas;"), index = 0)
    private Set<Object> walksylib$growAtlasForOurItems(final Set<Object> vanillaIdentities) {
        return ItemModels.withFadedIdentities(vanillaIdentities);
    }

    @Inject(method = "prepareItemElements", at = @At("RETURN"))
    private void walksylib$flushFadedItems(final CallbackInfo ci) {
        ItemModels.flushFaded(this.itemAtlas, this.renderState);
    }
}
