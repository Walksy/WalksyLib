package walksy.lib.core.config.impl.options.groups;

import walksy.lib.core.config.impl.Option;

import java.util.ArrayList;
import java.util.List;

public class OptionGroup {

    private final String name;
    private final List<Option<?>> options;
    private boolean isExpanded;

    private OptionGroup(String name, List<Option<?>> options, boolean expanded) {
        this.name = name;
        this.options = options;
        this.isExpanded = expanded;
    }

    public String getName() {
        return name;
    }

    public List<Option<?>> getOptions() {
        return options;
    }

    public boolean isExpanded()
    {
        return this.isExpanded;
    }

    public void setExpanded(boolean expanded)
    {
        this.isExpanded = expanded;
    }

    public void toggleExpanded()
    {
        this.isExpanded = !this.isExpanded;
    }

    public static Builder createBuilder(String name) {
        return new Builder(name);
    }

    public static class Builder {

        private final String name;
        private final List<Option<?>> options = new ArrayList<>();
        private boolean expanded = true;

        public Builder(String name) {
            this.name = name;
        }

        public Builder addOption(Option<?> option) {
            options.add(option);
            return this;
        }

        public Builder setExpanded(boolean expanded)
        {
            this.expanded = expanded;
            return this;
        }

        public OptionGroup build() {
            return new OptionGroup(name, options, expanded);
        }
    }
}

