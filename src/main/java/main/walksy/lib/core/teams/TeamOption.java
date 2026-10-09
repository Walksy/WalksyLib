package main.walksy.lib.core.teams;

import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.api.config.option.OptionDescription;
import main.walksy.lib.api.config.option.Options;
import main.walksy.lib.api.config.value.ListEntries;
import main.walksy.lib.api.config.value.WLColor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class TeamOption<T> {
    private final String name;
    private final T defaultValue;
    private final Map<String, T> values = new LinkedHashMap<>();
    private final Function<String, Option<T>> optionFactory;
    private OptionDescription description;

    private TeamOption(final String name, final T defaultValue, final Function<TeamOption<T>, Function<String, Option<T>>> factory) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.optionFactory = factory.apply(this);
    }

    public static TeamOption<Boolean> ofBoolean(final String name, final boolean defaultValue) {
        return new TeamOption<>(name, defaultValue, self -> team -> Options.ofBoolean(name, () -> self.get(team), defaultValue, v -> self.set(team, v)).description(self.description).build());
    }

    public static <T extends Number> TeamOption<T> ofNumber(final String name, final T defaultValue, final T min, final T max, final T increment) {
        return new TeamOption<>(name, defaultValue, self -> team -> Options.ofNumber(name, () -> self.get(team), defaultValue, v -> self.set(team, v)).range(min, max, increment).description(self.description).build());
    }

    public static TeamOption<String> ofString(final String name, final String defaultValue) {
        return new TeamOption<>(name, defaultValue, self -> team -> Options.ofString(name, () -> self.get(team), defaultValue, v -> self.set(team, v)).description(self.description).build());
    }

    public static TeamOption<WLColor> ofColor(final String name, final WLColor defaultValue) {
        return new TeamOption<>(name, defaultValue, self -> team -> Options.ofColor(name, () -> self.get(team), defaultValue, v -> self.set(team, v)).description(self.description).build());
    }

    public static <E extends Enum<E>> TeamOption<E> ofEnum(final String name, final E defaultValue, final Class<E> type) {
        return new TeamOption<>(name, defaultValue, self -> team -> Options.ofEnum(name, () -> self.get(team), defaultValue, v -> self.set(team, v), type).description(self.description).build());
    }

    public static TeamOption<List<String>> ofStringList(final String name, final List<String> defaultValue) {
        return new TeamOption<>(name, defaultValue, self -> team -> Options.ofStringList(name, () -> self.get(team), defaultValue, v -> self.set(team, v)).description(self.description).build());
    }

    public static TeamOption<List<String>> ofSelectionList(final String name, final List<String> defaultValue, final ListEntries entries) {
        return new TeamOption<>(name, defaultValue, self -> team -> Options.ofSelectionList(name, () -> self.get(team), defaultValue, v -> self.set(team, v), entries).description(self.description).build());
    }

    public TeamOption<T> description(final OptionDescription description) {
        this.description = description;
        return this;
    }

    public T get(final String team) {
        final T value = this.values.get(team);
        return value == null ? this.defaultValue : value;
    }

    public void set(final String team, final T value) {
        if (team == null) {
            return;
        }
        this.values.put(team, value);
    }

    public String name() {
        return this.name;
    }

    public T defaultValue() {
        return this.defaultValue;
    }

    public List<String> storedTeams() {
        return new ArrayList<>(this.values.keySet());
    }

    public Option<T> optionFor(final String team) {
        return this.optionFactory.apply(team);
    }

    public void renameTeam(final String from, final String to) {
        if (from == null || to == null || from.equals(to)) {
            return;
        }
        final T moved = this.values.remove(from);
        if (moved != null) {
            this.values.put(to, moved);
        }
    }

    public void removeTeam(final String team) {
        this.values.remove(team);
    }
}
