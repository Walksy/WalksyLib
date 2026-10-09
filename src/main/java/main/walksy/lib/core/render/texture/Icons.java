package main.walksy.lib.core.render.texture;

import main.walksy.lib.core.WalksyLibConstants;
import main.walksy.lib.core.gui.style.UiStyle;
import net.minecraft.resources.Identifier;
import main.walksy.lib.core.render.DevicePixels;
import main.walksy.lib.core.render.WLGraphics2D;

public final class Icons {
    public static final Icon SEARCH;
    public static final Icon CHEVRON;
    public static final Icon PLUS;
    public static final Icon MINUS;
    public static final Icon PULSE;
    public static final Icon COPY;
    public static final Icon PASTE;
    public static final Icon TEAM;
    public static final Icon APPEARANCE;
    public static final Icon HUD;
    public static final Icon ALL_MODS;
    public static final Icon KEYBOARD;
    public static final Icon LOGS;
    public static final Icon CHANGELOG;
    public static final Icon CHECK;
    public static final Icon INFO;
    public static final Icon WARNING;
    public static final Icon ERROR;
    public static final Icon UPDATE;
    public static final Icon GITHUB;
    public static final Icon WEB;
    public static final Icon RESET;
    public static final Icon TRASH;

    static {
        SEARCH = new Icon(id("search"), 48);
        CHEVRON = new Icon(id("chevron"), 48);
        PLUS = new Icon(id("plus"), 48);
        MINUS = new Icon(id("minus"), 48);
        PULSE = new Icon(id("pulse"), 48);
        COPY = new Icon(id("copy"), 48);
        PASTE = new Icon(id("paste"), 48);
        TEAM = new Icon(id("team"), 48);
        APPEARANCE = new Icon(id("appearance"), 48);
        HUD = new Icon(id("hud"), 48);
        ALL_MODS = new Icon(id("allmods"), 48);
        KEYBOARD = new Icon(id("keyboard"), 48);
        LOGS = new Icon(id("logs"), 48);
        CHANGELOG = new Icon(id("changelog"), 48);
        CHECK = new Icon(id("check"), 48);
        INFO = new Icon(id("info"), 48);
        WARNING = new Icon(id("warning"), 48);
        ERROR = new Icon(id("error"), 48);
        UPDATE = new Icon(id("update"), 48);
        GITHUB = new Icon(id("github"), 48);
        WEB = new Icon(id("web"), 48);
        RESET = new Icon(id("reset"), 48);
        TRASH = new Icon(id("trash"), 48);
    }

    private static Identifier id(final String name) {
        return Identifier.fromNamespaceAndPath(WalksyLibConstants.NAMESPACE, "gui/icon/" + name + ".png");
    }

    private static final int PIXEL_GRID = 12;

    public record Icon(Identifier id, int texSize) {
        private Identifier pixelId() {
            return Identifier.fromNamespaceAndPath(this.id.getNamespace(), this.id.getPath().replace("gui/icon/", "gui/icon/minecraft/"));
        }

        private boolean pixelated() {
            return UiStyle.minecraft() && this.id.getNamespace().equals(WalksyLibConstants.NAMESPACE) && this.id.getPath().startsWith("gui/icon/");
        }

        private void extractPixel(final WLGraphics2D graphics, final float x, final float y, final float size, final int color, final float degrees) {
            final float guiScale = DevicePixels.perLogicalY();
            final int pixel = Math.max(1, Math.round(size * guiScale / PIXEL_GRID));
            final float drawn = PIXEL_GRID * pixel / guiScale;
            final float px = DevicePixels.snapX(x + (size - drawn) / 2f, graphics.pose());
            final float py = DevicePixels.snapY(y + (size - drawn) / 2f, graphics.pose());
            final boolean rotated = degrees != 0f;
            if (rotated) {
                final float cx = px + drawn / 2f;
                final float cy = py + drawn / 2f;
                graphics.pose().pushMatrix();
                graphics.pose().translate(cx, cy);
                graphics.pose().rotate((float) Math.toRadians(degrees));
                graphics.pose().translate(-cx, -cy);
            }
            graphics.blit(this.pixelId(), px, py, 0f, 0f, drawn, drawn, PIXEL_GRID, PIXEL_GRID, PIXEL_GRID, PIXEL_GRID, color);
            if (rotated) {
                graphics.pose().popMatrix();
            }
        }

        public void extract(final WLGraphics2D graphics, final float x, final float y, final float size, final int color) {
            this.extractRotated(graphics, x, y, size, color, 0f);
        }

        public void extractCentered(final WLGraphics2D graphics, final float cx, final float cy, final float size, final int color,final float degrees) {
            if (this.pixelated()) {
                this.extractPixel(graphics, cx - size / 2f, cy - size / 2f, size, color, degrees);
                return;
            }
            final float normalized = ((degrees % 360f) + 360f) % 360f;
            final int quarterTurns = Math.round(normalized / 90f) % 4;
            if (Math.abs(normalized - quarterTurns * 90f) > 0.01f || !IconRaster.extractCentered(this, graphics, cx, cy, size, color, quarterTurns)) {
                this.extractViaPose(graphics, cx - size / 2f, cy - size / 2f, size, color, degrees);
            }
        }

        public void extractRotated(final WLGraphics2D graphics, final float x, final float y, final float size, final int color, final float degrees) {
            if (this.pixelated()) {
                this.extractPixel(graphics, x, y, size, color, degrees);
                return;
            }
            final float normalized = ((degrees % 360f) + 360f) % 360f;
            final int quarterTurns = Math.round(normalized / 90f) % 4;
            if (Math.abs(normalized - quarterTurns * 90f) > 0.01f) {
                this.extractViaPose(graphics, x, y, size, color, degrees);
                return;
            }
            if (!IconRaster.extract(this, graphics, x, y, size, color, quarterTurns)) {
                this.extractViaPose(graphics, x, y, size, color, degrees);
            }
        }

        private void extractViaPose(final WLGraphics2D graphics, final float x, final float y, final float size, final int color, final float degrees) {
            final float s = Math.max(size, DevicePixels.snapSizeY(0f));
            if (degrees != 0f) {
                final float cx = x + s / 2f;
                final float cy = y + s / 2f;
                graphics.pose().pushMatrix();
                graphics.pose().translate(cx, cy);
                graphics.pose().rotate((float) Math.toRadians(degrees));
                graphics.pose().translate(-cx, -cy);
                this.blitSource(graphics, x, y, s, color);
                graphics.pose().popMatrix();
                return;
            }
            this.blitSource(graphics, x, y, s, color);
        }

        void blitSource(final WLGraphics2D graphics, final float x, final float y, final float size, final int color) {
            graphics.blit(this.id, x, y, 0f, 0f, size, size, this.texSize, this.texSize, this.texSize, this.texSize, color);
        }
    }
}
