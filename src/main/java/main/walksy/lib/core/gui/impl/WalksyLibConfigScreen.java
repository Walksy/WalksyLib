package main.walksy.lib.core.gui.impl;

import main.walksy.lib.core.config.impl.ModConfig;
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
import main.walksy.lib.core.gui.Graphics;
import main.walksy.lib.core.utils.Animation;
import main.walksy.lib.core.utils.MainColors;
import main.walksy.lib.core.utils.ScreenGlobals;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class WalksyLibConfigScreen extends BaseScreen {
    private final TabManager tabManager;
    private final List<OptionGroupWidget> allGroupWidgets;
    private final List<OptionWidget> allOptionWidgets;
    private final List<CategoryTab> allTabs;
    public final Animation scrollAnim;
    private final String name;
    private final WalksyLibConfigManager configManager;
    private ScrollableTabWidget tabWidget;
    private ButtonWidget backButton, allModsButton, saveButton, resetButton, undoButton;
    private SearchBarWidget searchBar;
    private Option<?> focusedOption;
    private Graphics currentGraphicsContext;
    public PopUp popUp = null;
    private int maxScroll = 0;
    public boolean scroll = true;

    public WalksyLibConfigScreen(final Screen parent, final ModConfig config, final String title) {
        super(title + " Config Screen", parent);
        this.tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);
        this.allGroupWidgets = new ArrayList<>();
        this.allOptionWidgets = new ArrayList<>();
        this.allTabs = new ArrayList<>();
        this.scrollAnim = new Animation(0, 0.5F);
        this.name = title;
        this.configManager = new WalksyLibConfigManager(config);
        this.focusedOption = null;
        this.currentGraphicsContext = null;
    }

    @Override
    public void onClose() {
        if (this.popUp != null && !this.popUp.canClose()) {
            return;
        }

        if (this.shouldUndoOptions() && this.popUp == null) {
            this.popUp = new WarningPopUp(this, "You have unsaved changes!", "Are you sure you want to leave without saving?", () -> {
                this.undo();
                super.onClose();
                }, () -> {
                this.popUp.close();
            });
            return;
        }

        if (this.popUp != null) {
            this.popUp.close();
            return;
        }

        super.onClose();
        this.configManager.cleanCache();
        this.save(false);
    }

    @Override
    protected void init() {
        super.init();
        this.initButtons();
        this.initSearchBar();
        this.initTabs();
        this.rebuildWidgets();
        this.defineOptions();
        this.setOptionPrevs();
    }

    private void initButtons() {
        this.backButton = new ButtonWidget(8, 5, 50, 16, true, "Back", this::onClose);
        this.allModsButton = new ButtonWidget(this.width - 65, 5, 57, 16, true, "WalksyLib", () -> this.minecraft.setScreenAndShow(new APIScreen(this)));
        this.saveButton = new ButtonWidget(this.width - 58, this.height - 21, 50, 16, true, "Save", () -> this.save(true));
        this.resetButton = new ButtonWidget(this.width - 58 - 55, this.height - 21, 50, 16, true, "Reset", this::resetOptions);
        this.undoButton = new ButtonWidget(this.width - 58 - 110, this.height - 21, 50, 16, true, "Undo", this::undo);

        this.addRenderableWidget(this.backButton);
        this.addRenderableWidget(this.allModsButton);
        this.addRenderableWidget(this.saveButton);
        this.addRenderableWidget(this.resetButton);
        this.addRenderableWidget(this.undoButton);
    }

    private void initSearchBar() {
        this.searchBar = new SearchBarWidget(this, 6, this.height - 21, 150, 16, this::search);
        this.addRenderableWidget(this.searchBar);
    }

    private void initTabs() {
        this.updateScreenGlobals();
        final List<CategoryTab> tabList = new ArrayList<>();
        for (final Category category : this.configManager.get().categories()) {
            final List<OptionGroupWidget> groupWidgets = new ArrayList<>();
            int yOffset = 60;
            for (final OptionGroup group : category.optionGroups()) {
                final int groupH = ScreenGlobals.OPTION_HEIGHT;
                int groupHeight = groupH;
                final OptionGroupWidget groupWidget = new OptionGroupWidget(
                        (this.width - (ScreenGlobals.OPTION_PANEL_ENDX - ScreenGlobals.OPTION_PANEL_STARTX)) / 2,
                        yOffset, 150, groupH, group, this
                );
                if (group.isExpanded()) {
                    int childY = yOffset + groupH;
                    final List<OptionWidget> children = groupWidget.getChildren();
                    for (int i = 0; i < children.size(); i++) {
                        final OptionWidget child = children.get(i);
                        child.setPosition(child.getX(), childY);
                        childY += ScreenGlobals.OPTION_HEIGHT;
                        groupHeight += ScreenGlobals.OPTION_HEIGHT;
                        if (i < children.size() - 1) {
                            childY += ScreenGlobals.OPTION_GROUP_SEPARATION;
                            groupHeight += ScreenGlobals.OPTION_GROUP_SEPARATION;
                        }
                    }
                }
                groupWidget.setHeight(groupHeight);
                groupWidgets.add(groupWidget);
                this.allGroupWidgets.add(groupWidget);
                this.allOptionWidgets.addAll(groupWidget.getChildren());
                yOffset += groupHeight + 10;
            }
            final CategoryTab tab = new CategoryTab(category, groupWidgets);
            tabList.add(tab);
            this.allTabs.add(tab);
        }
        this.tabWidget = new ScrollableTabWidget(0, 27, this.width, 24, tabList, this.tabManager, TabLocation.TOP, this);
        this.addRenderableWidget(this.tabWidget);
        this.tabWidget.selectTab(0, true);
    }

    public void layoutGroupWidgets() {
        if (!(this.tabManager.getCurrentTab() instanceof CategoryTab categoryTab)) {
            return;
        }
        final List<OptionGroupWidget> widgets = categoryTab.getOptionGroupWidgets();
        int contentYOffset = 60;
        for (final OptionGroupWidget group : widgets) {
            if (!group.visible) {
                continue;
            }
            int groupHeight = ScreenGlobals.OPTION_HEIGHT;
            if (group.getGroup().isExpanded()) {
                final List<OptionWidget> children = group.getChildren();
                for (int i = 0; i < children.size(); i++) {
                    final OptionWidget child = children.get(i);
                    if (!child.isVisible()) continue;
                    groupHeight += this.getChildHeight(child);
                    if (this.hasNextVisible(children, i)) groupHeight += ScreenGlobals.OPTION_GROUP_SEPARATION;
                }
            }
            group.setHeight(groupHeight);
            contentYOffset += groupHeight + 10;
        }
        final int viewHeight = this.height - 120;
        this.maxScroll = Math.max(0, contentYOffset - (10 * widgets.size()) - viewHeight);
        if (this.scrollAnim.getTargetValue() > this.maxScroll) {
            this.scrollAnim.setTargetValue(Math.max(0, this.maxScroll));
        }
        int yOffset = (int) (60 - this.scrollAnim.getCurrentValue());
        for (final OptionGroupWidget group : widgets) {
            if (!group.visible) {
                continue;
            }
            group.setPosition((this.width - 150) / 2, yOffset);
            if (group.getGroup().isExpanded()) {
                int childY = yOffset + ScreenGlobals.OPTION_HEIGHT;
                final List<OptionWidget> children = group.getChildren();
                for (int i = 0; i < children.size(); i++) {
                    final OptionWidget child = children.get(i);
                    if (!child.isVisible()) {
                        continue;
                    }
                    final int childHeight = this.getChildHeight(child);
                    child.setPosition(child.getX(), childY);
                    child.setHeight(childHeight);
                    child.onWidgetUpdate(child.getX() + child.getWidth() - ScreenGlobals.OPTION_HEIGHT + 22, childY);
                    childY += childHeight;
                    if (this.hasNextVisible(children, i)) {
                        childY += ScreenGlobals.OPTION_GROUP_SEPARATION;
                    }
                }
            }
            yOffset += group.getHeight() + 10;
        }
    }

    @Override
    protected void extract(final Graphics graphics, final int mouseX, final int mouseY) {
        this.currentGraphicsContext = graphics;
        final GuiGraphicsExtractor extractor = graphics.extractor();
        this.suppressWidgetMouse = this.popUp != null;
        extractor.horizontalLine(0, this.width, 25, MainColors.OUTLINE_BLACK.getRGB());
        extractor.horizontalLine(0, this.width, 26, MainColors.OUTLINE_WHITE.getRGB());
        extractor.horizontalLine(0, this.width, this.height - 28, MainColors.OUTLINE_BLACK.getRGB());
        extractor.horizontalLine(0, this.width, this.height - 27, MainColors.OUTLINE_WHITE.getRGB());
        extractor.centeredText(this.font, this.name, this.width / 2, 12 - this.font.lineHeight / 2, -1);
        this.saveButton.setEnabled(this.shouldUndoOptions());
        this.saveButton.setTooltip(!this.saveButton.active ? Tooltip.create(Component.literal("No changes have occurred")) : null);
        this.resetButton.setEnabled(this.shouldResetOptions());
        this.undoButton.setEnabled(this.shouldUndoOptions());
        if (!this.isConfigEmpty()) {
            this.extractOptionPanel(graphics, this.focusedOption);
        }
        this.scrollAnim.update(this.delta, this::layoutGroupWidgets);
        if (this.isConfigEmpty()) {
            extractor.centeredText(this.getFont(), "No Available Options...", this.width / 2, this.height / 2, -1);
        }
        if (this.popUp != null) {
            extractor.fill(0, 0, this.width, this.height, new Color(0, 0, 0, 100).getRGB());
            this.popUp.extract(graphics, mouseX, mouseY, this.delta);
        }
    }


    private void extractOptionPanel(final Graphics graphics, final Option<?> option) {
        final GuiGraphicsExtractor extractor = graphics.extractor();
        final int startX = ScreenGlobals.OPTION_PANEL_STARTX;
        final int startY = ScreenGlobals.OPTION_PANEL_STARTY;
        final int endX = ScreenGlobals.OPTION_PANEL_ENDX;
        final int endY = ScreenGlobals.OPTION_PANEL_ENDY;
        graphics.fillRoundedRect(startX, startY, endX, endY, 2, new Color(0, 0, 0, 100).getRGB());
        graphics.fillRoundedRectOutline(startX, startY - 1, endX, endY, 2, 1, MainColors.OUTLINE_WHITE.getRGB());
        graphics.fillRoundedRectOutline(startX - 1, startY - 2, endX, endY + 2, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
        if (option != null) {
            this.extractOptionInfo(extractor, option, startX, startY, endX, endY);
        }
        if (this.popUp != null) {
            this.popUp.layout(this.popUp.width, this.popUp.height);
        }
    }

    private void extractOptionInfo(final GuiGraphicsExtractor extractor, final Option<?> option, final int startX, final int startY, final int endX, final int endY) {
        final List<FormattedText> nameLines = this.font.splitIgnoringLanguage(Component.literal(option.getName()), endX - startX - 20);
        final int lineSpacing = 2;
        final int totalNameHeight = (nameLines.size() * this.font.lineHeight) + ((nameLines.size() - 1) * lineSpacing);
        final int nameStartY = 66 - (totalNameHeight / 2) + 4;
        final int nameYBias = nameLines.size() > 1 ? 4 : 0;
        for (int i = 0; i < nameLines.size(); i++) {
            extractor.centeredText(this.font,
                    nameLines.get(i).getString(),
                    (startX + endX) / 2,
                    nameStartY + (i * (this.font.lineHeight + lineSpacing)) + nameYBias,
                    -1
            );
        }
        final int separatorY = nameStartY + totalNameHeight + 4;
        extractor.horizontalLine(startX + 1, endX, separatorY, MainColors.OUTLINE_WHITE.getRGB());
        final OptionDescription desc = option.getDescription();
        if (desc == null) {
            extractor.centeredText(this.font, "No Description", (startX + endX) / 2, separatorY + 8, -1);
            return;
        }
        switch (desc.getType()) {
            case TEXT -> this.extractTextDescription(extractor, desc, startX, endX, separatorY);
            case RENDER -> {
                extractor.enableScissor(startX, startY, startX + endX, startY + endY);
                desc.getRenderConsumer().accept(extractor, new OptionDescription.OptionPanel(startX, startY, endX, endY));
                extractor.disableScissor();
            }
        }
    }

    private void extractTextDescription(final GuiGraphicsExtractor extractor, final OptionDescription desc, final int startX, final int endX, final int separatorY) {
        final List<FormattedText> lines = this.font.splitIgnoringLanguage(
                Component.literal(desc.getStringSupplier().get()), endX - startX - 10
        );
        int descY = separatorY + 8;
        for (final FormattedText line : lines) {
            extractor.text(
                    this.font,
                    line.getString(),
                    startX + ((endX - startX) - this.font.width(line)) / 2,
                    descY,
                    new Color(182, 182, 182).getRGB()
            );
            descY += this.font.lineHeight + 2;
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent click, final boolean doubled) {
        if (this.popUp != null) {
            this.popUp.onClick(click, doubled);
        } else if (!this.tabWidget.isHoveringOverAnyTab(click.x(), click.y())) {
            this.searchBar.setFocused(this.searchBar.isHovered());
            ((ScreenAccessor) this).getDrawables().forEach(w -> {
                if (w instanceof OptionGroupWidget optionGroupWidget) {
                    optionGroupWidget.onMouseClick(click, doubled);
                } else if (w instanceof OptionWidget optionWidget) {
                    if (optionWidget.isVisible() && optionWidget.isInScissor(0, 49, this.width, this.height - 28) && optionWidget.isAvailable()) {
                        optionWidget.onMouseClick(click, doubled);
                    }
                    if (optionWidget.resetButton.active) {
                        optionWidget.resetButton.onClick(click, doubled);
                    }
                }
            });
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent click) {
        if (this.popUp != null) {
            this.popUp.onMouseRelease(click);
        }
        ((ScreenAccessor) this).getDrawables().forEach(w -> {
            if (w instanceof OptionWidget optionWidget) {
                optionWidget.onMouseRelease(click);
            }
        });
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent click, final double offsetX, final double offsetY) {
        if (this.popUp == null) {
            ((ScreenAccessor) this).getDrawables().forEach(w -> {
                if (w instanceof OptionWidget optionWidget && optionWidget.isAvailable()) {
                    optionWidget.onMouseDrag(click, offsetX, offsetY);
                }
            });
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public void mouseMoved(final double mouseX, final double mouseY) {
        if (this.popUp == null) {
            ((ScreenAccessor) this).getDrawables().forEach(w -> {
                if (w instanceof OptionWidget optionWidget && optionWidget.isAvailable()) {
                    optionWidget.onMouseMove(mouseX, mouseY);
                }
            });
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double horizontalAmount, final double verticalAmount) {
        if (this.popUp != null) {
            this.popUp.onScroll(mouseX, mouseY, verticalAmount);
        } else if (this.scroll && !this.tabWidget.isHoveringOverAnyTab(mouseX, mouseY)) {
            final float newTarget = Math.max(0, Math.min(this.scrollAnim.getTargetValue() - (float) (verticalAmount * 20), this.maxScroll));
            this.scrollAnim.setTargetValue(newTarget);
        } else {
            ((ScreenAccessor) this).getDrawables().forEach(w -> {
                if (w instanceof OptionWidget optionWidget && optionWidget.isAvailable()) {
                    optionWidget.onMouseScroll(mouseX, mouseY, verticalAmount);
                }
            });
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(final KeyEvent input) {
        if (this.popUp == null) {
            ((ScreenAccessor) this).getDrawables().forEach(w -> {
                if (w instanceof OptionWidget optionWidget && optionWidget.isAvailable()) {
                    optionWidget.onKeyPress(input);
                }
            });
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(final CharacterEvent input) {
        if (this.popUp == null) {
            ((ScreenAccessor) this).getDrawables().forEach(w -> {
                if (w instanceof OptionWidget optionWidget && optionWidget.isAvailable()) {
                    optionWidget.onCharTyped(input);
                }
            });
        }
        return super.charTyped(input);
    }

    @Override
    protected void rebuildWidgets() {
        if (this.tabWidget != null) {
            this.tabWidget.setWidth(this.width);
            this.tabWidget.setPosition(0, 27);
            final int i = this.tabWidget.getRectangle().bottom();
            this.tabManager.setTabArea(new ScreenRectangle(0, i, this.width, this.height - 36 - i));
            this.layoutGroupWidgets();
        }
        this.backButton.setPosition(8, 5);
        this.allModsButton.setPosition(this.width - 65, 5);
        this.saveButton.setPosition(this.width - 58, this.height - 21);
        this.resetButton.setPosition(this.width - 58 - 55, this.height - 21);
        this.undoButton.setPosition(this.width - 58 - 110, this.height - 21);
        this.searchBar.setPosition(6, this.height - 21);
        this.updateScreenGlobals();
        for (final OptionWidget widget : this.allOptionWidgets) {
            widget.setWidth(ScreenGlobals.OPTION_WIDTH);
            widget.onWidgetUpdate(widget.getWidth() - ScreenGlobals.OPTION_HEIGHT + 15 + 22, widget.getY());
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
        for (final OptionWidget widget : this.allOptionWidgets) {
            widget.tick();
        }
    }

    public void showWidgetsForCategory(final Category category) {
        final CategoryTab selected = (CategoryTab) this.tabManager.getCurrentTab();
        if (selected == null || !selected.getCategory().name().equalsIgnoreCase(category.name())) {
            return;
        }
        this.tabWidget.updateVisibleWidgetsForTab(selected);
        this.scrollAnim.setTargetValue(0);
    }

    public void setFocusedOption(final Option<?> option) {
        if (option != this.focusedOption) {
            this.focusedOption = option;
        }
    }

    public void search(final String query) {
        this.allOptionWidgets.forEach(w -> w.updateSearchQuery(query));
        this.allGroupWidgets.forEach(w -> w.updateSearchQuery(query));
        for (final OptionGroupWidget group : this.allGroupWidgets) {
            group.visible = group.getChildren().stream().anyMatch(w -> w.getOption().searched());
            if (group.searched(false)) {
                group.visible = true;
                group.getGroup().setExpanded(true);
                group.getChildren().forEach(w -> w.updateSearchQuery(""));
            }
        }
        final String queryLower = query.toLowerCase();
        final List<CategoryTab> filtered = this.allTabs.stream()
                .filter(tab -> {
                    final boolean categoryMatches = tab.getCategory().name().toLowerCase().contains(queryLower);
                    final boolean optionMatches = tab.getCategory().optionGroups().stream()
                            .flatMap(g -> g.getOptions().stream())
                            .anyMatch(Option::searched);
                    return categoryMatches || optionMatches;
                })
                .toList();

        this.tabWidget.setTabs(filtered);
        if (this.tabWidget.tabSize() != this.allTabs.size()) {
            this.tabWidget.selectTab(0, true);
        }
        this.layoutGroupWidgets();
    }

    public void onChangesMade(final Option<?> option) {}

    public void save(final boolean runSave) {
        this.setOptionPrevs();
        this.configManager.get().onSave();
        if (runSave) {
            this.configManager.get().runSave();
        }
        this.defineOptions();
    }

    public void setOptionPrevs() {
        this.forEachOption(o -> o.setPrev(this.name));
    }

    public void undo() {
        this.forEachOption(Option::undo);
        for (final OptionWidget widget : this.allOptionWidgets) {
            widget.onThirdPartyChange(widget.getOption().screenInstanceValue);
            if (widget instanceof PixelGridAnimationWidget w) w.reset();
        }
        this.setOptionPrevs();
    }

    public void resetOptions() {
        this.forEachOption(Option::reset);
        for (final OptionWidget widget : this.allOptionWidgets) {
            widget.onThirdPartyChange(widget.getOption().getDefaultValue());
            switch (widget) {
                case PixelGridAnimationWidget w -> w.reset();
                case StringListOptionWidget w -> w.setHeight();
                case SpriteOptionWidget w -> w.reCalc();
                default -> {
                }
            }
        }
        this.setOptionPrevs();
    }

    private boolean isConfigEmpty() {
        return this.configManager.get().categories().isEmpty();
    }

    private boolean shouldResetOptions() {
        return this.anyOption(Option::hasChanged);
    }

    private boolean shouldUndoOptions() {
        return this.anyOption(o -> !o.screenInstanceCheck());
    }

    private void defineOptions() {
        this.forEachOption(Option::setScreenInstance);
    }

    private void updateScreenGlobals() {
        ScreenGlobals.OPTION_PANEL_STARTX = (int) (this.width * 0.75);
        ScreenGlobals.OPTION_PANEL_STARTY = 61;
        ScreenGlobals.OPTION_PANEL_ENDX = this.width;
        ScreenGlobals.OPTION_PANEL_ENDY = this.height - 120;
        ScreenGlobals.OPTION_WIDTH = ScreenGlobals.OPTION_PANEL_STARTX - 30 - 22;
    }

    private void forEachOption(final Consumer<Option<?>> action) {
        this.configManager.get().categories()
                .forEach(c -> c.optionGroups()
                .forEach(g -> g.getOptions()
                .forEach(action)));
    }

    private boolean anyOption(final Predicate<Option<?>> predicate) {
        return this.configManager.get().categories().stream()
                .flatMap(c -> c.optionGroups().stream())
                .flatMap(g -> g.getOptions().stream())
                .anyMatch(predicate);
    }

    private boolean hasNextVisible(final List<OptionWidget> children, final int index) {
        for (int j = index + 1; j < children.size(); j++) {
            if (children.get(j).isVisible()) return true;
        }
        return false;
    }

    private int getChildHeight(final OptionWidget child) {
        if (child instanceof OpenableWidget oW) {
            return (int) oW.getCurrentHeight();
        }
        if (child instanceof StringListOptionWidget slw) {
            return ScreenGlobals.OPTION_HEIGHT + slw.ADDITIONAL_HEIGHT;
        }
        return ScreenGlobals.OPTION_HEIGHT;
    }

    public Graphics currentGraphicsContext() {
        return this.currentGraphicsContext;
    }
}
