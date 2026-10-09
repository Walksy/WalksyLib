package main.walksy.lib.core.mixin.modmenu;

import com.terraformersmc.modmenu.gui.widget.ModListWidget;
import com.terraformersmc.modmenu.gui.widget.entries.ModListEntry;
import com.terraformersmc.modmenu.util.mod.Mod;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.WalksyLibConstants;
import main.walksy.lib.core.gui.WalksyLibPanelScreen;
import main.walksy.lib.core.gui.liquid.LiquidRect;
import main.walksy.lib.core.platform.WLClient;
import main.walksy.lib.core.render.GuiContext;
import main.walksy.lib.core.render.ModListEntryContentExtractor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModListEntry.class)
public abstract class ModListEntryMixin extends ObjectSelectionList.Entry<ModListEntry> {
    @Unique private ModListEntryContentExtractor extractor;
    @Unique private boolean depends;
    @Unique private boolean isLibrary;
    @Shadow @Final public Mod mod;
    @Shadow @Final protected ModListWidget list;

    @Shadow public abstract int getXOffset();

    @Shadow public abstract int getYOffset();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(final Mod mod, final ModListWidget list, final CallbackInfo ci) {
        this.isLibrary = WalksyLibConstants.NAMESPACE.equals(this.mod.getId());
        this.depends = this.isLibrary || WalksyLib.getInstance().getModEntryPointList().contains(this.mod.getId());
        this.extractor = new ModListEntryContentExtractor(this.mod, this.list);
    }

    @Inject(method = "renderContent", at = @At("HEAD"), cancellable = true)
    public void renderIconGlass(final GuiGraphics drawContext, final int mouseX, final int mouseY, final boolean hovered, final float delta, final CallbackInfo ci) {
        if (!this.depends || WLClient.screen() instanceof WalksyLibPanelScreen) {
            return;
        }
        this.extractor.extract(GuiContext.graphics(drawContext), this.walksylib$left(), this.walksylib$contentTop(), this.walksylib$right(), this.walksylib$bottom(), hovered, this.list.getSelected() == (Object) this, delta);
    }

    @Inject(method = "openConfig", at = @At("HEAD"), cancellable = true)
    private void openWalksyConfigOnClick(final CallbackInfo ci) {
        if (!this.depends) {
            return;
        }
        final LiquidRect origin = ModListEntryContentExtractor.highlightRect(this.walksylib$left(), this.walksylib$contentTop(), this.walksylib$right(), this.walksylib$bottom());
        final WalksyLibPanelScreen screen = new WalksyLibPanelScreen(WLClient.screen(), this.mod.getName(), origin, ModListEntryContentExtractor.STYLE);
        WLClient.setScreen(this.isLibrary ? screen.openOnAllMods() : screen.preselect(this.mod.getId()));
        ci.cancel();
    }

    @Unique
    private int walksylib$left() {
        return this.getX() + this.getXOffset();
    }

    @Unique
    private int walksylib$contentTop() {
        return this.getContentY() + this.getYOffset();
    }

    @Unique
    private int walksylib$right() {
        return this.getX() + this.getXOffset() + this.getWidth();
    }

    @Unique
    private int walksylib$bottom() {
        return this.getY() + this.getYOffset() + this.getHeight();
    }
}
