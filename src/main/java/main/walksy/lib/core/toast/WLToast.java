package main.walksy.lib.core.toast;

import main.walksy.lib.api.hud.HudElement;
import main.walksy.lib.core.render.texture.Icons;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public final class WLToast {
    public static final float STICKY = 0f;
    static final float DEFAULT_SECONDS = 5f;
    private String title;
    private @Nullable String message;
    private @Nullable ToastIcon icon;
    private @Nullable ToastContent content;
    private ToastLevel level = ToastLevel.PLAIN;
    private @Nullable Integer accent;
    private float seconds = DEFAULT_SECONDS;
    private @Nullable Object id;
    private @Nullable Runnable onDismissed;
    private @Nullable ToastIcon resolvedIcon;
    private boolean iconResolved;

    private WLToast(final String title) {
        this.title = title == null ? "" : title;
    }

    public static WLToast of(final String title) {
        return new WLToast(title);
    }

    public static WLToast of(final String title, final String message) {
        return new WLToast(title).message(message);
    }

    public WLToast title(final String value) {
        this.title = value == null ? "" : value;
        return this;
    }

    public WLToast message(final @Nullable String value) {
        this.message = value == null || value.isBlank() ? null : value;
        return this;
    }

    public WLToast icon(final @Nullable ToastIcon value) {
        this.icon = value;
        this.iconResolved = false;
        return this;
    }

    public WLToast icon(final Icons.Icon value) {
        return this.icon(ToastIcon.glyph(value));
    }

    public WLToast icon(final Identifier texture, final int textureSize) {
        return this.icon(ToastIcon.texture(texture, textureSize));
    }

    public WLToast item(final String itemId) {
        return this.icon(ToastIcon.item(itemId));
    }

    public WLToast icon(final HudElement element) {
        return this.icon(ToastIcon.hud(element));
    }

    public WLToast content(final @Nullable ToastContent value) {
        this.content = value;
        return this;
    }

    public WLToast level(final ToastLevel value) {
        this.level = value == null ? ToastLevel.PLAIN : value;
        this.iconResolved = false;
        return this;
    }

    public WLToast info() {
        return this.level(ToastLevel.INFO);
    }

    public WLToast success() {
        return this.level(ToastLevel.SUCCESS);
    }

    public WLToast warning() {
        return this.level(ToastLevel.WARNING);
    }

    public WLToast error() {
        return this.level(ToastLevel.ERROR);
    }

    public WLToast accent(final @Nullable Integer argb) {
        this.accent = argb;
        return this;
    }

    public WLToast seconds(final float value) {
        this.seconds = Math.max(0f, value);
        return this;
    }

    public WLToast id(final @Nullable Object value) {
        this.id = value;
        return this;
    }

    public WLToast onDismissed(final @Nullable Runnable value) {
        this.onDismissed = value;
        return this;
    }

    public String getTitle() {
        return this.title;
    }

    public @Nullable String getMessage() {
        return this.message;
    }

    public @Nullable ToastIcon resolveIcon() {
        if (!this.iconResolved) {
            this.resolvedIcon = this.icon != null ? this.icon : this.level.icon();
            this.iconResolved = true;
        }
        return this.resolvedIcon;
    }

    public @Nullable ToastContent getContent() {
        return this.content;
    }

    public ToastLevel getLevel() {
        return this.level;
    }

    public int resolveAccent() {
        return this.accent != null ? this.accent : this.level.accent();
    }

    public float getSeconds() {
        return this.seconds;
    }

    public @Nullable Object getId() {
        return this.id;
    }

    public @Nullable Runnable getOnDismissed() {
        return this.onDismissed;
    }

    WLToast copy() {
        final WLToast copy = new WLToast(this.title);
        copy.message = this.message;
        copy.icon = this.icon;
        copy.content = this.content;
        copy.level = this.level;
        copy.accent = this.accent;
        copy.seconds = this.seconds;
        copy.id = this.id;
        copy.onDismissed = this.onDismissed;
        return copy;
    }
}
