package main.walksy.lib.core.config.local.options.groups;

import main.walksy.lib.core.config.local.Option;

import java.util.ArrayList;
import java.util.List;

public class OptionGroup {

    private final String name;
    private final List<Option<?>> options;
    private boolean isExpanded;

    private OptionGroup(final String name, final List<Option<?>> options, final boolean expanded) {
        this.name = name;
        this.options = options;
        this.isExpanded = expanded;
    }

    public String getName() {
        return this.name;
    }

    public List<Option<?>> getOptions() {
        return this.options;
    }

    public boolean isExpanded() {
        return this.isExpanded;
    }

    public void setExpanded(final boolean expanded) {
        this.isExpanded = expanded;
    }

    public void toggleExpanded() {
        this.isExpanded = !this.isExpanded;
    }

    public static Builder createBuilder(final String name) {
        return new Builder(name);
    }

    public static class Builder {

        private final String name;
        private final List<Option<?>> options = new ArrayList<>();
        private boolean expanded = true;

        public Builder(final String name) {
            this.name = name;
        }

        public Builder addOption(final Option<?> option) {
            this.options.add(option);
            return this;
        }

        public Builder setExpanded(final boolean expanded) {
            this.expanded = expanded;
            return this;
        }

        public OptionGroup build() {
            return new OptionGroup(this.name, this.options, this.expanded);
        }
    }
}
