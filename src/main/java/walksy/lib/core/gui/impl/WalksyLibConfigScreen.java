package walksy.lib.core.gui.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tab.TabManager;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.Pool;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import walksy.lib.core.config.WalksyLibConfig;
import walksy.lib.core.config.impl.Category;
import walksy.lib.core.config.impl.Option;
import walksy.lib.core.config.impl.OptionDescription;
import walksy.lib.core.config.impl.options.groups.OptionGroup;
import walksy.lib.core.gui.WalksyLibScreenManager;
import walksy.lib.core.gui.utils.CategoryTab;
import walksy.lib.core.gui.utils.TabLocation;
import walksy.lib.core.gui.widgets.*;
import walksy.lib.core.mixin.ScreenAccessor;
import walksy.lib.core.utils.MainColors;
import walksy.lib.core.utils.Renderer;

import java.awt.*;
import java.util.*;
import java.util.List;

public class WalksyLibConfigScreen extends BaseScreen {

    private final WalksyLibConfig config;

    private final Pool shaderPool = new Pool(3);
    private final TabManager tabManager = new TabManager(this::addDrawableChild, this::remove);

    private final List<OptionGroupWidget> allGroupWidgets = new ArrayList<>();
    private final List<OptionWidget> allOptionWidgets = new ArrayList<>();


    private ScrollableTabWidget tabWidget;
    private ButtonWidget backButton, allModsButton, saveButton, resetButton, undoButton;
    private SearchBarWidget searchBar;

    private Option<?> focusedOption;

    public int tickCount = 0;

    public WalksyLibConfigScreen(Screen parent, WalksyLibConfig config) {
        super(config.define().getName(), parent);
        this.config = config;
        this.focusedOption = null;
    }

    @Override
    protected void init() {
        super.init();
        initButtons();
        initSearchBar();
        initTabs();
        refreshWidgetPositions();
    }

    private void initButtons() {
        backButton = new ButtonWidget(8, 5, 50, 16, true, "Back", this::close);
        allModsButton = new ButtonWidget(width - 58, 5, 50, 16, true, "All Mods", WalksyLibScreenManager::openAllMods);

        saveButton = new ButtonWidget(width - 58, height - 21, 50, 16, true, "Save", config.define()::save);
        saveButton.setEnabled(false);
        saveButton.setTooltip(Tooltip.of(Text.of("No changes have occurred")));

        resetButton = new ButtonWidget(width - 58 - 55, height - 21, 50, 16, true, "Reset", null);
        undoButton = new ButtonWidget(width - 58 - 110, height - 21, 50, 16, true, "Undo", null);

        addDrawableChild(backButton);
        addDrawableChild(allModsButton);
        addDrawableChild(saveButton);
        addDrawableChild(resetButton);
        addDrawableChild(undoButton);
    }

    private void initSearchBar() {
        searchBar = new SearchBarWidget(
                Text.of("SearchBar"),
                this,
                6,
                height - 21,
                150,
                16,
                this::search
        );
        addDrawableChild(searchBar);
    }

    private void initTabs() {
        List<CategoryTab> tabList = new ArrayList<>();

        for (Category category : config.define().getCategories()) {
            List<OptionGroupWidget> groupWidgets = new ArrayList<>();
            int yOffset = 60;

            for (OptionGroup group : category.optionGroups()) {
                int groupH = WalksyLibScreenManager.Globals.OPTION_HEIGHT;
                int optionHeight = WalksyLibScreenManager.Globals.OPTION_HEIGHT;

                int groupHeight = groupH;

                WalksyLibScreenManager.Globals.OPTION_PANEL_STARTX = (int) (width * 0.75);
                WalksyLibScreenManager.Globals.OPTION_PANEL_STARTY = 61;
                WalksyLibScreenManager.Globals.OPTION_PANEL_ENDX = width;
                WalksyLibScreenManager.Globals.OPTION_PANEL_ENDY = height - 120;
                WalksyLibScreenManager.Globals.OPTION_WIDTH = WalksyLibScreenManager.Globals.OPTION_PANEL_STARTX - 30;

                OptionGroupWidget groupWidget = new OptionGroupWidget((width - (WalksyLibScreenManager.Globals.OPTION_PANEL_ENDX - WalksyLibScreenManager.Globals.OPTION_PANEL_STARTX)) / 2, yOffset, 150, groupH, group, this);

                if (group.isExpanded()) {
                    int childY = yOffset + groupH;
                    List<OptionWidget> children = groupWidget.getChildren();
                    int childCount = children.size();

                    for (int i = 0; i < childCount; i++) {
                        OptionWidget child = children.get(i);
                        child.setPosition(child.getX(), childY);
                        childY += optionHeight;

                        groupHeight += optionHeight;
                        if (i < childCount - 1) {
                            childY += WalksyLibScreenManager.Globals.OPTION_GROUP_SEPARATION;
                            groupHeight += WalksyLibScreenManager.Globals.OPTION_GROUP_SEPARATION;
                        }
                    }
                }


                groupWidget.setHeight(groupHeight);
                groupWidgets.add(groupWidget);
                allGroupWidgets.add(groupWidget);
                allOptionWidgets.addAll(groupWidget.getChildren());

                yOffset += groupHeight + 10;
            }


            tabList.add(new CategoryTab(category, groupWidgets));
        }

        tabWidget = new ScrollableTabWidget(0, 27, this.width, 24, tabList, tabManager, TabLocation.TOP, this);
        addDrawableChild(tabWidget);
        tabWidget.selectTab(0, true);
    }


    public void layoutGroupWidgets() {
        if (tabManager.getCurrentTab() instanceof CategoryTab categoryTab) {
            List<OptionGroupWidget> widgets = categoryTab.getOptionGroupWidgets();
            int yOffset = 60;

            for (OptionGroupWidget group : widgets) {
                if (!group.visible) continue;

                group.setPosition((width - 150) / 2, yOffset);

                int groupHeaderHeight = WalksyLibScreenManager.Globals.OPTION_HEIGHT;
                int optionHeight = WalksyLibScreenManager.Globals.OPTION_HEIGHT;
                int groupHeight = groupHeaderHeight;

                if (group.getGroup().isExpanded()) {
                    int childY = yOffset + groupHeaderHeight;
                    List<OptionWidget> children = group.getChildren();

                    for (int i = 0; i < children.size(); i++) {
                        OptionWidget child = children.get(i);
                        if (!child.isVisible()) continue;

                        child.setPosition(child.getX(), childY);
                        childY += optionHeight;
                        groupHeight += optionHeight;

                        boolean hasNextVisible = false;
                        for (int j = i + 1; j < children.size(); j++) {
                            if (children.get(j).isVisible()) {
                                hasNextVisible = true;
                                break;
                            }
                        }

                        if (hasNextVisible) {
                            childY += WalksyLibScreenManager.Globals.OPTION_GROUP_SEPARATION;
                            groupHeight += WalksyLibScreenManager.Globals.OPTION_GROUP_SEPARATION;
                        }
                    }
                }

                group.setHeight(groupHeight);
                yOffset += groupHeight + 10;
            }
        }
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackgroundLayer(context, delta);
        renderBlurEffect();
        super.render(context, mouseX, mouseY, delta);
        this.render(context);
    }

    private void renderBackgroundLayer(DrawContext context, float delta) {
        if (this.client.world == null) {
            this.renderPanoramaBackground(context, delta);
        }
        this.renderDarkening(context);
    }

    private void render(DrawContext context) {
        context.drawTexture(RenderLayer::getGuiTextured, FOOTER_SEPARATOR_TEXTURE, 0, 25, 0.0F, 0.0F, width, 2, 32, 2);
        context.drawTexture(RenderLayer::getGuiTextured, FOOTER_SEPARATOR_TEXTURE, 0, height - 28, 0.0F, 0.0F, width, 2, 32, 2);
        context.drawCenteredTextWithShadow(textRenderer, config.define().getName(), width / 2, 12 - textRenderer.fontHeight / 2, 0xFFFFFF);

        WalksyLibScreenManager.Globals.OPTION_PANEL_STARTX = (int) (width * 0.75);
        WalksyLibScreenManager.Globals.OPTION_PANEL_STARTY = 61;
        WalksyLibScreenManager.Globals.OPTION_PANEL_ENDX = width;
        WalksyLibScreenManager.Globals.OPTION_PANEL_ENDY = height - 120;
        this.renderOptionPanel(context, this.focusedOption);
    }

    private void renderOptionPanel(DrawContext context, Option<?> option)
    {
        Renderer.fillRoundedRect(
                context,
                WalksyLibScreenManager.Globals.OPTION_PANEL_STARTX,
                WalksyLibScreenManager.Globals.OPTION_PANEL_STARTY,
                WalksyLibScreenManager.Globals.OPTION_PANEL_ENDX,
                WalksyLibScreenManager.Globals.OPTION_PANEL_ENDY,
                2,
                new Color(0, 0, 0, 100).getRGB()
        );
        Renderer.fillRoundedRectOutline(
                context,
                WalksyLibScreenManager.Globals.OPTION_PANEL_STARTX,
                WalksyLibScreenManager.Globals.OPTION_PANEL_STARTY - 1,
                WalksyLibScreenManager.Globals.OPTION_PANEL_ENDX,
                WalksyLibScreenManager.Globals.OPTION_PANEL_ENDY,
                2,
                1,
                MainColors.OUTLINE_WHITE.getRGB()
        );
        Renderer.fillRoundedRectOutline(
                context,
                WalksyLibScreenManager.Globals.OPTION_PANEL_STARTX - 1,
                WalksyLibScreenManager.Globals.OPTION_PANEL_STARTY - 2,
                WalksyLibScreenManager.Globals.OPTION_PANEL_ENDX,
                WalksyLibScreenManager.Globals.OPTION_PANEL_ENDY + 2,
                2,
                1,
                MainColors.OUTLINE_BLACK.getRGB()
        );

        context.drawHorizontalLine(WalksyLibScreenManager.Globals.OPTION_PANEL_STARTX + 1, WalksyLibScreenManager.Globals.OPTION_PANEL_ENDX, 78, MainColors.OUTLINE_WHITE.getRGB());

        if (option != null)
        {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    option.getName(),
                    (WalksyLibScreenManager.Globals.OPTION_PANEL_ENDX + WalksyLibScreenManager.Globals.OPTION_PANEL_STARTX) / 2,
                    66,
                    -1
            );

            OptionDescription desc = option.getDescription();
            if (desc != null)
            {
                switch (desc.getType())
                {
                    case TEXT -> {
                        String description = desc.getStringSupplier().get();
                        int maxWidth = WalksyLibScreenManager.Globals.OPTION_PANEL_ENDX;
                        int startX = WalksyLibScreenManager.Globals.OPTION_PANEL_STARTX + 5;
                        int startY = WalksyLibScreenManager.Globals.OPTION_PANEL_STARTY + 26;

                        List<OrderedText> lines = MinecraftClient.getInstance().textRenderer.wrapLines(
                                Text.of(description), (maxWidth - startX)
                        );

                        for (OrderedText line : lines) {
                            context.drawTextWithShadow(
                                    textRenderer,
                                    line,
                                    startX,
                                    startY,
                                    new Color(182, 182, 182).getRGB()
                            );
                            startY += textRenderer.fontHeight + 2;
                        }
                    }

                    //TODO Probably scissor the option panel
                    case RENDER -> {
                        int x = WalksyLibScreenManager.Globals.OPTION_PANEL_STARTX;
                        int y = WalksyLibScreenManager.Globals.OPTION_PANEL_STARTY;
                        int endX = WalksyLibScreenManager.Globals.OPTION_PANEL_ENDX;
                        int endY = WalksyLibScreenManager.Globals.OPTION_PANEL_ENDY;


                        desc.getRenderConsumer().accept(
                                context,
                                new OptionDescription.OptionPanel(x, y, endX, endY)
                        );
                    }
                }
            } else {
                context.drawCenteredTextWithShadow(
                        this.textRenderer,
                        "No Description",
                        (WalksyLibScreenManager.Globals.OPTION_PANEL_ENDX + WalksyLibScreenManager.Globals.OPTION_PANEL_STARTX) / 2,
                        WalksyLibScreenManager.Globals.OPTION_PANEL_STARTY + 26,
                        -1
                        );
            }
        }
    }

    private void renderBlurEffect() {
        PostEffectProcessor blur = client.getShaderLoader().loadPostEffect(Identifier.ofVanilla("blur"), DefaultFramebufferSet.MAIN_ONLY);
        if (blur != null) {
            blur.setUniforms("Radius", 12);
            blur.render(client.getFramebuffer(), shaderPool);
        }
        client.getFramebuffer().beginWrite(false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ((ScreenAccessor)this).getDrawables().forEach(w ->
        {
            if (w instanceof OptionGroupWidget optionGroupWidget)
            {
                //TODO
                /**
                 * Figure out why the ClickableWidget::mouseClicked method isn't functioning properly ->
                 * On click (when hovered) option groups don't get toggled
                 */
                optionGroupWidget.onMouseClick(mouseX, mouseY, button);
            } else if (w instanceof OptionWidget optionWidget)
            {
                optionWidget.onMouseClick(mouseX, mouseY, button);
            }
        });
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void refreshWidgetPositions() {
        if (tabWidget != null) {
            tabWidget.setWidth(this.width);
            tabWidget.setPosition(0, 27);
            int i = tabWidget.getNavigationFocus().getBottom();
            ScreenRect screenRect = new ScreenRect(0, i, width, height - 36 - i);
            tabManager.setTabArea(screenRect);


            layoutGroupWidgets();
        }

        backButton.setPosition(8, 5);
        allModsButton.setPosition(width - 58, 5);
        saveButton.setPosition(width - 58, height - 21);
        resetButton.setPosition(width - 58 - 55, height - 21);
        undoButton.setPosition(width - 58 - 110, height - 21);
        searchBar.setPosition(6, height - 21);

        WalksyLibScreenManager.Globals.OPTION_PANEL_STARTX = (int) (width * 0.75);
        WalksyLibScreenManager.Globals.OPTION_PANEL_STARTY = 61;
        WalksyLibScreenManager.Globals.OPTION_PANEL_ENDX = width;
        WalksyLibScreenManager.Globals.OPTION_PANEL_ENDY = height - 120;
        WalksyLibScreenManager.Globals.OPTION_WIDTH = WalksyLibScreenManager.Globals.OPTION_PANEL_STARTX - 30;

        for (OptionWidget widget : allOptionWidgets) {
            widget.setWidth(WalksyLibScreenManager.Globals.OPTION_WIDTH);
            widget.setHeight(WalksyLibScreenManager.Globals.OPTION_HEIGHT);
        }
    }

    @Override
    public void tick() {
        super.tick();
        tickCount++;
    }

    @Override
    protected void applyBlur() {}

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    public void showWidgetsForCategory(Category category) {
        CategoryTab selected = (CategoryTab) tabManager.getCurrentTab();
        if (selected == null || !selected.getCategory().name().equalsIgnoreCase(category.name())) return;
        tabWidget.updateVisibleWidgetsForTab(selected);
    }

    public void addWidget(ClickableWidget widget)
    {
        this.addDrawableChild(widget);
    }

    public void setFocusedOption(Option<?> option)
    {
        if (option != focusedOption) {
            this.focusedOption = option;
        }
    }

    //TODO Filter out categories
    public void search(String query) {
        allOptionWidgets.forEach(w -> w.updateSearchQuery(query));
        allGroupWidgets.forEach(w -> w.updateSearchQuery(query));
        for (OptionGroupWidget group : allGroupWidgets) {
            //extracted search to a new method, ensures the group isn't hidden if the group is not expanded
            group.visible = group.getChildren().stream()
                    .anyMatch(OptionWidget::searched);

            //Holy this is so ugly
            if (group.searched(false)) //no Levenshtein distance, similar groups won't be filtered
            {
                group.visible = true;
                group.getGroup().setExpanded(true);
                group.getChildren().forEach(w -> w.updateSearchQuery(""));
            }
        }
        layoutGroupWidgets();
    }
}
