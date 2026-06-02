package main.walksy.lib.core.gui.utils;

import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import main.walksy.lib.core.config.local.Category;
import main.walksy.lib.core.gui.widgets.OptionGroupWidget;
import net.minecraft.network.chat.Component;

import java.util.List;

public class CategoryTab extends GridLayoutTab {

    private final List<OptionGroupWidget> optionGroupWidgets;
    private final Category category;

    public CategoryTab(final Category category, final List<OptionGroupWidget> optionGroupWidgets) {
        super(Component.literal(category.name()));
        this.optionGroupWidgets = optionGroupWidgets;
        this.category = category;
    }

    public List<OptionGroupWidget> getOptionGroupWidgets() {
        return this.optionGroupWidgets;
    }

    public Category getCategory() {
        return this.category;
    }
}
