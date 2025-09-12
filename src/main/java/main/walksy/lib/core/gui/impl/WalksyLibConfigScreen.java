package main.walksy.lib.core.gui.impl;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.config.impl.LocalConfig;
import main.walksy.lib.core.gui.popup.PopUp;
import main.walksy.lib.core.gui.popup.impl.WarningPopUp;
import main.walksy.lib.core.gui.widgets.*;
import main.walksy.lib.core.gui.widgets.sub.SubWidget;
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
import main.walksy.lib.core.config.local.Category;
import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.OptionDescription;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.manager.WalksyLibScreenManager;
import main.walksy.lib.core.gui.utils.CategoryTab;
import main.walksy.lib.core.gui.utils.TabLocation;
import main.walksy.lib.core.mixin.ScreenAccessor;
import main.walksy.lib.core.utils.MainColors;
import main.walksy.lib.core.utils.Renderer;

import java.awt.*;
import java.util.*;
import java.util.List;

public class WalksyLibConfigScreen extends BaseScreen {
    private final LocalConfig config;
    private final Pool shaderPool = new Pool(3);
    private final TabManager tabManager = new TabManager(this::addDrawableChild, this::remove);
    private final List<OptionGroupWidget> allGroupWidgets = new ArrayList<>();
    private final List<OptionWidget> allOptionWidgets = new ArrayList<>();
    private ScrollableTabWidget tabWidget;
    private ButtonWidget backButton, allModsButton, saveButton, resetButton, undoButton;
    private SearchBarWidget searchBar;
    private Option<?> focusedOption;
    private int maxScroll = 0;
    public boolean scroll = true;
    public int tickCount = 0;
    public PopUp popUp = null;
    public int scrollY = 0;

    public WalksyLibConfigScreen(Screen parent) {
        super(parent.getTitle().getString(), parent);
        this.config = WalksyLib.getInstance().getConfigManager().getLocal();
        this.focusedOption = null;
    }

    @Override
    public void close() {
        if (this.shouldUndoOptions()) {
            popUp = new WarningPopUp(this, "You have unsaved changes!", "Are you sure you want to leave without saving?",
                    () -> {
                        this.undo();
                        super.close();
                    },
                    () -> popUp = null
            );

        } else if (popUp != null)
        {

        } else {
            super.close();
        }
    }

    @Override
    protected void init() {
        super.init();
        initButtons();
        initSearchBar();
        initTabs();
        refreshWidgetPositions();
        this.defineOptions();
    }

    private void initButtons() {
        backButton = new ButtonWidget(8, 5, 50, 16, true, "Back", this::close);
        allModsButton = new ButtonWidget(width - 58, 5, 50, 16, true, "All Mods", WalksyLib.getInstance().getScreenManager()::openAllMods);

        saveButton = new ButtonWidget(width - 58, height - 21, 50, 16, true, "Save", this::save);
        resetButton = new ButtonWidget(width - 58 - 55, height - 21, 50, 16, true, "Reset", this::resetOptions);
        undoButton = new ButtonWidget(width - 58 - 110, height - 21, 50, 16, true, "Undo", this::undo);

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

        for (Category category : config.categories()) {
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
                WalksyLibScreenManager.Globals.OPTION_WIDTH = WalksyLibScreenManager.Globals.OPTION_PANEL_STARTX - 30 - 22;

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
        if (!(tabManager.getCurrentTab() instanceof CategoryTab categoryTab)) return;

        List<OptionGroupWidget> widgets = categoryTab.getOptionGroupWidgets();

        int contentYOffset = 60;
        for (OptionGroupWidget group : widgets) {
            if (!group.visible) continue;

            int groupHeight = WalksyLibScreenManager.Globals.OPTION_HEIGHT;

            if (group.getGroup().isExpanded()) {
                List<OptionWidget> children = group.getChildren();
                for (int i = 0; i < children.size(); i++) {
                    OptionWidget child = children.get(i);
                    if (!child.isVisible()) continue;

                    int childHeight = (child instanceof OpenableWidget oW && oW.open) ? oW.OPEN_HEIGHT : WalksyLibScreenManager.Globals.OPTION_HEIGHT;
                    groupHeight += childHeight;

                    boolean hasNextVisible = false;
                    for (int j = i + 1; j < children.size(); j++) {
                        if (children.get(j).isVisible()) {
                            hasNextVisible = true;
                            break;
                        }
                    }
                    if (hasNextVisible) groupHeight += WalksyLibScreenManager.Globals.OPTION_GROUP_SEPARATION;
                }
            }

            group.setHeight(groupHeight);
            contentYOffset += groupHeight + 10;
        }

        int viewHeight = height - 120;
        maxScroll = Math.max(0, contentYOffset - (10 * widgets.size()) - viewHeight);
        scrollY = Math.min(scrollY, maxScroll);

        int yOffset = 60 - scrollY;
        for (OptionGroupWidget group : widgets) {
            if (!group.visible) continue;

            group.setPosition((width - 150) / 2, yOffset);

            if (group.getGroup().isExpanded()) {
                int childY = yOffset + WalksyLibScreenManager.Globals.OPTION_HEIGHT;
                List<OptionWidget> children = group.getChildren();

                for (int i = 0; i < children.size(); i++) {
                    OptionWidget child = children.get(i);
                    if (!child.isVisible()) continue;

                    int childHeight = (child instanceof OpenableWidget oW && oW.open) ? oW.OPEN_HEIGHT : WalksyLibScreenManager.Globals.OPTION_HEIGHT;
                    child.setPosition(child.getX(), childY);
                    child.setHeight(childHeight);
                    int size = WalksyLibScreenManager.Globals.OPTION_HEIGHT;
                    child.onWidgetUpdate(child.getX() + child.getWidth() - size + 22, childY);
                    childY += childHeight;

                    boolean hasNextVisible = false;
                    for (int j = i + 1; j < children.size(); j++) {
                        if (children.get(j).isVisible()) {
                            hasNextVisible = true;
                            break;
                        }
                    }
                    if (hasNextVisible) childY += WalksyLibScreenManager.Globals.OPTION_GROUP_SEPARATION;
                }
            }

            yOffset += group.getHeight() + 10;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackgroundLayer(context, delta);
        renderBlurEffect();
        super.render(context, popUp == null ? mouseX : 0, popUp == null ? mouseY : 0, delta);
        this.render(context);
        if (popUp != null)
        {
            WalksyLib.getInstance().get2DRenderer().startPopUpRender(context, 1, width, height);
            popUp.render(context, mouseX, mouseY, delta);
            WalksyLib.getInstance().get2DRenderer().endPopUpRender(context);
        }
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
        context.drawCenteredTextWithShadow(textRenderer, config.name(), width / 2, 12 - textRenderer.fontHeight / 2, 0xFFFFFF);

        WalksyLibScreenManager.Globals.OPTION_PANEL_STARTX = (int) (width * 0.75);
        WalksyLibScreenManager.Globals.OPTION_PANEL_STARTY = 61;
        WalksyLibScreenManager.Globals.OPTION_PANEL_ENDX = width;
        WalksyLibScreenManager.Globals.OPTION_PANEL_ENDY = height - 120;
        this.saveButton.setEnabled(this.shouldUndoOptions());
        this.saveButton.setTooltip(!this.saveButton.active ? Tooltip.of(Text.of("No changes have occurred")) : null);
        this.resetButton.setEnabled(this.shouldResetOptions());
        this.undoButton.setEnabled(this.shouldUndoOptions());
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

        if (popUp != null)
        {
            popUp.layout(popUp.width, popUp.height);
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
        if (popUp != null)
        {
            popUp.onClick(mouseX, mouseY, button);
        } else {
            ((ScreenAccessor) this).getDrawables().forEach(w ->
            {
                if (w instanceof OptionGroupWidget optionGroupWidget) {
                    //TODO
                    /**
                     * Figure out why the ClickableWidget::mouseClicked method isn't functioning properly ->
                     * On click (when hovered) option groups don't get toggled
                     */
                    optionGroupWidget.onMouseClick(mouseX, mouseY, button);
                } else if (w instanceof OptionWidget optionWidget) {
                    if (optionWidget.isVisible() && optionWidget.isInScissor(0, 49, width, height - 28)) {
                        optionWidget.onMouseClick(mouseX, mouseY, button);
                    }
                    if (optionWidget.resetButton.active) {
                        optionWidget.resetButton.onClick(mouseX, mouseY);
                    }
                }
            });
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.popUp != null)
        {
            this.popUp.onMouseRelease(mouseX, mouseY, button);
        }
        ((ScreenAccessor)this).getDrawables().forEach(w ->
        {
            if (w instanceof OptionGroupWidget optionGroupWidget)
            {
                //TODO
                /**
                 * Figure out why the ClickableWidget::mouseClicked method isn't functioning properly ->
                 * On click (when hovered) option groups don't get toggled
                 */
            } else if (w instanceof OptionWidget optionWidget)
            {
                optionWidget.onMouseRelease(mouseX, mouseY, button);
            }
        });
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (popUp == null) {
            ((ScreenAccessor) this).getDrawables().forEach(w ->
            {
                if (w instanceof OptionGroupWidget optionGroupWidget) {
                    //TODO
                    /**
                     * Figure out why the ClickableWidget::mouseClicked method isn't functioning properly ->
                     * On click (when hovered) option groups don't get toggled
                     */
                } else if (w instanceof OptionWidget optionWidget) {
                    optionWidget.onMouseDrag(mouseX, mouseY, button, deltaX, deltaY);
                }
            });
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (popUp == null) {
            ((ScreenAccessor) this).getDrawables().forEach(w ->
            {
                if (w instanceof OptionGroupWidget optionGroupWidget) {
                    //TODO
                    /**
                     * Figure out why the ClickableWidget::mouseClicked method isn't functioning properly ->
                     * On click (when hovered) option groups don't get toggled
                     */
                } else if (w instanceof OptionWidget optionWidget) {
                    optionWidget.onMouseMove(mouseX, mouseY);
                }
            });
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (popUp == null && scroll) {
            scrollY -= (int) (verticalAmount * 20);
            scrollY = Math.max(0, Math.min(scrollY, maxScroll));
            layoutGroupWidgets();
        } else if (popUp == null)
        {
            ((ScreenAccessor) this).getDrawables().forEach(w ->
            {
                if (w instanceof OptionGroupWidget optionGroupWidget) {
                    //TODO
                    /**
                     * Figure out why the ClickableWidget::mouseClicked method isn't functioning properly ->
                     * On click (when hovered) option groups don't get toggled
                     */
                } else if (w instanceof OptionWidget optionWidget) {
                    optionWidget.onMouseScroll(mouseX, mouseY, verticalAmount);
                }
            });
        } else {
            popUp.onScroll(mouseX, mouseY, verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
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
        WalksyLibScreenManager.Globals.OPTION_WIDTH = WalksyLibScreenManager.Globals.OPTION_PANEL_STARTX - 30 - 22;

        for (OptionWidget widget : allOptionWidgets) {
            widget.setWidth(WalksyLibScreenManager.Globals.OPTION_WIDTH);
            int size = WalksyLibScreenManager.Globals.OPTION_HEIGHT;
            widget.onWidgetUpdate(widget.getWidth() - size + 15 + 22, widget.getY());
            if (widget instanceof OpenableWidget openableWidget && openableWidget.open)
            {
                openableWidget.setHeight(openableWidget.OPEN_HEIGHT);
            } else {
                widget.setHeight(WalksyLibScreenManager.Globals.OPTION_HEIGHT);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        for (OptionWidget widget : allOptionWidgets)
        {
            widget.tick();
        }
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

    public void onChangesMade(Option<?> option) {

    }

    public void save()
    {
        this.config.save();
        this.defineOptions();
    }

    private boolean shouldResetOptions() {
        for (Category category : this.config.categories()) {
            for (OptionGroup group : category.optionGroups()) {
                for (Option<?> option : group.getOptions()) {
                    if (option.hasChanged()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    private boolean shouldUndoOptions() {
        for (Category category : this.config.categories()) {
            for (OptionGroup group : category.optionGroups()) {
                for (Option<?> option : group.getOptions()) {
                    if (!option.screenInstanceCheck()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    public void undo()
    {
        this.config.categories().forEach(category -> category.optionGroups().forEach(optionGroup -> optionGroup.getOptions().forEach(Option::undo)));
        for (OptionWidget widget : allOptionWidgets)
        {
            if (widget instanceof PixelGridAnimationWidget widget1)
            {
                widget1.reset();
            }
        }
    }

    public void resetOptions()
    {
        this.config.categories().forEach(category -> category.optionGroups().forEach(optionGroup -> optionGroup.getOptions().forEach(Option::reset)));
        for (OptionWidget widget : allOptionWidgets)
        {
            if (widget instanceof PixelGridAnimationWidget widget1)
            {
                widget1.reset();
            }
        }
    }

    private void defineOptions()
    {
        for (Category category : this.config.categories())
        {
            for (OptionGroup group : category.optionGroups())
            {
                for (Option<?> option : group.getOptions())
                {
                    option.setScreenInstance();
                }
            }
        }
    }
}
