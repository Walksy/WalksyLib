package main.walksy.lib.core.gui.impl;

import main.walksy.lib.core.config.local.Category;
import main.walksy.lib.core.gui.utils.CategoryTab;
import main.walksy.lib.core.gui.utils.TabLocation;
import main.walksy.lib.core.gui.widgets.ButtonWidget;
import main.walksy.lib.core.gui.widgets.LogWidget;
import main.walksy.lib.core.gui.widgets.ModWidget;
import main.walksy.lib.core.gui.widgets.UniversalTabWidget;
import main.walksy.lib.core.mods.Mod;
import main.walksy.lib.core.mods.ModEntryPointList;
import main.walksy.lib.core.utils.MainColors;
import main.walksy.lib.core.utils.log.InternalLog;
import main.walksy.lib.core.utils.log.WalksyLibLogger;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.List;

public class APIScreen extends BaseScreen {

    private final TabManager tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);
    private final List<ModWidget> modWidgets;
    private final ModEntryPointList entryPointList;
    private UniversalTabWidget tabWidget;
    private LogWidget logWidget;
    private boolean loaded = false;
    public Screen parent;

    public APIScreen(Screen parent) {
        super("APIScreen", parent);
        this.modWidgets = new ArrayList<>();
        this.parent = parent;
        this.entryPointList = new ModEntryPointList();
        this.entryPointList.retrieve();
    }

    @Override
    protected void init() {
        super.init();
        ButtonWidget backButton = new ButtonWidget(8, 5, 50, 16, true, "Back", this::onClose);
        addRenderableWidget(backButton);

        List<CategoryTab> tabList = new ArrayList<>();
        Category modCategory = new Category("Mods", null, null);
        Category logCategory = new Category("Logs", null, null);
        tabList.add(new CategoryTab(modCategory, null));
        tabList.add(new CategoryTab(logCategory, null));
        if (!loaded) {
            tabWidget = new UniversalTabWidget(0, 27, this.width, 24, tabList, tabManager, TabLocation.TOP, this);
            tabWidget.selectTab(0, true);
        }
        loaded = true;
        addRenderableWidget(tabWidget);
        logWidget = new LogWidget("Logs", this, 40, 60, width - 80, height - 90);
        this.refreshLogs(logWidget);

        this.setupModWidgets();
    }

    public void refreshLogs(LogWidget widget)
    {
        widget.clearLogs();
        for (InternalLog log : WalksyLibLogger.getLogs()) {
            widget.addLog(log);
        }
    }


    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        renderBackgroundLayer(context, delta);
        super.extractRenderState(context, mouseX, mouseY, delta);

        context.horizontalLine(0, width, 25, MainColors.OUTLINE_BLACK.getRGB());
        context.horizontalLine(0, width, 26, MainColors.OUTLINE_WHITE.getRGB());

        //context.horizontalLine(0, width, height - 28, MainColors.OUTLINE_BLACK.getRGB());
        //context.horizontalLine(0, width, height - 27, MainColors.OUTLINE_WHITE.getRGB());
        //context.drawTexture(RenderPipelines.GUI_TEXTURED, FOOTER_SEPARATOR_TEXTURE, 0, height - 28, 0.0F, 0.0F, width, 2, 32, 2);
        context.centeredText(font, "WalksyLib API Screen", width / 2, 12 - font.lineHeight / 2, -1);

        if (!viewingMods()) {
            logWidget.extractRenderState(context, mouseX, mouseY, delta);
        } else {
            if (this.modWidgets.isEmpty()) {
                context.centeredText(font, "No Mods", width / 2, height / 2, -1);
            }
            for (ModWidget widget : this.modWidgets) {
                widget.extractRenderState(context, mouseX, mouseY, delta);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        for (ModWidget widget : this.modWidgets) {
            if (this.viewingMods()) {
                widget.mouseClicked(click, doubled);
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.logWidget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private boolean viewingMods()
    {
        return this.tabManager.getCurrentTab().getTabTitle().getString().equals("Mods");
    }


    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {

    }

    @Override
    protected void rebuildWidgets() {
        super.rebuildWidgets();
        if (tabWidget != null) {
            tabWidget.setWidth(this.width);
            tabWidget.setPosition(0, 27);
            int i = tabWidget.getRectangle().bottom();
            ScreenRectangle screenRect = new ScreenRectangle(0, i, width, height - 36 - i);
            tabManager.setTabArea(screenRect);
        }
        this.logWidget.setPosition(20, 60);
        this.logWidget.setWidth(width - 40);
        this.logWidget.setHeight(height - 80);
        this.setupModWidgets();
    }

    private void setupModWidgets() {
        this.modWidgets.clear();
        int x = 13, y = 60;

        for (Mod mod : this.entryPointList.get()) {
            if (x + 140 + 13 > this.width) { //140 mod widget width
                x = 13;
                y += 47;
            }
            this.modWidgets.add(new ModWidget(mod, this, x, y));
            x += 158;
        }
    }
}
