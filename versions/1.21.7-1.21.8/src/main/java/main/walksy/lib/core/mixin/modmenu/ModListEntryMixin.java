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
    @Unique private int rowLeft;
    @Unique private int rowContentTop;
    @Unique private int rowRight;
    @Unique private int rowBottom;
    @Shadow @Final public Mod mod;
    @Shadow @Final protected ModListWidget list;

    @Shadow public abstract int getXOffset();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(final Mod mod, final ModListWidget list, final CallbackInfo ci) {
        this.isLibrary = WalksyLibConstants.NAMESPACE.equals(this.mod.getId());
        this.depends = this.isLibrary || WalksyLib.getInstance().getModEntryPointList().contains(this.mod.getId());
        this.extractor = new ModListEntryContentExtractor(this.mod, this.list);
    }

    @Inject(method = "render", at = @At("HEAD"))
    public void renderIconGlass(final GuiGraphics drawContext, final int index, final int y, final int x, final int rowWidth, final int rowHeight, final int mouseX, final int mouseY, final boolean hovered, final float delta, final CallbackInfo ci) {
        this.rowLeft = x + this.getXOffset();
        this.rowContentTop = y;
        this.rowRight = x + this.getXOffset() + rowWidth;
        this.rowBottom = y + rowHeight + 2;
        if (!this.depends || WLClient.screen() instanceof WalksyLibPanelScreen) {
            return;
        }
        this.extractor.extract(GuiContext.graphics(drawContext), this.rowLeft, this.rowContentTop, this.rowRight, this.rowBottom, hovered, this.list.getSelected() == (Object) this, delta);
    }

    @Inject(method = "openConfig", at = @At("HEAD"), cancellable = true)
    private void openWalksyConfigOnClick(final CallbackInfo ci) {
        if (!this.depends) {
            return;
        }
        final LiquidRect origin = ModListEntryContentExtractor.highlightRect(this.rowLeft, this.rowContentTop, this.rowRight, this.rowBottom);
        final WalksyLibPanelScreen screen = new WalksyLibPanelScreen(WLClient.screen(), this.mod.getName(), origin, ModListEntryContentExtractor.STYLE);
        WLClient.setScreen(this.isLibrary ? screen.openOnAllMods() : screen.preselect(this.mod.getId()));
        ci.cancel();
    }
}
