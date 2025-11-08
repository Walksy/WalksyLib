package main.walksy.lib.core.gui.impl;

import main.walksy.lib.core.config.impl.LocalConfig;
import main.walksy.lib.core.config.local.Category;
import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.OptionDescription;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.gui.popup.PopUp;
import main.walksy.lib.core.gui.popup.impl.WarningPopUp;
import main.walksy.lib.core.gui.utils.CategoryTab;
import main.walksy.lib.core.gui.utils.TabLocation;
import main.walksy.lib.core.gui.widgets.*;
import main.walksy.lib.core.manager.WalksyLibConfigManager;
import main.walksy.lib.core.mixin.ScreenAccessor;
import main.walksy.lib.core.renderer.Renderer2D;
import main.walksy.lib.core.utils.Animation;
import main.walksy.lib.core.utils.MainColors;
import main.walksy.lib.core.utils.ScreenGlobals;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tab.TabManager;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class WalksyLibConfigScreen extends BaseScreen {
    private final TabManager tabManager = new TabManager(this::addDrawableChild, this::remove);
    private final List<OptionGroupWidget> allGroupWidgets = new ArrayList<>();
    private final List<OptionWidget> allOptionWidgets = new ArrayList<>();
    private final List<CategoryTab> allTabs = new ArrayList<>();
    public final Animation scrollAnim = new Animation(0, 0.5F);
    private final WalksyLibConfigManager configManager;
    private ScrollableTabWidget tabWidget;
    private ButtonWidget backButton, allModsButton, saveButton, resetButton, undoButton;
    private SearchBarWidget searchBar;
    private Option<?> focusedOption;
    public PopUp popUp = null;
    private int maxScroll = 0;
    public boolean scroll = true;

    public WalksyLibConfigScreen(Screen parent, LocalConfig config) {
        super(config.name() + " Config Screen", parent);
        this.configManager = new WalksyLibConfigManager(config);
        this.focusedOption = null;
    }

    @Override
    public void close() {
        if (popUp != null && !popUp.canClose()) {
            return;
        }

        if (shouldUndoOptions() && popUp == null) {
            popUp = new WarningPopUp(
                    this,
                    "You have unsaved changes!",
                    "Are you sure you want to leave without saving?",
                    () -> {
                        undo();
                        super.close();
                    },
                    () -> popUp.close()
            );
            return;
        }

        if (popUp != null) {
            popUp.close();
            return;
        }

        super.close();
        this.configManager.cleanCache();
        save(false); //saves option group states
    }


    @Override
    protected void init() {
        super.init();
        initButtons();
        initSearchBar();
        initTabs();
        refreshWidgetPositions();
        this.defineOptions();
        this.setOptionPrevs();
    }

    private void initButtons() {
        backButton = new ButtonWidget(8, 5, 50, 16, true, "Back", this::close);
        allModsButton = new ButtonWidget(width - 65, 5, 57, 16, true, "WalksyLib", () -> client.setScreen(new APIScreen(this)));

        saveButton = new ButtonWidget(width - 58, height - 21, 50, 16, true, "Save", () -> this.save(true));
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

        for (Category category : configManager.get().categories()) {
            List<OptionGroupWidget> groupWidgets = new ArrayList<>();
            int yOffset = 60;

            for (OptionGroup group : category.optionGroups()) {
                int groupH = ScreenGlobals.OPTION_HEIGHT;
                int optionHeight = ScreenGlobals.OPTION_HEIGHT;

                int groupHeight = groupH;

                ScreenGlobals.OPTION_PANEL_STARTX = (int) (width * 0.75);
                ScreenGlobals.OPTION_PANEL_STARTY = 61;
                ScreenGlobals.OPTION_PANEL_ENDX = width;
                ScreenGlobals.OPTION_PANEL_ENDY = height - 120;
                ScreenGlobals.OPTION_WIDTH = ScreenGlobals.OPTION_PANEL_STARTX - 30 - 22;

                OptionGroupWidget groupWidget = new OptionGroupWidget((width - (ScreenGlobals.OPTION_PANEL_ENDX - ScreenGlobals.OPTION_PANEL_STARTX)) / 2, yOffset, 150, groupH, group, this);

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
                            childY += ScreenGlobals.OPTION_GROUP_SEPARATION;
                            groupHeight += ScreenGlobals.OPTION_GROUP_SEPARATION;
                        }
                    }
                }


                groupWidget.setHeight(groupHeight);
                groupWidgets.add(groupWidget);
                allGroupWidgets.add(groupWidget);
                allOptionWidgets.addAll(groupWidget.getChildren());

                yOffset += groupHeight + 10;
            }

            CategoryTab tab = new CategoryTab(category, groupWidgets);
            tabList.add(tab);
            allTabs.add(tab);
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

            int groupHeight = ScreenGlobals.OPTION_HEIGHT;

            if (group.getGroup().isExpanded()) {
                List<OptionWidget> children = group.getChildren();
                for (int i = 0; i < children.size(); i++) {
                    OptionWidget child = children.get(i);
                    if (!child.isVisible()) continue;

                    int childHeight = ScreenGlobals.OPTION_HEIGHT;

                    if (child instanceof OpenableWidget oW) {
                        childHeight = (int) oW.getCurrentHeight();
                    } else if (child instanceof StringListOptionWidget slw) {
                        childHeight += slw.ADDITIONAL_HEIGHT;
                    }
                    groupHeight += childHeight;

                    boolean hasNextVisible = false;
                    for (int j = i + 1; j < children.size(); j++) {
                        if (children.get(j).isVisible()) {
                            hasNextVisible = true;
                            break;
                        }
                    }
                    if (hasNextVisible) groupHeight += ScreenGlobals.OPTION_GROUP_SEPARATION;
                }
            }

            group.setHeight(groupHeight);
            contentYOffset += groupHeight + 10;
        }

        int viewHeight = height - 120;
        maxScroll = Math.max(0, contentYOffset - (10 * widgets.size()) - viewHeight);
        if (scrollAnim.getTargetValue() > maxScroll) {
            scrollAnim.setTargetValue(Math.max(0, maxScroll)); //stop the user from getting stuck below the maxScroll offset
        }
        int yOffset = (int) (60 - scrollAnim.getCurrentValue());
        for (OptionGroupWidget group : widgets) {
            if (!group.visible) continue;

            group.setPosition((width - 150) / 2, yOffset);

            if (group.getGroup().isExpanded()) {
                int childY = yOffset + ScreenGlobals.OPTION_HEIGHT;
                List<OptionWidget> children = group.getChildren();

                for (int i = 0; i < children.size(); i++) {
                    OptionWidget child = children.get(i);
                    if (!child.isVisible()) continue;

                    int childHeight = ScreenGlobals.OPTION_HEIGHT;

                    if (child instanceof OpenableWidget oW) {
                        childHeight = (int) oW.getCurrentHeight();
                    } else if (child instanceof StringListOptionWidget slw) {
                        childHeight += slw.ADDITIONAL_HEIGHT;
                    }
                    child.setPosition(child.getX(), childY);
                    child.setHeight(childHeight);
                    int size = ScreenGlobals.OPTION_HEIGHT;
                    child.onWidgetUpdate(child.getX() + child.getWidth() - size + 22, childY);
                    childY += childHeight;

                    boolean hasNextVisible = false;
                    for (int j = i + 1; j < children.size(); j++) {
                        if (children.get(j).isVisible()) {
                            hasNextVisible = true;
                            break;
                        }
                    }
                    if (hasNextVisible) childY += ScreenGlobals.OPTION_GROUP_SEPARATION;
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
        this.scrollAnim.update(delta, this::layoutGroupWidgets); //this could cause some performance issues
        if (this.isConfigEmpty()) {
            context.drawCenteredTextWithShadow(this.getTextRenderer(), "No Available Options...", this.width / 2, this.height / 2, -1);
        }
        if (popUp != null)
        {
            Renderer2D.startPopUpRender(context, 1, width, height);
            popUp.render(context, mouseX, mouseY, delta);
            Renderer2D.endPopUpRender(context);
        }
    }

    private void render(DrawContext context) {
        //context.drawTexture(RenderLayer::getGuiTextured, FOOTER_SEPARATOR_TEXTURE, 0, 25, 0.0F, 0.0F, width, 2, 32, 2);
        context.drawHorizontalLine(0, width, 25, MainColors.OUTLINE_BLACK.getRGB());
        context.drawHorizontalLine(0, width, 26, MainColors.OUTLINE_WHITE.getRGB());

        context.drawHorizontalLine(0, width, height - 28, MainColors.OUTLINE_BLACK.getRGB());
        context.drawHorizontalLine(0, width, height - 27, MainColors.OUTLINE_WHITE.getRGB());
        context.drawCenteredTextWithShadow(textRenderer, configManager.get().name(), width / 2, 12 - textRenderer.fontHeight / 2, -1);

        ScreenGlobals.OPTION_PANEL_STARTX = (int) (width * 0.75);
        ScreenGlobals.OPTION_PANEL_STARTY = 61;
        ScreenGlobals.OPTION_PANEL_ENDX = width;
        ScreenGlobals.OPTION_PANEL_ENDY = height - 120;
        this.saveButton.setEnabled(this.shouldUndoOptions());
        this.saveButton.setTooltip(!this.saveButton.active ? Tooltip.of(Text.of("No changes have occurred")) : null);
        this.resetButton.setEnabled(this.shouldResetOptions());
        this.undoButton.setEnabled(this.shouldUndoOptions());
        if (!this.isConfigEmpty()) {
            this.renderOptionPanel(context, this.focusedOption);
        }
    }

    private void renderOptionPanel(DrawContext context, Option<?> option) {
        int startX = ScreenGlobals.OPTION_PANEL_STARTX;
        int startY = ScreenGlobals.OPTION_PANEL_STARTY;
        int endX = ScreenGlobals.OPTION_PANEL_ENDX;
        int endY = ScreenGlobals.OPTION_PANEL_ENDY;

        Renderer2D.fillRoundedRect(context, startX, startY, endX, endY, 2, new Color(0, 0, 0, 100).getRGB());
        Renderer2D.fillRoundedRectOutline(context, startX, startY - 1, endX, endY, 2, 1, MainColors.OUTLINE_WHITE.getRGB());
        Renderer2D.fillRoundedRectOutline(context, startX - 1, startY - 2, endX, endY + 2, 2, 1, MainColors.OUTLINE_BLACK.getRGB());

        if (option != null) {
            String optionName = option.getName();
            int maxTextWidth = endX - startX - 20;

            List<OrderedText> nameLines = this.textRenderer.wrapLines(Text.of(optionName), maxTextWidth);

            int lineSpacing = 2;
            int totalNameHeight = (nameLines.size() * this.textRenderer.fontHeight) + ((nameLines.size() - 1) * lineSpacing);

            int centerY = 66;
            int nameStartY = centerY - (totalNameHeight / 2) + 4;
            int z = nameLines.size() > 1 ? 4 : 0;

            for (int i = 0; i < nameLines.size(); i++) {
                int lineY = nameStartY + (i * (this.textRenderer.fontHeight + lineSpacing));
                context.drawCenteredTextWithShadow(
                        this.textRenderer,
                        nameLines.get(i),
                        (startX + endX) / 2,
                        lineY + z,
                        -1
                );
            }

            int lineY = nameStartY + totalNameHeight + 4;
            context.drawHorizontalLine(startX + 1, endX, lineY, MainColors.OUTLINE_WHITE.getRGB());

            OptionDescription desc = option.getDescription();
            if (desc != null) {
                switch (desc.getType()) {
                    case TEXT -> {
                        String description = desc.getStringSupplier().get();
                        int descStartY = lineY + 8;
                        int maxWidth = endX - startX - 10;

                        List<OrderedText> lines = textRenderer.wrapLines(Text.of(description), maxWidth);

                        for (OrderedText line : lines) {
                            int lineWidth = textRenderer.getWidth(line);
                            int descStartX = startX + ((endX - startX) - lineWidth) / 2;

                            context.drawTextWithShadow(
                                    textRenderer,
                                    line,
                                    descStartX,
                                    descStartY,
                                    new Color(182, 182, 182).getRGB()
                            );

                            descStartY += textRenderer.fontHeight + 2;
                        }
                    }


                    case RENDER -> {
                        context.enableScissor(startX, startY, startX + endX, startY + endY);
                        desc.getRenderConsumer().accept(
                                context,
                                new OptionDescription.OptionPanel(startX, startY, endX, endY)
                        );
                        context.disableScissor();
                    }
                }
            } else {
                context.drawCenteredTextWithShadow(
                        textRenderer,
                        "No Description",
                        (startX + endX) / 2,
                        lineY + 8,
                        -1
                );
            }
        }

        if (popUp != null) {
            popUp.layout(popUp.width, popUp.height);
        }
    }




    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (popUp != null)
        {
            popUp.onClick(mouseX, mouseY, button);
        } else if (!tabWidget.isHoveringOverAnyTab(mouseX, mouseY)) {

            if (!searchBar.isHovered())
            {
                searchBar.setFocused(false);
            }

            ((ScreenAccessor) this).getDrawables().forEach(w ->
            {
                if (w instanceof OptionGroupWidget optionGroupWidget) {
                    /**
                     * Figure out why the ClickableWidget::mouseClicked method isn't functioning properly ->
                     * On click (when hovered) option groups don't get toggled
                     */
                    optionGroupWidget.onMouseClick(mouseX, mouseY, button);
                } else if (w instanceof OptionWidget optionWidget) {
                    if (optionWidget.isVisible() && optionWidget.isInScissor(0, 49, width, height - 28) && optionWidget.isAvailable()) {
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
                } else if (w instanceof OptionWidget optionWidget && optionWidget.isAvailable()) {
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
                } else if (w instanceof OptionWidget optionWidget && optionWidget.isAvailable()) {
                    optionWidget.onMouseMove(mouseX, mouseY);
                }
            });
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (popUp == null && scroll) {
            float newTarget = scrollAnim.getTargetValue() - (float) (verticalAmount * 20);
            newTarget = Math.max(0, Math.min(newTarget, maxScroll));
            scrollAnim.setTargetValue(newTarget);
        } else if (popUp == null) {
            ((ScreenAccessor) this).getDrawables().forEach(w -> {
                if (w instanceof OptionWidget optionWidget && optionWidget.isAvailable()) {
                    optionWidget.onMouseScroll(mouseX, mouseY, verticalAmount);
                }
            });
        } else {
            popUp.onScroll(mouseX, mouseY, verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }


    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (popUp == null)
        {
            ((ScreenAccessor) this).getDrawables().forEach(w ->
            {
                if (w instanceof OptionGroupWidget optionGroupWidget) {
                } else if (w instanceof OptionWidget optionWidget && optionWidget.isAvailable()) {
                    optionWidget.onKeyPress(keyCode, scanCode, modifiers);
                }
            });
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (popUp == null)
        {
            ((ScreenAccessor) this).getDrawables().forEach(w ->
            {
                if (w instanceof OptionGroupWidget optionGroupWidget) {
                } else if (w instanceof OptionWidget optionWidget && optionWidget.isAvailable()) {
                    optionWidget.onCharTyped(chr, modifiers);
                }
            });
        }
        return super.charTyped(chr, modifiers);
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
        allModsButton.setPosition(width - 65, 5);
        saveButton.setPosition(width - 58, height - 21);
        resetButton.setPosition(width - 58 - 55, height - 21);
        undoButton.setPosition(width - 58 - 110, height - 21);
        searchBar.setPosition(6, height - 21);

        ScreenGlobals.OPTION_PANEL_STARTX = (int) (width * 0.75);
        ScreenGlobals.OPTION_PANEL_STARTY = 61;
        ScreenGlobals.OPTION_PANEL_ENDX = width;
        ScreenGlobals.OPTION_PANEL_ENDY = height - 120;
        ScreenGlobals.OPTION_WIDTH = ScreenGlobals.OPTION_PANEL_STARTX - 30 - 22;
        for (OptionWidget widget : allOptionWidgets) {
            widget.setWidth(ScreenGlobals.OPTION_WIDTH);
            int size = ScreenGlobals.OPTION_HEIGHT;
            widget.onWidgetUpdate(widget.getWidth() - size + 15 + 22, widget.getY());
            if (widget instanceof OpenableWidget openableWidget && openableWidget.open) {
                openableWidget.setHeight(openableWidget.OPEN_HEIGHT);
            } else if (widget instanceof StringListOptionWidget stringListOptionWidget) {
                stringListOptionWidget.setHeight(ScreenGlobals.OPTION_HEIGHT + stringListOptionWidget.ADDITIONAL_HEIGHT);
            } else {
                widget.setHeight(ScreenGlobals.OPTION_HEIGHT);
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
    }

    @Override
    protected void applyBlur() {}

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    public void showWidgetsForCategory(Category category) {
        CategoryTab selected = (CategoryTab) tabManager.getCurrentTab();
        if (selected == null || !selected.getCategory().name().equalsIgnoreCase(category.name())) return;
        tabWidget.updateVisibleWidgetsForTab(selected);
        this.scrollAnim.setTargetValue(0);
    }

    private boolean isConfigEmpty() {
        return (configManager.get().categories().isEmpty());
    }

    public void setFocusedOption(Option<?> option)
    {
        if (option != focusedOption) {
            this.focusedOption = option;
        }
    }

    public void search(String query) {
        allOptionWidgets.forEach(w -> w.updateSearchQuery(query));
        allGroupWidgets.forEach(w -> w.updateSearchQuery(query));

        for (OptionGroupWidget group : allGroupWidgets) {
            group.visible = group.getChildren().stream().anyMatch(optionWidget -> optionWidget.getOption().searched());

            if (group.searched(false)) {
                group.visible = true;
                group.getGroup().setExpanded(true);
                group.getChildren().forEach(w -> w.updateSearchQuery(""));
            }
        }

        String queryLower = query.toLowerCase();

        List<CategoryTab> filtered = allTabs.stream()
                .filter(tab -> {
                    boolean categoryMatches = tab.getCategory().name().toLowerCase().contains(queryLower);
                    boolean optionMatches = tab.getCategory().optionGroups().stream()
                            .flatMap(group -> group.getOptions().stream())
                            .anyMatch(Option::searched);
                    return categoryMatches || optionMatches;
                })
                .toList();

        tabWidget.setTabs(filtered);

        if (tabWidget.tabSize() != allTabs.size()) {
            tabWidget.selectTab(0, true);
        }

        layoutGroupWidgets();
    }



    public void onChangesMade(Option<?> option) {

    }

    public void save(boolean runSave)
    {
        this.setOptionPrevs();
        this.configManager.get().save();
        if (runSave) {
            this.configManager.get().runSave();
        }
        this.defineOptions();
    }

    public void setOptionPrevs()
    {
        this.configManager.get().categories().forEach(category -> category.optionGroups().forEach(optionGroup -> optionGroup.getOptions().forEach(option -> option.setPrev(configManager.get()))));
    }

    private boolean shouldResetOptions() {
        for (Category category : this.configManager.get().categories()) {
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
        for (Category category : this.configManager.get().categories()) {
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
        this.configManager.get().categories().forEach(category -> category.optionGroups().forEach(optionGroup -> optionGroup.getOptions().forEach(Option::undo)));
        for (OptionWidget widget : allOptionWidgets)
        {
            widget.onThirdPartyChange(widget.getOption().screenInstanceValue);
            if (widget instanceof PixelGridAnimationWidget widget1)
            {
                widget1.reset();
            }
        }
        this.setOptionPrevs();
    }

    public void resetOptions()
    {
        this.configManager.get().categories().forEach(category -> category.optionGroups().forEach(optionGroup -> optionGroup.getOptions().forEach(Option::reset)));
        for (OptionWidget widget : allOptionWidgets)
        {
            widget.onThirdPartyChange(widget.getOption().getDefaultValue());
            if (widget instanceof PixelGridAnimationWidget widget1)
            {
                widget1.reset();
            } else if (widget instanceof StringListOptionWidget widget1)
            {
                widget1.setHeight();
            } else if (widget instanceof SpriteOptionWidget widget1)
            {
                widget1.reCalc();
            }
        }
        this.setOptionPrevs();
    }

    private void defineOptions()
    {
        for (Category category : this.configManager.get().categories())
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

    public WalksyLibConfigManager getConfigManager () {
        return this.configManager;
    }
}
