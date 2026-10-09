package main.walksy.lib.core.mixin.modmenu;

import com.terraformersmc.modmenu.gui.widget.ModListWidget;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.WalksyLibConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModListWidget.class)
public class ModListWidgetMixin<E extends AbstractSelectionList.Entry<E>> {
    @Shadow
    private @Nullable String selectedModId;

    @Inject(method = "drawSelectionHighlight", at = @At("HEAD"), cancellable = true)
    public void extractSelection(final GuiGraphicsExtractor context, final int x, final int y, final int width, final int height, final int borderColor, final int fillColor, final CallbackInfo ci) {
        if (WalksyLibConstants.NAMESPACE.equals(this.selectedModId) || WalksyLib.getInstance().getModEntryPointList().contains(this.selectedModId)) {
            ci.cancel();
        }
    }
}
