package main.walksy.lib.core.gui.widget.options;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.api.config.option.Category;
import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.api.config.option.OptionGroup;
import main.walksy.lib.api.config.value.CroppedTexture;
import main.walksy.lib.api.config.value.ItemInventory;
import main.walksy.lib.api.config.value.ModLinks;
import main.walksy.lib.api.config.value.PixelGridAnimation;
import main.walksy.lib.api.config.value.Preview;
import main.walksy.lib.api.config.value.WLColor;
import main.walksy.lib.api.config.value.WLKeyMapping;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.widget.ScrollPanel;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;
import java.util.function.BooleanSupplier;
import main.walksy.lib.core.gui.widget.options.types.BooleanOptionWidget;
import main.walksy.lib.core.gui.widget.options.types.ButtonOptionWidget;
import main.walksy.lib.core.gui.widget.options.types.ColorOptionWidget;
import main.walksy.lib.core.gui.widget.options.types.EnumOptionWidget;
import main.walksy.lib.core.gui.widget.options.types.HudSpaceOptionWidget;
import main.walksy.lib.core.gui.widget.options.types.PreviewOptionWidget;
import main.walksy.lib.core.gui.widget.options.types.InventoryOptionWidget;
import main.walksy.lib.core.gui.widget.options.types.KeybindOptionWidget;
import main.walksy.lib.core.gui.widget.options.types.ModLinksOptionWidget;
import main.walksy.lib.core.gui.widget.options.types.NumberOptionWidget;
import main.walksy.lib.core.gui.widget.options.types.PageListOptionWidget;
import main.walksy.lib.core.gui.widget.options.types.CroppedTextureOptionWidget;
import main.walksy.lib.core.gui.widget.options.types.PixelGridAnimationOptionWidget;
import main.walksy.lib.core.gui.widget.options.types.StringListOptionWidget;
import main.walksy.lib.core.gui.widget.options.types.StringOptionWidget;
import main.walksy.lib.core.gui.widget.options.types.TextureOptionWidget;

public final class OptionWidgetFactory {
    private static final int GROUP_HEADER_HEIGHT_BASE = 18;
    public static final int ROW_HEIGHT_BASE = 22;
    private static final int GROUP_GAP_BASE = 14;
    private static final int ROW_GAP_BASE = 0;
    private static final int LOOSE_ROW_GAP_BASE = 2;
    private static final String HIDDEN_FALLBACK_REASON = "";

    private OptionWidgetFactory() {
    }

    public static void populate(final ScrollPanel scroll, final Category category, final RowContext ctx) {
        populate(scroll, category, ctx, null);
    }

    public static void populate(final ScrollPanel scroll, final Category category, final RowContext ctx, final ScrollPanel.ChildFactory<? extends AbstractWidget> emptyState) {
        populate(scroll, category.groups(), category.options(), ctx, emptyState);
    }

    public static void populateGroups(final ScrollPanel scroll, final List<OptionGroup> groups, final RowContext ctx, final ScrollPanel.ChildFactory<? extends AbstractWidget> emptyState) {
        populate(scroll, groups, List.of(), ctx, emptyState);
    }

    private static void populate(final ScrollPanel scroll, final List<OptionGroup> groups, final List<Option<?>> looseOptions, final RowContext ctx, final ScrollPanel.ChildFactory<? extends AbstractWidget> emptyState) {
        final float scale = WLScreen.uiScaleFactor();
        final int groupHeaderHeight = Math.round(GROUP_HEADER_HEIGHT_BASE * scale);
        final int rowHeight = Math.round(ROW_HEIGHT_BASE * scale);
        final int groupGap = Math.round(GROUP_GAP_BASE * scale);
        final int rowGap = Math.round(ROW_GAP_BASE * scale);
        final int looseRowGap = Math.round(LOOSE_ROW_GAP_BASE * scale);
        final Runnable rebuild = () -> scroll.defer(() -> {
            scroll.clear();
            populate(scroll, groups, looseOptions, ctx, emptyState);
        });
        final ShownWatch watch = new ShownWatch();
        boolean anyGroup = false;
        final List<Option<?>> hiddenByAvailability = new ArrayList<>();
        for (final OptionGroup group : groups) {
            collectHidden(group.getOptions(), watch, hiddenByAvailability);
        }
        collectHidden(looseOptions, watch, hiddenByAvailability);
        for (final OptionGroup group : groups) {
            if (!watch.anyShown(group.getOptions())) {
                continue;
            }
            final int gapBefore = anyGroup ? groupGap : 0;
            anyGroup = true;
            final OptionGroupHeaderWidget header = scroll.addChild(gapBefore, (x, y, w) -> new OptionGroupHeaderWidget(group, x, y, w, groupHeaderHeight));
            for (final Option<?> option : group.getOptions()) {
                for (final AbstractWidget row : addRow(scroll, option, ctx, rowGap, rowHeight, rebuild, watch)) {
                    header.addRow(row);
                }
            }
        }
        boolean firstLoose = true;
        for (final Option<?> option : looseOptions) {
            final int gapBefore = firstLoose ? (anyGroup ? groupGap : 0) : looseRowGap;
            final OptionGroupHeaderWidget card = scroll.addChild(gapBefore, (x, y, w) -> new OptionGroupHeaderWidget("", x, y, w, 0));
            for (final AbstractWidget row : addRow(scroll, option, ctx, 0, rowHeight, rebuild, watch)) {
                card.addRow(row);
            }
            firstLoose = false;
        }
        if (!hiddenByAvailability.isEmpty()) {
            final String title = hiddenTitle(hiddenByAvailability.size());
            final String detail = hiddenReason(hiddenByAvailability);
            if (scroll.children().isEmpty()) {
                scroll.addChild(0, (x, y, w) -> new EmptyStateWidget(title, detail, x, y, w, EmptyStateWidget.preferredHeight()));
            } else {
                scroll.addChild(groupGap, (x, y, w) -> new HiddenOptionsWidget(title, detail, x, y, w, HiddenOptionsWidget.preferredHeight()));
            }
        } else if (emptyState != null && scroll.children().isEmpty()) {
            scroll.addChild(0, emptyState);
        }
        scroll.rebuildWhenStale(watch, rebuild);
        scroll.relayout();
    }

    private static void collectHidden(final List<Option<?>> options, final ShownWatch watch, final List<Option<?>> into) {
        for (final Option<?> option : options) {
            final String helper = option.getAvailabilityHelper();
            if (!watch.shown(option) && option.isHiddenByAvailability() && helper != null && !helper.isEmpty()) {
                into.add(option);
            }
        }
    }

    private static String hiddenTitle(final int count) {
        return count == 1 ? "1 option hidden" : count + " options hidden";
    }

    private static String hiddenReason(final List<Option<?>> hidden) {
        String common = null;
        for (final Option<?> option : hidden) {
            final String helper = option.getAvailabilityHelper();
            if (helper == null || helper.isEmpty()) {
                return HIDDEN_FALLBACK_REASON;
            }
            if (common == null) {
                common = helper;
            } else if (!common.equals(helper)) {
                return HIDDEN_FALLBACK_REASON;
            }
        }
        return common == null ? HIDDEN_FALLBACK_REASON : common;
    }

    public record SearchScope(String heading, Category category) {}

    @Deprecated
    public static boolean populateSearch(final ScrollPanel scroll, final List<Category> categories, final String query, final RowContext ctx) {
        final List<SearchScope> scopes = new ArrayList<>(categories.size());
        for (final Category category : categories) {
            scopes.add(new SearchScope(category.name(), category));
        }
        return populateSearchScopes(scroll, scopes, query, ctx);
    }

    public static boolean populateSearchScopes(final ScrollPanel scroll, final List<SearchScope> scopes, final String query, final RowContext ctx) {
        return populateSearchScopes(scroll, scopes, query, ctx, null);
    }

    public static boolean populateSearchScopes(final ScrollPanel scroll, final List<SearchScope> scopes, final String query, final RowContext ctx, final ScrollPanel.ChildFactory<? extends AbstractWidget> emptyState) {
        final float scale = WLScreen.uiScaleFactor();
        final int groupHeaderHeight = Math.round(GROUP_HEADER_HEIGHT_BASE * scale);
        final int rowHeight = Math.round(ROW_HEIGHT_BASE * scale);
        final int groupGap = Math.round(GROUP_GAP_BASE * scale);
        final int rowGap = Math.round(ROW_GAP_BASE * scale);
        final String needle = query.toLowerCase(Locale.ROOT).trim();
        final Runnable rebuild = () -> scroll.defer(() -> {
            scroll.clear();
            populateSearchScopes(scroll, scopes, query, ctx, emptyState);
        });
        final ShownWatch watch = new ShownWatch();
        boolean any = false;
        boolean first = true;
        for (final SearchScope scope : scopes) {
            final Category category = scope.category();
            final List<Option<?>> matches = new ArrayList<>();
            for (final OptionGroup group : category.groups()) {
                for (final Option<?> option : group.getOptions()) {
                    collectMatches(option, needle, matches, watch);
                }
            }
            for (final Option<?> option : category.options()) {
                collectMatches(option, needle, matches, watch);
            }
            if (matches.isEmpty()) {
                continue;
            }
            any = true;
            final int gapBefore = first ? 0 : groupGap;
            first = false;
            final OptionGroupHeaderWidget header = scroll.addChild(gapBefore, (x, y, w) -> new OptionGroupHeaderWidget(scope.heading(), x, y, w, groupHeaderHeight));
            for (final Option<?> option : matches) {
                for (final AbstractWidget row : addRow(scroll, option, ctx, rowGap, rowHeight, rebuild, watch)) {
                    header.addRow(row);
                }
            }
        }
        if (!any && emptyState != null) {
            scroll.addChild(0, emptyState);
        }
        scroll.rebuildWhenStale(watch, rebuild);
        scroll.relayout();
        return any;
    }

    private static boolean matches(final Option<?> option, final String needle) {
        final String name = option.getName();
        return name != null && name.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static void collectMatches(final Option<?> option, final String needle, final List<Option<?>> matches, final ShownWatch watch) {
        if (!watch.shown(option)) {
            return;
        }
        if (matches(option, needle)) {
            matches.add(option);
        }
        if (option.hasPages()) {
            for (final List<Option<?>> page : option.getPages().values()) {
                for (final Option<?> nested : page) {
                    collectMatches(nested, needle, matches, watch);
                }
            }
        }
    }

    private static List<AbstractWidget> addRow(final ScrollPanel scroll, final Option<?> option, final RowContext ctx, final int gap, final int rowHeight, final Runnable onPageChanged, final ShownWatch watch) {
        if (!watch.shown(option)) {
            return List.of();
        }
        watch.trackConflicts(option);
        if (option.isPageSelector()) {
            return addPageRow(scroll, cast(option), ctx, gap, rowHeight, onPageChanged, watch);
        }
        if (option.isPageList()) {
            return addPageListRow(scroll, cast(option), ctx, gap, rowHeight, onPageChanged, watch);
        }
        final AbstractWidget row = addSimpleRow(scroll, option, ctx, gap, rowHeight);
        return row == null ? List.of() : List.of(row);
    }

    private static List<AbstractWidget> addPageRow(final ScrollPanel scroll, final Option<String> option, final RowContext ctx, final int gap, final int rowHeight, final Runnable onPageChanged, final ShownWatch watch) {
        scroll.addChild(gap, option, (x, y, w) -> new OptionPageWidget(option, ctx, onPageChanged, x, y, w, rowHeight));
        final List<AbstractWidget> rows = new ArrayList<>();
        for (final Option<?> child : option.getActivePageOptions()) {
            rows.addAll(addRow(scroll, child, ctx, gap, rowHeight, onPageChanged, watch));
        }
        return rows;
    }

    private static List<AbstractWidget> addPageListRow(final ScrollPanel scroll, final Option<List<String>> option, final RowContext ctx, final int gap, final int rowHeight, final Runnable onPageChanged, final ShownWatch watch) {
        final AbstractWidget listRow = scroll.addChild(gap, option, (x, y, w) -> new PageListOptionWidget(option, ctx, scroll::relayout, onPageChanged, x, y, w, rowHeight));
        final List<Option<?>> page = option.getActivePageOptions();
        if (!watch.anyShown(page)) {
            return List.of(listRow);
        }
        float scale = WLScreen.uiScaleFactor();
        int groupHeaderHeight = Math.round(GROUP_HEADER_HEIGHT_BASE * scale);
        int groupGap = Math.round(GROUP_GAP_BASE * scale);
        int nestedRowGap = Math.round(ROW_GAP_BASE * scale);
        String title = option.pageLabel(option.getActivePageId());
        final List<OptionGroup> groups = option.getActivePageGroups();
        if (groups.isEmpty()) {
            OptionGroupHeaderWidget header = scroll.addChild(groupGap, (x, y, w) -> new OptionGroupHeaderWidget(title, x, y, w, groupHeaderHeight));
            for (final Option<?> child : page) {
                for (final AbstractWidget row : addRow(scroll, child, ctx, nestedRowGap, rowHeight, onPageChanged, watch)) {
                    header.addRow(row);
                }
            }
            return List.of(listRow);
        }
        boolean first = true;
        for (final OptionGroup group : groups) {
            if (!watch.anyShown(group.getOptions())) {
                continue;
            }
            final String groupTitle = first ? title : group.getName();
            first = false;
            final OptionGroupHeaderWidget header = scroll.addChild(groupGap, (x, y, w) -> new OptionGroupHeaderWidget(groupTitle, x, y, w, groupHeaderHeight));
            for (final Option<?> child : group.getOptions()) {
                for (final AbstractWidget row : addRow(scroll, child, ctx, nestedRowGap, rowHeight, onPageChanged, watch)) {
                    header.addRow(row);
                }
            }
        }
        return List.of(listRow);
    }

    private static AbstractWidget addSimpleRow(final ScrollPanel scroll, final Option<?> option, final RowContext ctx, final int gap, final int rowHeight) {
        final Class<?> type = option.getType();
        if (type == Boolean.class) {
            final Option<Boolean> o = cast(option);
            return scroll.addChild(gap, option, (x, y, w) -> new BooleanOptionWidget(o, ctx, x, y, w, rowHeight));
        } else if (type == Integer.class || type == Double.class || type == Float.class) {
            final Option<Number> o = cast(option);
            return scroll.addChild(gap, option, (x, y, w) -> new NumberOptionWidget(o, ctx, x, y, w, rowHeight));
        } else if (type == String.class) {
            final Option<String> o = cast(option);
            return scroll.addChild(gap, option, (x, y, w) -> new StringOptionWidget(o, ctx, x, y, w, rowHeight));
        } else if (type == List.class) {
            final Option<List<String>> o = cast(option);
            return scroll.addChild(gap, option, (x, y, w) -> new StringListOptionWidget(o, ctx, scroll::relayout, x, y, w, rowHeight));
        } else if (type == WLColor.class) {
            final Option<WLColor> o = cast(option);
            return scroll.addChild(gap, option, (x, y, w) -> new ColorOptionWidget(o, ctx, x, y, w, rowHeight));
        } else if (type == Identifier.class) {
            final Option<Identifier> o = cast(option);
            return scroll.addChild(gap, option, (x, y, w) -> new TextureOptionWidget(o, ctx, x, y, w, rowHeight));
        } else if (type == CroppedTexture.class) {
            final Option<CroppedTexture> o = cast(option);
            return scroll.addChild(gap, option, (x, y, w) -> new CroppedTextureOptionWidget(o, ctx, x, y, w, rowHeight));
        } else if (type == ItemInventory.class) {
            final Option<ItemInventory> o = cast(option);
            return scroll.addChild(gap, option, (x, y, w) -> new InventoryOptionWidget(o, ctx, x, y, w, rowHeight));
        } else if (type == PixelGridAnimation.class) {
            final Option<PixelGridAnimation> o = cast(option);
            return scroll.addChild(gap, option, (x, y, w) -> new PixelGridAnimationOptionWidget(o, ctx, x, y, w, rowHeight));
        } else if (type == Preview.class) {
            final Option<Preview> o = cast(option);
            return scroll.addChild(gap, option, (x, y, w) -> new PreviewOptionWidget(o, ctx, x, y, w, rowHeight));
        } else if (type == ModLinks.class) {
            final Option<ModLinks> o = cast(option);
            return scroll.addChild(gap, option, (x, y, w) -> new ModLinksOptionWidget(o, ctx, x, y, w, rowHeight));
        } else if (type == WLKeyMapping.class) {
            final Option<WLKeyMapping> o = cast(option);
            return scroll.addChild(gap, option, (x, y, w) -> new KeybindOptionWidget(o, ctx, x, y, w, rowHeight));
        } else if (type == Runnable.class) {
            final Option<Runnable> o = cast(option);
            if (o.getHudElement() != null) {
                return scroll.addChild(gap, option, (x, y, w) -> new HudSpaceOptionWidget(o, ctx, x, y, w, rowHeight));
            }
            return scroll.addChild(gap, option, (x, y, w) -> new ButtonOptionWidget(o, ctx, x, y, w, rowHeight));
        } else if (Enum.class.isAssignableFrom(type)) {
            final Option<Enum> o = cast(option);
            return scroll.addChild(gap, option, (x, y, w) -> new EnumOptionWidget(o, ctx, x, y, w, rowHeight));
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Option<T> cast(final Option<?> option) {
        return (Option) option;
    }

    private static final class ShownWatch implements BooleanSupplier {
        private final Map<Option<?>, Boolean> watched = new LinkedHashMap<>();

        private final Map<Option<?>, String> conflicts = new LinkedHashMap<>();

        private void trackConflicts(final Option<?> option) {
            if (option.getType() == WLKeyMapping.class) {
                this.conflicts.put(option, conflictKey(option));
            }
        }

        private static String conflictKey(final Option<?> option) {
            return option.getValue() instanceof final WLKeyMapping keymapping ? String.join(", ", WalksyLib.getInstance().getKeyBinds().conflictsFor(keymapping)) : "";
        }

        private boolean shown(final Option<?> option) {
            if (!option.canHide()) {
                return true;
            }
            final Boolean cached = this.watched.get(option);
            if (cached != null) {
                return cached;
            }
            final boolean shown = option.isShown();
            this.watched.put(option, shown);
            return shown;
        }

        private boolean anyShown(final List<Option<?>> options) {
            for (final Option<?> option : options) {
                if (this.shown(option)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean getAsBoolean() {
            for (final Map.Entry<Option<?>, Boolean> entry : this.watched.entrySet()) {
                if (entry.getKey().isShown() != entry.getValue()) {
                    return true;
                }
            }
            for (final Map.Entry<Option<?>, String> entry : this.conflicts.entrySet()) {
                if (!conflictKey(entry.getKey()).equals(entry.getValue())) {
                    return true;
                }
            }
            return false;
        }
    }
}
