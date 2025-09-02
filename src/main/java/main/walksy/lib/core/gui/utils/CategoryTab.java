package main.walksy.lib.core.gui.utils;

import net.minecraft.client.gui.tab.GridScreenTab;
import net.minecraft.text.Text;
import main.walksy.lib.core.config.local.Category;
import main.walksy.lib.core.gui.widgets.OptionGroupWidget;

import java.util.List;

public class CategoryTab extends GridScreenTab {

    private final List<OptionGroupWidget> optionGroupWidgets;
    private final Category category;

    public CategoryTab(Category category, List<OptionGroupWidget> optionGroupWidgets) {
        super(Text.literal(category.name()));
        this.optionGroupWidgets = optionGroupWidgets;
        this.category = category;
    }

    public List<OptionGroupWidget> getOptionGroupWidgets() {
        return optionGroupWidgets;
    }

    public Category getCategory()
    {
        return this.category;
    }
}
