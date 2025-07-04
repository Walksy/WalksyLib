package walksy.lib.core.config.impl.options.groups;

import walksy.lib.core.config.impl.Option;

import java.util.ArrayList;
import java.util.List;

public class OptionGroup {

    private final String name;
    private final List<Option<?>> options;

    private OptionGroup(String name, List<Option<?>> options) {
        this.name = name;
        this.options = options;
    }

    public String getName() {
        return name;
    }

    public List<Option<?>> getOptions() {
        return options;
    }

    public static Builder createBuilder(String name) {
        return new Builder(name);
    }

    public static class Builder {

        private final String name;
        private final List<Option<?>> options = new ArrayList<>();

        public Builder(String name) {
            this.name = name;
        }

        public Builder addOption(Option<?> option) {
            options.add(option);
            return this;
        }

        public OptionGroup build() {
            return new OptionGroup(name, options);
        }
    }
}

