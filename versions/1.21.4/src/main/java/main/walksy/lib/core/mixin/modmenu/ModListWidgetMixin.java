package main.walksy.lib.core.mixin.modmenu;

import com.terraformersmc.modmenu.gui.widget.ModListWidget;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.WalksyLibConstants;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ModListWidget.class)
public abstract class ModListWidgetMixin {
    @Shadow
    private @Nullable String selectedModId;

    @Shadow
    protected abstract boolean isSelectedItem(int index);

    @Redirect(method = "renderListItems", at = @At(value = "INVOKE", target = "Lcom/terraformersmc/modmenu/gui/widget/ModListWidget;isSelectedItem(I)Z"))
    private boolean walksylib$hideSelection(final ModListWidget list, final int index) {
        if (WalksyLibConstants.NAMESPACE.equals(this.selectedModId) || WalksyLib.getInstance().getModEntryPointList().contains(this.selectedModId)) {
            return false;
        }
        return this.isSelectedItem(index);
    }
}
