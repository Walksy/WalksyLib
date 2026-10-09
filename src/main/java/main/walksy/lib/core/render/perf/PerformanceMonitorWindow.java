package main.walksy.lib.core.render.perf;

import main.walksy.lib.core.render.TextQueueMark;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.WalksyLibConstants;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.render.Cursors;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.msdf.MsdfFont;
import net.minecraft.util.Mth;

import java.util.Arrays;

public final class PerformanceMonitorWindow {
    private static final float WIDTH = 260f;
    private static final float PAD = 10f;
    private static final float TITLE_HEIGHT = 26f;
    private static final float ROW_HEIGHT = 13f;
    private static final float GRAPH_HEIGHT = 36f;
    private static final float SECTION_GAP = 8f;
    private static final float CORNER_RADIUS = 12f;
    private static final float CHECKBOX = 9f;
    private static final float BUTTON_HEIGHT = 14f;
    private static final float CHIP_GAP = 4f;
    private static final float COMPACT_HEIGHT = 19f;
    private static final float CLICK_SLOP = 3f;
    private static final PerformanceMonitorWindow INSTANCE = new PerformanceMonitorWindow();
    private final boolean[] expanded = new boolean[Group.values().length];
    private final float[] groupHeaderY = new float[Group.values().length];
    private final float[] bypassRowY = new float[UiProfiler.Bypass.values().length];
    private boolean open;
    private float x = -1f;
    private float y = -1f;
    private float width;
    private float height;
    private float scale = 1f;
    private boolean dragging;
    private float dragOffsetX;
    private float dragOffsetY;
    private boolean compact;
    private float pressStartX;
    private float pressStartY;
    private boolean dragMoved;
    private float minimiseX;
    private float minimiseW;
    private float closeX;
    private float closeW;
    private float baselineX;
    private float baselineW;
    private float buttonY;
    private float buttonH;
    private float rowLeft;
    private float rowWidth;
    private boolean laidOut;

    private enum Group {
        RENDER_THREAD("RENDER THREAD", true),
        GPU_WORK("GPU WORK / FRAME", true),
        SWITCHES("TURN OFF TO MEASURE", true);

        private final String title;
        private final boolean openByDefault;

        Group(final String title, final boolean openByDefault) {
            this.title = title;
            this.openByDefault = openByDefault;
        }
    }

    public static PerformanceMonitorWindow get() {
        return INSTANCE;
    }

    private PerformanceMonitorWindow() {
        for (final Group group : Group.values()) {
            this.expanded[group.ordinal()] = group.openByDefault;
        }
    }

    public boolean isOpen() {
        return this.open;
    }

    public void toggle() {
        if (this.open) {
            this.close();
        } else {
            this.openWindow();
        }
    }

    public void openWindow() {
        this.open = true;
        UiProfiler.setEnabled(true);
    }

    public void close() {
        this.open = false;
        this.laidOut = false;
        this.dragging = false;
        this.compact = false;
        UiProfiler.setEnabled(false);
        UiProfiler.clearBypasses();
        UiProfiler.clearBaseline();
    }

    private void place(final int screenW, final int screenH) {
        if (this.x >= 0f && this.y >= 0f) {
            return;
        }
        this.x = Math.max(6f, screenW - this.width - 12f);
        this.y = Math.max(6f, (screenH - this.height) * 0.5f);
    }

    public void extract(final WLGraphics2D graphics, final int screenW, final int screenH, final int mouseX, final int mouseY) {
        if (!WalksyLibConstants.DEV_BUILD || !this.open) {
            return;
        }
        this.scale = WLScreen.uiScaleFactor();
        if (this.compact) {
            final MsdfFont font = Type.SUBHEADLINE.emphasized();
            this.width = font.width(compactLabel(), Type.SUBHEADLINE.size() * this.scale) + 16f * this.scale;
            this.height = COMPACT_HEIGHT * this.scale;
        } else {
            this.width = WIDTH * this.scale;
            this.rowLeft = this.x + PAD * this.scale;
            this.rowWidth = this.width - PAD * this.scale * 2f;
            this.height = this.layout(null, 0, 0);
        }
        this.place(screenW, screenH);
        this.x = Mth.clamp(this.x, 0f, Math.max(0f, screenW - this.width));
        this.y = Mth.clamp(this.y, 0f, Math.max(0f, screenH - this.height));
        this.rowLeft = this.x + PAD * this.scale;
        final TextQueueMark textMark = WLRenderSystem.textMark();
        UiProfiler.begin(UiProfiler.Section.MONITOR);
        WLRenderSystem.contentStratum().beginDev();
        try {
            if (this.compact) {
                this.paintCompact(graphics, mouseX, mouseY);
            } else {
                this.layout(graphics, mouseX, mouseY);
            }
        } finally {
            WLRenderSystem.contentStratum().endDev();
            WLRenderSystem.markDevText(textMark);
            UiProfiler.end(UiProfiler.Section.MONITOR);
        }
        this.laidOut = true;
    }

    private void paintCompact(final WLGraphics2D graphics, final int mouseX, final int mouseY) {
        final Theme theme = Theme.get();
        final int edge = argb(theme.isDark() ? 0xFFFFFF : 0x000000, theme.isDark() ? 0.22f : 0.18f);
        graphics.fillRoundedGradient(this.x, this.y, this.x + this.width, this.y + this.height, edge, edge, edge, edge, this.height * 0.5f);
        final float border = Math.max(WLScreen.hairlinePx(), 0.75f * this.scale);
        final int plate = plate(theme);
        graphics.fillRoundedGradient(this.x + border, this.y + border, this.x + this.width - border, this.y + this.height - border, plate, plate, plate, plate, (this.height - border * 2f) * 0.5f);
        final boolean hovered = this.inRect(mouseX, mouseY, this.x, this.y, this.width, this.height);
        WLRenderSystem.cursors().requestIf(hovered || this.dragging, Cursors.Cursor.MOVE);
        final MsdfFont font = Type.SUBHEADLINE.emphasized();
        final float size = Type.SUBHEADLINE.size() * this.scale;
        final String label = compactLabel();
        font.extract(graphics, label, this.x + (this.width - font.width(label, size)) / 2f, this.y + (this.height - font.height(size)) / 2f, size, UiProfiler.anyBypassed() ? theme.accent() : theme.label());
    }

    private static String compactLabel() {
        return Math.round(UiProfiler.fps()) + " fps";
    }

    private float layout(final WLGraphics2D graphics, final int mouseX, final int mouseY) {
        final Theme theme = Theme.get();
        final float s = this.scale;
        final float left = this.rowLeft;
        final float right = left + this.rowWidth;
        final boolean draw = graphics != null;
        if (draw) {
            final int edge = argb(theme.isDark() ? 0xFFFFFF : 0x000000, theme.isDark() ? 0.22f : 0.18f);
            graphics.fillRoundedGradient(this.x, this.y, this.x + this.width, this.y + this.height, edge, edge, edge, edge, CORNER_RADIUS * s);
            final float border = Math.max(WLScreen.hairlinePx(), 0.75f * s);
            final int plate = plate(theme);
            graphics.fillRoundedGradient(this.x + border, this.y + border, this.x + this.width - border, this.y + this.height - border, plate, plate, plate, plate, CORNER_RADIUS * s - border);
        }
        float cursorY = this.y;
        cursorY = this.titleBar(graphics, left, right, cursorY, mouseX, mouseY, theme);
        cursorY = this.gap(graphics, left, right, cursorY, theme, false);
        cursorY = this.headline(graphics, left, right, cursorY, theme);
        cursorY = this.graph(graphics, left, cursorY, theme);
        cursorY = this.gap(graphics, left, right, cursorY, theme, true);
        cursorY = this.groupHeader(graphics, left, right, cursorY, Group.RENDER_THREAD, fmt(UiProfiler.libraryCpuMs(), 2) + " ms", mouseX, mouseY, theme);
        if (this.expanded[Group.RENDER_THREAD.ordinal()]) {
            final double basis = Math.max(0.05, UiProfiler.frameMs());
            for (final UiProfiler.Section section : UiProfiler.Section.values()) {
                cursorY = this.costRow(graphics, left, right, cursorY, section.label(), UiProfiler.sectionMs(section), basis, theme.accent(), theme);
            }
            cursorY = this.costRow(graphics, left, right, cursorY, "Rest of frame (game + GPU)", UiProfiler.unattributedMs(), basis, theme.tertiaryLabel(), theme);
        }
        cursorY = this.gap(graphics, left, right, cursorY, theme, true);
        cursorY = this.groupHeader(graphics, left, right, cursorY, Group.GPU_WORK, whole(UiProfiler.Counter.COMPOSITE_PASSES) + " passes", mouseX, mouseY, theme);
        if (this.expanded[Group.GPU_WORK.ordinal()]) {
            cursorY = this.valueRow(graphics, left, right, cursorY, "Render passes", whole(UiProfiler.Counter.RENDER_PASSES), theme);
            cursorY = this.valueRow(graphics, left, right, cursorY, "Composite", whole(UiProfiler.Counter.COMPOSITE_PASSES) + " x " + mpx(UiProfiler.Counter.COMPOSITE_PIXELS), theme);
            cursorY = this.valueRow(graphics, left, right, cursorY, "Blur + downsample", whole(UiProfiler.Counter.BLUR_PASSES) + " x " + mpx(UiProfiler.Counter.BLUR_PIXELS), theme);
            cursorY = this.valueRow(graphics, left, right, cursorY, "Blur texture reads", fmt(UiProfiler.counter(UiProfiler.Counter.BLUR_TAPS) * 1e-6, 1) + "M", theme);
            cursorY = this.valueRow(graphics, left, right, cursorY, "Colour-target copies", mpx(UiProfiler.Counter.CAPTURE_PIXELS), theme);
            cursorY = this.valueRow(graphics, left, right, cursorY, "Overlay clears", mpx(UiProfiler.Counter.OVERLAY_PIXELS), theme);
            cursorY = this.valueRow(graphics, left, right, cursorY, "Glass shapes / layers", whole(UiProfiler.Counter.GLASS_SHAPES) + " / " + whole(UiProfiler.Counter.GLASS_LAYERS), theme);
            cursorY = this.valueRow(graphics, left, right, cursorY, "Stratum shapes / glyphs", whole(UiProfiler.Counter.STRATUM_SHAPES) + " / " + whole(UiProfiler.Counter.GLYPHS), theme);
        }
        cursorY = this.gap(graphics, left, right, cursorY, theme, true);
        cursorY = this.groupHeader(graphics, left, right, cursorY, Group.SWITCHES, UiProfiler.anyBypassed() ? "ON" : "", mouseX, mouseY, theme);
        if (this.expanded[Group.SWITCHES.ordinal()]) {
            final UiProfiler.Bypass[] bypasses = UiProfiler.Bypass.values();
            for (int i = 0; i < bypasses.length; i++) {
                this.bypassRowY[i] = cursorY;
                cursorY = this.checkboxRow(graphics, left, right, cursorY, bypasses[i], mouseX, mouseY, theme);
            }
        } else {
            Arrays.fill(this.bypassRowY, -1e6f);
        }
        return cursorY + PAD * s - this.y;
    }

    private float titleBar(final WLGraphics2D graphics, final float left, final float right, final float top, final int mouseX, final int mouseY, final Theme theme) {
        final float s = this.scale;
        final float h = TITLE_HEIGHT * s;
        final float buttonHeight = BUTTON_HEIGHT * s;
        this.buttonY = top + (h - buttonHeight) / 2f;
        this.buttonH = buttonHeight;
        this.closeW = buttonHeight;
        this.closeX = right - this.closeW;
        final String baselineLabel = UiProfiler.hasBaseline() ? "clear" : "base";
        this.baselineW = this.chipWidth(baselineLabel);
        this.baselineX = this.closeX - CHIP_GAP * s - this.baselineW;
        this.minimiseW = buttonHeight;
        this.minimiseX = this.baselineX - CHIP_GAP * s - this.minimiseW;
        if (graphics != null) {
            final MsdfFont font = Type.SUBHEADLINE.emphasized();
            final float size = Type.SUBHEADLINE.size() * s;
            font.extract(graphics, "Performance Monitor", left, top + (h - font.height(size)) / 2f, size, theme.label());
            final boolean overTitle = this.inRect(mouseX, mouseY, this.x, top, this.width, h) && !this.inRect(mouseX, mouseY, this.minimiseX, this.buttonY, right - this.minimiseX, buttonHeight);
            WLRenderSystem.cursors().requestIf(overTitle || this.dragging, Cursors.Cursor.MOVE);
            this.chip(graphics, this.closeX, this.buttonY, this.closeW, buttonHeight, "x", this.inRect(mouseX, mouseY, this.closeX, this.buttonY, this.closeW, buttonHeight), false, theme);
            this.chip(graphics, this.baselineX, this.buttonY, this.baselineW, buttonHeight, baselineLabel, this.inRect(mouseX, mouseY, this.baselineX, this.buttonY, this.baselineW, buttonHeight), UiProfiler.hasBaseline(), theme);
            this.chip(graphics, this.minimiseX, this.buttonY, this.minimiseW, buttonHeight, "-", this.inRect(mouseX, mouseY, this.minimiseX, this.buttonY, this.minimiseW, buttonHeight), false, theme);
        }
        return top + h;
    }

    private float gap(final WLGraphics2D graphics, final float left, final float right, final float top, final Theme theme, final boolean rule) {
        final float s = this.scale;
        if (graphics != null && rule) {
            graphics.fill(left, top + SECTION_GAP * s * 0.5f, right, top + SECTION_GAP * s * 0.5f + WLScreen.hairlinePx(), theme.separator(), null);
        }
        return top + SECTION_GAP * s * (rule ? 1.6f : 1f);
    }

    private float headline(final WLGraphics2D graphics, final float left, final float right, final float top, final Theme theme) {
        final float s = this.scale;
        final MsdfFont big = Type.TITLE2.emphasized();
        final float bigSize = Type.TITLE2.size() * s;
        final float lineHeight = big.height(bigSize);
        if (graphics != null) {
            final double frameMs = UiProfiler.frameMs();
            final String head = fmt(frameMs, 2) + " ms";
            big.extract(graphics, head, left, top, bigSize, theme.label());
            final MsdfFont small = Type.CAPTION1.regular();
            final float smallSize = Type.CAPTION1.size() * s;
            final float smallY = top + lineHeight - small.height(smallSize);
            small.extract(graphics, Math.round(UiProfiler.fps()) + " fps", left + big.width(head, bigSize) + 5f * s, smallY, smallSize, theme.secondaryLabel());
            final String trailing;
            final int color;
            if (UiProfiler.hasBaseline()) {
                final double delta = frameMs - UiProfiler.baselineMs();
                trailing = (delta >= 0 ? "+" : "-") + fmt(Math.abs(delta), 2) + " ms";
                color = delta > 0.05 ? theme.destructive() : delta < -0.05 ? theme.accent() : theme.secondaryLabel();
            } else {
                trailing = "peak " + fmt(UiProfiler.peakMs(), 1);
                color = theme.tertiaryLabel();
            }
            small.extract(graphics, trailing, right - small.width(trailing, smallSize), smallY, smallSize, color);
        }
        return top + lineHeight + 4f * s;
    }

    private float graph(final WLGraphics2D graphics, final float gx, final float gy, final Theme theme) {
        final float gw = this.rowWidth;
        final float gh = GRAPH_HEIGHT * this.scale;
        if (graphics == null) {
            return gy + gh;
        }
        final int track = argb(theme.isDark() ? 0xFFFFFF : 0x000000, theme.isDark() ? 0.07f : 0.05f);
        graphics.fillRoundedGradient(gx, gy, gx + gw, gy + gh, track, track, track, track, 4f * this.scale);
        final int count = UiProfiler.sparklineCount();
        if (count < 2) {
            return gy + gh;
        }
        final float columnWidth = Math.max(1.5f * this.scale, gw / count);
        final int columns = Math.max(1, (int) (gw / columnWidth));
        float peak = 0.001f;
        for (int i = 0; i < count; i++) {
            peak = Math.max(peak, UiProfiler.sparklineAt(i));
        }
        peak *= 1.15f;
        final int line = argb(theme.accent() & 0xFFFFFF, 0.8f);
        for (int c = 0; c < columns; c++) {
            final int from = (int) ((long) c * count / columns);
            final int to = Math.max(from + 1, (int) ((long) (c + 1) * count / columns));
            float value = 0f;
            for (int i = from; i < to && i < count; i++) {
                value = Math.max(value, UiProfiler.sparklineAt(i));
            }
            final float h = Math.min(gh, gh * (value / peak));
            if (h <= 0f) {
                continue;
            }
            final float cx = gx + c * columnWidth;
            graphics.fill(cx, gy + gh - h, cx + columnWidth, gy + gh, line, null);
        }
        if (UiProfiler.hasBaseline()) {
            final float by = gy + gh - Math.min(gh, gh * (float) (UiProfiler.baselineMs() / peak));
            final int rule = argb(theme.isDark() ? 0xFFFFFF : 0x000000, 0.4f);
            graphics.fill(gx, by, gx + gw, by + WLScreen.hairlinePx(), rule, null);
        }
        return gy + gh;
    }

    private float groupHeader(final WLGraphics2D graphics, final float left, final float right, final float top, final Group group, final String trailing, final int mouseX, final int mouseY, final Theme theme) {
        final float s = this.scale;
        this.groupHeaderY[group.ordinal()] = top;
        if (graphics != null) {
            final boolean hovered = this.inRect(mouseX, mouseY, this.x, top, this.width, ROW_HEIGHT * s);
            WLRenderSystem.cursors().requestIf(hovered, Cursors.Cursor.POINTER);
            final MsdfFont font = Type.CAPTION2.emphasized();
            final float size = Type.CAPTION2.size() * s * 0.9f;
            final float markSize = 5f * s;
            final float markY = top + font.height(size) / 2f - WLScreen.hairlinePx() / 2f;
            final int markColor = theme.secondaryLabel();
            graphics.fill(left, markY, left + markSize, markY + Math.max(WLScreen.hairlinePx(), 1f * s), markColor, null);
            if (!this.expanded[group.ordinal()]) {
                final float cx = left + markSize / 2f;
                graphics.fill(cx - Math.max(WLScreen.hairlinePx(), 1f * s) / 2f, markY - markSize / 2f, cx + Math.max(WLScreen.hairlinePx(), 1f * s) / 2f, markY + markSize / 2f, markColor, null);
            }
            font.extract(graphics, group.title, left + markSize + 5f * s, top, size, hovered ? theme.label() : theme.secondaryLabel());
            if (!trailing.isEmpty()) {
                font.extract(graphics, trailing, right - font.width(trailing, size), top, size, theme.secondaryLabel());
            }
        }
        return top + ROW_HEIGHT * s;
    }

    private float costRow(final WLGraphics2D graphics, final float left, final float right, final float top, final String label, final double ms, final double basis, final int barColor, final Theme theme) {
        final float s = this.scale;
        if (graphics != null) {
            final float share = (float) Mth.clamp(ms / basis, 0.0, 1.0);
            if (share > 0.002f) {
                final int fill = argb(barColor & 0xFFFFFF, 0.22f);
                graphics.fillRoundedGradient(left - 3f * s, top - 1f * s, left - 3f * s + (this.rowWidth + 6f * s) * share, top + ROW_HEIGHT * s - 2f * s, fill, fill, fill, fill, 2.5f * s);
            }
            final MsdfFont font = Type.CAPTION1.regular();
            final float size = Type.CAPTION1.size() * s;
            font.extract(graphics, label, left, top, size, theme.secondaryLabel());
            final String value = fmt(ms, 2) + " ms";
            font.extract(graphics, value, right - font.width(value, size), top, size, theme.label());
        }
        return top + ROW_HEIGHT * s;
    }

    private float valueRow(final WLGraphics2D graphics, final float left, final float right, final float top, final String label, final String value, final Theme theme) {
        final float s = this.scale;
        if (graphics != null) {
            final MsdfFont font = Type.CAPTION1.regular();
            final float size = Type.CAPTION1.size() * s;
            font.extract(graphics, label, left, top, size, theme.secondaryLabel());
            font.extract(graphics, value, right - font.width(value, size), top, size, theme.label());
        }
        return top + ROW_HEIGHT * s;
    }

    private float checkboxRow(final WLGraphics2D graphics, final float left, final float right, final float top, final UiProfiler.Bypass bypass, final int mouseX, final int mouseY, final Theme theme) {
        final float s = this.scale;
        if (graphics != null) {
            final MsdfFont font = Type.CAPTION1.regular();
            final float size = Type.CAPTION1.size() * s;
            final float box = CHECKBOX * s;
            final boolean on = UiProfiler.bypassFlipped(bypass);
            final boolean hovered = this.inRect(mouseX, mouseY, this.x, top, this.width, ROW_HEIGHT * s);
            WLRenderSystem.cursors().requestIf(hovered, Cursors.Cursor.POINTER);
            final float boxY = top + (font.height(size) - box) / 2f;
            final int boxColor = on ? theme.accent() : argb(theme.isDark() ? 0xFFFFFF : 0x000000, hovered ? 0.22f : 0.12f);
            graphics.fillRoundedGradient(left, boxY, left + box, boxY + box, boxColor, boxColor, boxColor, boxColor, 2.5f * s);
            if (on) {
                final int mark = theme.onAccent();
                final float inset = box * 0.3f;
                graphics.fillRoundedGradient(left + inset, boxY + inset, left + box - inset, boxY + box - inset, mark, mark, mark, mark, 1f * s);
            }
            font.extract(graphics, bypass.label(), left + box + 6f * s, top, size, on ? theme.label() : theme.secondaryLabel());
            if (on) {
                final String tag = "off";
                font.extract(graphics, tag, right - font.width(tag, size), top, size, theme.accent());
            }
        }
        return top + ROW_HEIGHT * s;
    }

    private void chip(final WLGraphics2D graphics, final float cx, final float cy, final float cw, final float ch, final String label, final boolean hovered, final boolean active, final Theme theme) {
        WLRenderSystem.cursors().requestIf(hovered, Cursors.Cursor.POINTER);
        final int fill = active ? theme.accent() : argb(theme.isDark() ? 0xFFFFFF : 0x000000, hovered ? 0.22f : 0.10f);
        graphics.fillRoundedGradient(cx, cy, cx + cw, cy + ch, fill, fill, fill, fill, ch * 0.34f);
        final MsdfFont font = Type.CAPTION1.emphasized();
        final float size = Type.CAPTION1.size() * this.scale;
        font.extract(graphics, label, cx + (cw - font.width(label, size)) / 2f, cy + (ch - font.height(size)) / 2f, size, active ? theme.onAccent() : theme.label());
    }

    private float chipWidth(final String label) {
        final MsdfFont font = Type.CAPTION1.emphasized();
        return font.width(label, Type.CAPTION1.size() * this.scale) + 11f * this.scale;
    }

    public boolean mouseClicked(final double mx, final double my) {
        if (!WalksyLibConstants.DEV_BUILD || !this.open || !this.laidOut || !this.contains(mx, my)) {
            return false;
        }
        this.pressStartX = (float) mx;
        this.pressStartY = (float) my;
        this.dragMoved = false;
        if (this.compact) {
            this.dragging = true;
            this.dragOffsetX = (float) mx - this.x;
            this.dragOffsetY = (float) my - this.y;
            return true;
        }
        if (this.inRect(mx, my, this.minimiseX, this.buttonY, this.minimiseW, this.buttonH)) {
            this.compact = true;
            return true;
        }
        if (this.inRect(mx, my, this.closeX, this.buttonY, this.closeW, this.buttonH)) {
            this.close();
            return true;
        }
        if (this.inRect(mx, my, this.baselineX, this.buttonY, this.baselineW, this.buttonH)) {
            if (UiProfiler.hasBaseline()) {
                UiProfiler.clearBaseline();
            } else {
                UiProfiler.takeBaseline();
            }
            return true;
        }
        for (final Group group : Group.values()) {
            if (this.inRect(mx, my, this.x, this.groupHeaderY[group.ordinal()], this.width, ROW_HEIGHT * this.scale)) {
                this.expanded[group.ordinal()] = !this.expanded[group.ordinal()];
                return true;
            }
        }
        final UiProfiler.Bypass[] bypasses = UiProfiler.Bypass.values();
        for (int i = 0; i < bypasses.length; i++) {
            if (this.inRect(mx, my, this.x, this.bypassRowY[i], this.width, ROW_HEIGHT * this.scale)) {
                UiProfiler.toggleBypass(bypasses[i]);
                return true;
            }
        }
        if (my < this.y + TITLE_HEIGHT * this.scale) {
            this.dragging = true;
            this.dragOffsetX = (float) mx - this.x;
            this.dragOffsetY = (float) my - this.y;
        }
        return true;
    }

    public boolean mouseDragged(final double mx, final double my) {
        if (!this.dragging) {
            return false;
        }
        if (Math.abs(mx - this.pressStartX) > CLICK_SLOP * this.scale || Math.abs(my - this.pressStartY) > CLICK_SLOP * this.scale) {
            this.dragMoved = true;
        }
        this.x = (float) mx - this.dragOffsetX;
        this.y = (float) my - this.dragOffsetY;
        return true;
    }

    public boolean mouseReleased() {
        final boolean was = this.dragging;
        this.dragging = false;
        if (was && this.compact && !this.dragMoved) {
            this.compact = false;
        }
        this.dragMoved = false;
        return was;
    }

    public boolean contains(final double mx, final double my) {
        return WalksyLibConstants.DEV_BUILD && this.open && this.laidOut && mx >= this.x && mx < this.x + this.width && my >= this.y && my < this.y + this.height;
    }

    private boolean inRect(final double mx, final double my, final float rx, final float ry, final float rw, final float rh) {
        return mx >= rx && mx < rx + rw && my >= ry && my < ry + rh;
    }

    private static int plate(final Theme theme) {
        return 0xFF000000 | (theme.flatSurface() & 0xFFFFFF);
    }

    private static int argb(final int rgb, final float alpha) {
        return (Math.round(Mth.clamp(alpha, 0f, 1f) * 255f) << 24) | (rgb & 0xFFFFFF);
    }

    private static String whole(final UiProfiler.Counter counter) {
        return String.valueOf(Math.round(UiProfiler.counter(counter)));
    }

    private static String mpx(final UiProfiler.Counter counter) {
        final double value = UiProfiler.counter(counter) * 1e-6;
        return fmt(value, value < 10 ? 2 : 1) + " Mpx";
    }

    private static String fmt(final double value, final int decimals) {
        return String.format("%." + decimals + "f", value);
    }
}
