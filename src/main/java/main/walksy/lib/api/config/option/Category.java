package main.walksy.lib.api.config.option;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class Category {
    private final String name;
    private final List<OptionGroup> groups;
    private final List<Option<?>> options;
    private final boolean library;

    private Category(final String name, final List<OptionGroup> groups, final List<Option<?>> options, final boolean library) {
        this.name = name;
        this.groups = List.copyOf(groups);
        this.options = List.copyOf(options);
        this.library = library;
    }

    public boolean isLibrary() {
        return this.library;
    }

    public static Builder builder(final String name) {
        return new Builder(name);
    }

    public String name() {
        return this.name;
    }

    public List<OptionGroup> groups() {
        return this.groups;
    }

    public List<Option<?>> options() {
        return this.options;
    }

    public Stream<Option<?>> allOptions() {
        return Stream.concat(this.options.stream(), this.groups.stream().flatMap(group -> group.getOptions().stream())).flatMap(Option::allNested);
    }

    public static final class Builder {
        private final String name;
        private final List<OptionGroup> groups = new ArrayList<>();
        private final List<Option<?>> options = new ArrayList<>();
        private boolean library;

        private Builder(final String name) {
            this.name = name;
        }

        public Builder library() {
            this.library = true;
            return this;
        }

        public Builder group(final OptionGroup group) {
            this.groups.add(group);
            return this;
        }

        public Builder option(final Option<?> option) {
            this.options.add(option);
            return this;
        }

        public Category build() {
            final Category category = new Category(this.name, this.groups, this.options, this.library);
            category.allOptions().forEach(option -> option.stampSource(this.name, this.library));
            return category;
        }
}
}
