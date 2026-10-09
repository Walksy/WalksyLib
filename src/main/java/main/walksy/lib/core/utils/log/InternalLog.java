package main.walksy.lib.core.utils.log;

import main.walksy.lib.api.config.value.CroppedTexture;
import main.walksy.lib.api.config.value.ItemInventory;
import main.walksy.lib.core.utils.EnumNames;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record InternalLog(String time, String source, String field, String from, String to, @Nullable String warning, boolean library) {
    private static final int DECIMALS = 2;
    private static final int LISTED_ITEMS = 4;

    public static InternalLog of(final String time, final String source, final String field, final Object from, final Object to, final @Nullable String warning, final boolean library) {
        return new InternalLog(time, source, field, text(from), text(to), warning, library);
    }

    public static String text(final Object value) {
        if (value == null) {
            return "nothing";
        }
        final String string;
        if (value instanceof final Boolean flag) {
            string = flag ? "On" : "Off";
        } else if (value instanceof final Float number) {
            string = decimal(Double.parseDouble(Float.toString(number)));
        } else if (value instanceof final Double number) {
            string = decimal(number);
        } else if (value instanceof final Enum<?> constant) {
            string = EnumNames.pretty(constant);
        } else if (value instanceof final Collection<?> items) {
            string = list(items);
        } else if (value instanceof final ItemInventory inventory) {
            final int filled = inventory.filledCount();
            string = filled == 1 ? "1 item" : filled + " items";
        } else if (value instanceof final CroppedTexture texture) {
            string = texture.isEmpty() ? "none" : fileName(String.valueOf(texture.texture()));
        } else {
            string = String.valueOf(value);
        }
        return string.isBlank() ? "empty" : string;
    }

    public static String decimal(final double value) {
        if (!Double.isFinite(value)) {
            return String.valueOf(value);
        }
        return BigDecimal.valueOf(value).setScale(DECIMALS, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    public static String percent(final float fraction) {
        return Math.round(fraction * 100f) + "%";
    }

    private static String list(final Collection<?> items) {
        if (items.isEmpty()) {
            return "empty";
        }
        final List<String> shown = new ArrayList<>();
        for (final Object item : items) {
            if (shown.size() == LISTED_ITEMS) {
                break;
            }
            shown.add(text(item));
        }
        final String joined = String.join(", ", shown);
        final int hidden = items.size() - shown.size();
        return hidden > 0 ? joined + " and " + hidden + " more" : joined;
    }

    private static String fileName(final String id) {
        final int slash = id.lastIndexOf('/');
        final int colon = id.lastIndexOf(':');
        return id.substring(Math.max(slash, colon) + 1);
    }

    public String summary() {
        return this.field + ": " + this.from + " -> " + this.to;
    }

    public String detail() {
        final String where = this.time + "  " + this.source;
        return this.warning == null ? where : where + "  -  " + this.warning;
    }
}
