package walksy.lib.core.gui.impl;

import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tab.TabManager;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.Pool;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import walksy.lib.core.config.WalksyLibConfig;
import walksy.lib.core.config.impl.Category;
import walksy.lib.core.config.impl.options.groups.OptionGroup;
import walksy.lib.core.gui.WalksyLibScreenManager;
import walksy.lib.core.gui.utils.CategoryTab;
import walksy.lib.core.gui.utils.TabLocation;
import walksy.lib.core.gui.widgets.*;

import java.awt.*;
import java.util.*;
import java.util.List;

public class WalksyLibConfigScreen extends BaseScreen {

    private ScrollableTabWidget tabWidget;

    private final WalksyLibConfig config;
    private final Pool pool = new Pool(3);
    private final TabManager tabManager = new TabManager(this::addDrawableChild, this::remove);

    private final Map<Category, List<OptionGroupWidget>> categoryGroupWidgets = new HashMap<>();
    private final Map<Category, List<OptionWidget>> categoryUngroupedOptionWidgets = new HashMap<>();
    private final List<OptionGroupWidget> currentVisibleGroups = new ArrayList<>();
    private final List<OptionWidget> currentVisibleUngroupedOptions = new ArrayList<>();

    public int tickCount;

    private boolean tab;

    public WalksyLibConfigScreen(Screen parent, WalksyLibConfig config) {
        super(config.define().getName(), parent);
        this.config = config;
        this.tab = false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderB(context, delta);
        renderBlur();

        //context.fill(0, 47, width, height - 28, new Color(0, 0, 0, 100).getRGB());

        super.render(context, mouseX, mouseY, delta); //Render drawables
        context.drawTexture(RenderLayer::getGuiTextured, FOOTER_SEPARATOR_TEXTURE, 0, 25, 0.0F, 0.0F, this.width, 2, 32, 2);
        context.drawTexture(RenderLayer::getGuiTextured, FOOTER_SEPARATOR_TEXTURE, 0, height - 28, 0.0F, 0.0F, this.width, 2, 32, 2);

        context.drawCenteredTextWithShadow(this.textRenderer, config.define().getName(), this.width / 2, 12 - this.textRenderer.fontHeight / 2, 0xFFFFFF);
        if (this.tab)
        {
            //context.fillGradient(tabWidget.getX(), tabWidget.getY() + tabWidget.getHeight() - 3, tabWidget.getX() + tabWidget.getWidth(), tabWidget.getY() + tabWidget.getHeight() + 2, Color.BLACK.getRGB(), new Color(0, 0, 0, 0).getRGB());
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    @Override
    public void tick() {
        super.tick();
        tickCount++;
    }

    @Override
    protected void init() {
        super.init();

        ButtonWidget backButton
            = new ButtonWidget(8, 5, 50, 16, true, "Back", this::close); //TODO make a check so people can't leave if they have not saved

        ButtonWidget allModsButton
            = new ButtonWidget(width - 58, 5, 50, 16, true, "All Mods", WalksyLibScreenManager::openAllMods);

        int buttonOffset = 55;

        //TODO
        ButtonWidget saveButton
            = new ButtonWidget(width - 58, height - 21, 50, 16, true, "Save", config.define()::save);

        ButtonWidget resetButton
            = new ButtonWidget((width - 58) - buttonOffset, height - 21, 50, 16, true, "Reset", null);

        ButtonWidget undoButton
            = new ButtonWidget((width - 58) - buttonOffset * 2, height - 21, 50, 16, true, "Undo", null);

        //END TODO

        SearchBarWidget searchBar = new SearchBarWidget(6, height - 21, 150, 16, Text.of("SearchBar"), this);

        this.addDrawableChild(backButton);
        this.addDrawableChild(allModsButton);
        this.addDrawableChild(saveButton);
        this.addDrawableChild(resetButton);
        this.addDrawableChild(undoButton);

        this.addDrawableChild(searchBar);


        List<CategoryTab> tabsList = new ArrayList<>();

        for (Category category : config.define().getCategories()) {
            List<OptionGroupWidget> optionGroupWidgets = new ArrayList<>();
            List<OptionWidget> ungroupedOptionWidgets = new ArrayList<>();

            int yOff = 60;

            for (OptionGroup group : category.optionGroups()) {
                int height = group.getOptions().size() * 15;

                OptionGroupWidget groupWidget = new OptionGroupWidget(0, yOff, 150, height, group, this);
                optionGroupWidgets.add(groupWidget);

                yOff += height + 5;
            }


            categoryGroupWidgets.put(category, optionGroupWidgets);
            categoryUngroupedOptionWidgets.put(category, ungroupedOptionWidgets);

            tabsList.add(new CategoryTab(category, optionGroupWidgets));
            this.tab = true;
        }

        this.tabWidget = new ScrollableTabWidget(0, 25 + 2, this.width, 24, tabsList, tabManager, TabLocation.TOP, this);
        this.addDrawableChild(this.tabWidget);
        this.tabWidget.selectTab(0);

        this.refreshWidgetPositions();
    }


    @Override
    protected void refreshWidgetPositions() {
        if (this.tabWidget != null) {
            this.tabWidget.setWidth(this.width);
            int i = this.tabWidget.getNavigationFocus().getBottom();
            ScreenRect screenRect = new ScreenRect(0, i, this.width, this.height - 36 - i);
            this.tabManager.setTabArea(screenRect);
        }
    }

    @Override
    protected void applyBlur() {}

    public void showWidgetsForCategory(Category category) {
        //Remove old widgets
        for (OptionGroupWidget groupWidget : currentVisibleGroups) {
            this.remove(groupWidget);
            for (OptionWidget child : groupWidget.getChildren()) {
                this.remove(child);
            }
        }
        for (OptionWidget optionWidget : currentVisibleUngroupedOptions) {
            this.remove(optionWidget);
        }

        currentVisibleGroups.clear();
        currentVisibleUngroupedOptions.clear();

        //Add new widgets for selected category
        List<OptionGroupWidget> groups = categoryGroupWidgets.get(category);
        if (groups != null) {
            int yOff = 0;
            for (OptionGroupWidget groupWidget : groups) {
                groupWidget.setPosition(15, yOff);
                this.addDrawableChild(groupWidget);

                for (OptionWidget child : groupWidget.getChildren()) {
                    this.addDrawableChild(child);
                }

                currentVisibleGroups.add(groupWidget);
                yOff += groupWidget.getHeight() + 5;
            }
        }

        List<OptionWidget> ungroupedOptions = categoryUngroupedOptionWidgets.get(category);
        if (ungroupedOptions != null) {
            int yOff = groups != null ? groups.stream().mapToInt(OptionGroupWidget::getHeight).sum() + (groups.size() * 5) : 0;

            for (OptionWidget optionWidget : ungroupedOptions) {
                optionWidget.setPosition(15, yOff);
                this.addDrawableChild(optionWidget);
                currentVisibleUngroupedOptions.add(optionWidget);
                yOff += 15 + 5;
            }
        }
    }

    public void renderB(DrawContext context, float delta) {
        if (this.client.world == null) {
            this.renderPanoramaBackground(context, delta);
        }

        this.renderDarkening(context);
    }

    public void renderBlur() {
        PostEffectProcessor postEffectProcessor = this.client.getShaderLoader().loadPostEffect(Identifier.ofVanilla("blur"), DefaultFramebufferSet.MAIN_ONLY);
        if (postEffectProcessor != null) {
            postEffectProcessor.setUniforms("Radius", 12);
            postEffectProcessor.render(this.client.getFramebuffer(), this.pool);
        }
        this.client.getFramebuffer().beginWrite(false);
    }
}
