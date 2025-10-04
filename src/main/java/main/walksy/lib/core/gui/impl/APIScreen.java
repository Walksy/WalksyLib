package main.walksy.lib.core.gui.impl;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.config.local.Category;
import main.walksy.lib.core.gui.utils.CategoryTab;
import main.walksy.lib.core.gui.utils.TabLocation;
import main.walksy.lib.core.gui.widgets.*;
import main.walksy.lib.core.mods.Mod;
import main.walksy.lib.core.utils.MainColors;
import main.walksy.lib.core.utils.log.InternalLog;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tab.TabManager;

import java.util.ArrayList;
import java.util.List;

public class APIScreen extends BaseScreen {

    private final TabManager tabManager = new TabManager(this::addDrawableChild, this::remove);
    private final List<ModWidget> modWidgets;
    private ButtonWidget backButton;
    private UniversalTabWidget tabWidget;
    private LogWidget logWidget;
    private boolean wtf = false;


    public APIScreen(Screen parent) {
        super("APIScreen", parent);
        this.modWidgets = new ArrayList<>();
        int yOffset = 60;
        int xOffset = 13;

        int count = 0;
        for (Mod mod : WalksyLib.getEntryPointModList()) {
            modWidgets.add(new ModWidget(mod, this, xOffset, yOffset));
            count++;
            if (count % 4 == 0) {
                xOffset = 13;
                yOffset += 47;
            } else {
                xOffset += 158;
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        backButton = new ButtonWidget(8, 5, 50, 16, true, "Back", this::close);
        addDrawableChild(backButton);

        List<CategoryTab> tabList = new ArrayList<>();
        Category modCategory = new Category("Mods", null, null);
        Category logCategory = new Category("Logs", null, null);
        tabList.add(new CategoryTab(modCategory, null));
        tabList.add(new CategoryTab(logCategory, null));
        if (!wtf) {
            tabWidget = new UniversalTabWidget(0, 27, this.width, 24, tabList, tabManager, TabLocation.TOP, this);
            tabWidget.selectTab(0, true);
        }
        wtf = true;
        addDrawableChild(tabWidget);
        logWidget = new LogWidget("Logs", this, 40, 60, width - 80, height - 90);
        this.refreshLogs(logWidget);
    }

    public void refreshLogs(LogWidget widget)
    {
        widget.clearLogs();
        for (InternalLog log : WalksyLib.getLogger().getLogs()) {
            widget.addLog(log);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackgroundLayer(context, delta);
        renderBlurEffect();
        super.render(context, mouseX, mouseY, delta);

        context.drawHorizontalLine(0, width, 25, MainColors.OUTLINE_BLACK.getRGB());
        context.drawHorizontalLine(0, width, 26, MainColors.OUTLINE_WHITE.getRGB());

        //context.drawHorizontalLine(0, width, height - 28, MainColors.OUTLINE_BLACK.getRGB());
        //context.drawHorizontalLine(0, width, height - 27, MainColors.OUTLINE_WHITE.getRGB());
        //context.drawTexture(RenderLayer::getGuiTextured, FOOTER_SEPARATOR_TEXTURE, 0, height - 28, 0.0F, 0.0F, width, 2, 32, 2);
        context.drawCenteredTextWithShadow(textRenderer, "WalksyLib API Screen", width / 2, 12 - textRenderer.fontHeight / 2, 0xFFFFFF);

        if (!viewingMods()) {
            logWidget.render(context, mouseX, mouseY, delta);
        } else {
            for (ModWidget widget : this.modWidgets) {
                widget.render(context, mouseX, mouseY, delta);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (ModWidget widget : this.modWidgets)
        {
            if (this.viewingMods()) {
                widget.mouseClicked(mouseX, mouseY, button);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.logWidget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private boolean viewingMods()
    {
        return this.tabManager.getCurrentTab().getTitle().getString().equals("Mods");
    }

    @Override
    protected void applyBlur() {}

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    @Override
    protected void refreshWidgetPositions() {
        super.refreshWidgetPositions();
        if (tabWidget != null) {
            tabWidget.setWidth(this.width);
            tabWidget.setPosition(0, 27);
            int i = tabWidget.getNavigationFocus().getBottom();
            ScreenRect screenRect = new ScreenRect(0, i, width, height - 36 - i);
            tabManager.setTabArea(screenRect);
        }
        this.logWidget.setPosition(20, 60);
        this.logWidget.setWidth(width - 40);
        this.logWidget.setHeight(height - 80);
    }
}
