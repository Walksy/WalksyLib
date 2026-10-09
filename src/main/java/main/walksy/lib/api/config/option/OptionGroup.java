package main.walksy.lib.api.config.option;

import java.util.ArrayList;
import java.util.List;

public final class OptionGroup {
    private final String name;
    private final List<Option<?>> options;
    private boolean expanded;

    private OptionGroup(final String name, final List<Option<?>> options, final boolean expanded) {
        this.name = name;
        this.options = List.copyOf(options);
        this.expanded = expanded;
    }

    public static Builder builder(final String name) {
        return new Builder(name);
    }

    public String getName() {
        return this.name;
    }

    public List<Option<?>> getOptions() {
        return this.options;
    }

    public boolean isExpanded() {
        return this.expanded;
    }

    public void setExpanded(final boolean expanded) {
        this.expanded = expanded;
    }

    public void toggleExpanded() {
        this.expanded = !this.expanded;
    }

    public static final class Builder {
        private final String name;
        private final List<Option<?>> options = new ArrayList<>();
        private boolean expanded = true;

        private Builder(final String name) {
            this.name = name;
        }

        public Builder option(final Option<?> option) {
            this.options.add(option);
            return this;
        }

        public Builder expanded(final boolean expanded) {
            this.expanded = expanded;
            return this;
        }

        public OptionGroup build() {
            return new OptionGroup(this.name, this.options, this.expanded);
        }
}
}
