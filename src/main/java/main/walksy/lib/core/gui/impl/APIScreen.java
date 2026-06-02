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
import main.walksy.lib.core.gui.Graphics;
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

    public APIScreen(final Screen parent) {
        super("APIScreen", parent);
        this.modWidgets = new ArrayList<>();
        this.entryPointList = new ModEntryPointList();
        this.entryPointList.retrieve();
    }

    @Override
    protected void init() {
        super.init();
        final ButtonWidget backButton = new ButtonWidget(8, 5, 50, 16, true, "Back", this::onClose);
        this.addRenderableWidget(backButton);
        final List<CategoryTab> tabList = new ArrayList<>();
        final Category modCategory = new Category("Mods", null, null);
        final Category logCategory = new Category("Logs", null, null);
        tabList.add(new CategoryTab(modCategory, null));
        tabList.add(new CategoryTab(logCategory, null));
        if (!this.loaded) {
            this.tabWidget = new UniversalTabWidget(0, 27, this.width, 24, tabList, this.tabManager, TabLocation.TOP, this);
            this.tabWidget.selectTab(0, true);
        }
        this.loaded = true;
        this.addRenderableWidget(this.tabWidget);
        this.logWidget = new LogWidget("Logs", this, 40, 60, this.width - 80, this.height - 90);
        this.refreshLogs(this.logWidget);
        this.setupModWidgets();
    }

    public void refreshLogs(final LogWidget widget) {
        widget.clearLogs();
        for (final InternalLog log : WalksyLibLogger.getLogs()) {
            widget.addLog(log);
        }
    }


    @Override
    protected void extract(final Graphics graphics, final int mouseX, final int mouseY) {
        final GuiGraphicsExtractor context = graphics.context();
        context.horizontalLine(0, this.width, 25, MainColors.OUTLINE_BLACK.getRGB());
        context.horizontalLine(0, this.width, 26, MainColors.OUTLINE_WHITE.getRGB());
        context.centeredText(this.font, "WalksyLib API Screen", this.width / 2, 12 - this.font.lineHeight / 2, -1);
        if (!this.viewingMods()) {
            this.logWidget.extractRenderState(context, mouseX, mouseY, this.delta);
        } else {
            if (this.modWidgets.isEmpty()) {
                context.centeredText(this.font, "No Mods", this.width / 2, this.height / 2, -1);
            }
            for (final ModWidget widget : this.modWidgets) {
                widget.extractRenderState(context, mouseX, mouseY, this.delta);
            }
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent click, final boolean doubled) {
        for (final ModWidget widget : this.modWidgets) {
            if (this.viewingMods()) {
                widget.mouseClicked(click, doubled);
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double horizontalAmount, final double verticalAmount) {
        this.logWidget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private boolean viewingMods() {
        return this.tabManager.getCurrentTab().getTabTitle().getString().equals("Mods");
    }


    @Override
    protected void rebuildWidgets() {
        super.rebuildWidgets();
        if (this.tabWidget != null) {
            this.tabWidget.setWidth(this.width);
            this.tabWidget.setPosition(0, 27);
            final int i = this.tabWidget.getRectangle().bottom();
            final ScreenRectangle screenRect = new ScreenRectangle(0, i, this.width, this.height - 36 - i);
            this.tabManager.setTabArea(screenRect);
        }
        this.logWidget.setPosition(20, 60);
        this.logWidget.setWidth(this.width - 40);
        this.logWidget.setHeight(this.height - 80);
        this.setupModWidgets();
    }

    private void setupModWidgets() {
        this.modWidgets.clear();
        int x = 13, y = 60;

        for (final Mod mod : this.entryPointList.get()) {
            if (x + 140 + 13 > this.width) {
                x = 13;
                y += 47;
            }
            this.modWidgets.add(new ModWidget(mod, this, x, y));
            x += 158;
        }
    }
}
