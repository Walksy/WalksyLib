package main.walksy.lib.core.render.perf;

import main.walksy.lib.core.WalksyLibConstants;

import java.util.Arrays;

public final class UiProfiler {
    public static final int HISTORY = 8192;
    private static final Section[] SECTIONS = Section.values();
    private static final Counter[] COUNTERS = Counter.values();
    private static final Bypass[] BYPASSES = Bypass.values();
    private static final long[] ACCUMULATED = new long[SECTIONS.length];
    private static final long[] OPENED_AT = new long[SECTIONS.length];
    private static final double[] SMOOTHED_MS = new double[SECTIONS.length];
    private static final long[] COUNTS = new long[COUNTERS.length];
    private static final double[] SMOOTHED_COUNTS = new double[COUNTERS.length];
    private static final boolean[] BYPASSED = new boolean[BYPASSES.length];
    private static final float[] FRAME_HISTORY = new float[HISTORY];
    private static int historyHead;
    private static int historyFilled;
    private static long lastFrameStart;
    private static double smoothedFrameMs;
    private static double peakFrameMs;
    private static double baselineMs;
    private static boolean enabled;
    private static final double SMOOTHING = 0.06;
    public static final int SPARKLINE_FRAMES = 180;

    public enum Section {
        SCREEN("Screen chrome", "Panel sheet, sidebar, title, modal and popover"),
        WIDGETS("Option rows", "Every child widget's extract: the detail pane and the sidebar list"),
        GLASS_UPLOAD("Glass uniforms", "Packing every glass shape into its layer's UBO"),
        STRATA_CLEAR("  overlay clears", "The three full-target clears the strata need"),
        STRATA_SHAPES("  stratum shapes", "Building the row/popover shape vertex buffers"),
        STRATA_TEXT("  stratum text", "Building the row/popover glyph vertex buffers"),
        BLUR("Blur (record)", "Recording the frost pyramid; its real cost is in the counters"),
        CAPTURE("Capture (record)", "Recording the per-layer colour-target copies"),
        COMPOSITE("Composite (record)", "Recording one fullscreen glass pass per layer"),
        TEXT_TAIL("Text (tail)", "Every label drawn on top of the finished glass"),
        MONITOR("This monitor", "What the window you are reading costs to draw");

        private final String label;
        private final String detail;

        Section(final String label, final String detail) {
            this.label = label;
            this.detail = detail;
        }

        public String label() {
            return this.label;
        }

        public String detail() {
            return this.detail;
        }
    }

    public enum Counter {
        COMPOSITE_PASSES("Composite passes", "", 1.0),
        COMPOSITE_PIXELS("Composite shaded", " Mpx", 1e-6),
        CAPTURE_PIXELS("Colour-target copies", " Mpx", 1e-6),
        BLUR_PASSES("Blur passes", "", 1.0),
        BLUR_PIXELS("Blur shaded", " Mpx", 1e-6),
        BLUR_TAPS("Blur texture reads", "M", 1e-6),
        OVERLAY_PIXELS("Overlay clears", " Mpx", 1e-6),
        RENDER_PASSES("Render passes", "", 1.0),
        GLASS_SHAPES("Glass shapes", "", 1.0),
        GLASS_LAYERS("Glass layers", "", 1.0),
        STRATUM_SHAPES("Stratum shapes", "", 1.0),
        GLYPHS("Glyphs", "", 1.0);

        private final String label;
        private final String unit;
        private final double scale;

        Counter(final String label, final String unit, final double scale) {
            this.label = label;
            this.unit = unit;
            this.scale = scale;
        }

        public String label() {
            return this.label;
        }

        public String unit() {
            return this.unit;
        }

        public double scale() {
            return this.scale;
        }
    }

    public enum Bypass {
        COMPOSITE("Glass composite", "Skip every glass pass. The window loses all its glass; text and flat fills stay."),
        BLUR("Blur pyramid", "Build no frost. Glass still refracts, but samples the sharp capture."),
        SCISSOR("Pass scissors", "Shade every pass edge to edge instead of only where it can change a pixel."),
        CAPTURE("Scene re-capture", "Reuse the first pass's snapshot for every later pass. Glass stops lensing glass."),
        STRATA("Overlay strata", "Skip the row and popover overlays. Their text falls back to the tail pass."),
        TEXT("Font rendering", "Draw no glyphs at all, in any queue. Every label in the window disappears.");

        private final String label;
        private final String detail;

        Bypass(final String label, final String detail) {
            this.label = label;
            this.detail = detail;
        }

        public String label() {
            return this.label;
        }

        public String detail() {
            return this.detail;
        }
    }

    private UiProfiler() {
    }

    public static boolean isEnabled() {
        return WalksyLibConstants.DEV_BUILD && enabled;
    }

    public static void setEnabled(final boolean on) {
        if (!WalksyLibConstants.DEV_BUILD || enabled == on) {
            return;
        }
        enabled = on;
        if (!on) {
            reset();
        }
    }

    private static void reset() {
        Arrays.fill(ACCUMULATED, 0L);
        Arrays.fill(OPENED_AT, 0L);
        Arrays.fill(SMOOTHED_MS, 0.0);
        Arrays.fill(COUNTS, 0L);
        Arrays.fill(SMOOTHED_COUNTS, 0.0);
        Arrays.fill(FRAME_HISTORY, 0f);
        historyHead = 0;
        historyFilled = 0;
        lastFrameStart = 0L;
        smoothedFrameMs = 0.0;
        peakFrameMs = 0.0;
    }

    public static void beginFrame() {
        if (!WalksyLibConstants.DEV_BUILD || !enabled) {
            return;
        }
        final long now = System.nanoTime();
        if (lastFrameStart != 0L) {
            final double frameMs = (now - lastFrameStart) / 1_000_000.0;
            pushHistory((float) frameMs);
            smoothedFrameMs = smoothedFrameMs <= 0.0 ? frameMs : smoothedFrameMs + (frameMs - smoothedFrameMs) * SMOOTHING;
            peakFrameMs = Math.max(frameMs, peakFrameMs * 0.995);
            for (int i = 0; i < SECTIONS.length; i++) {
                final double ms = ACCUMULATED[i] / 1_000_000.0;
                SMOOTHED_MS[i] += (ms - SMOOTHED_MS[i]) * SMOOTHING;
            }
            for (int i = 0; i < COUNTERS.length; i++) {
                SMOOTHED_COUNTS[i] += (COUNTS[i] - SMOOTHED_COUNTS[i]) * SMOOTHING;
            }
        }
        lastFrameStart = now;
        Arrays.fill(ACCUMULATED, 0L);
        Arrays.fill(OPENED_AT, 0L);
        Arrays.fill(COUNTS, 0L);
    }

    private static void pushHistory(final float ms) {
        FRAME_HISTORY[historyHead] = ms;
        historyHead = (historyHead + 1) % HISTORY;
        if (historyFilled < HISTORY) {
            historyFilled++;
        }
    }

    public static void begin(final Section section) {
        if (!WalksyLibConstants.DEV_BUILD || !enabled) {
            return;
        }
        final int i = section.ordinal();
        if (OPENED_AT[i] == 0L) {
            OPENED_AT[i] = System.nanoTime();
        }
    }

    public static void end(final Section section) {
        if (!WalksyLibConstants.DEV_BUILD || !enabled) {
            return;
        }
        final int i = section.ordinal();
        if (OPENED_AT[i] != 0L) {
            ACCUMULATED[i] += System.nanoTime() - OPENED_AT[i];
            OPENED_AT[i] = 0L;
        }
    }

    public static void count(final Counter counter, final long amount) {
        if (!WalksyLibConstants.DEV_BUILD || !enabled) {
            return;
        }
        COUNTS[counter.ordinal()] += amount;
    }

    public static void compositePass(final long pixels) {
        count(Counter.COMPOSITE_PASSES, 1L);
        count(Counter.COMPOSITE_PIXELS, pixels);
    }

    public static void blurPass(final long pixels) {
        count(Counter.BLUR_PASSES, 1L);
        count(Counter.BLUR_PIXELS, pixels);
    }

    public static double frameMs() {
        return smoothedFrameMs;
    }

    public static double peakMs() {
        return peakFrameMs;
    }

    public static double fps() {
        return smoothedFrameMs <= 0.0001 ? 0.0 : 1000.0 / smoothedFrameMs;
    }

    public static double sectionMs(final Section section) {
        return SMOOTHED_MS[section.ordinal()];
    }

    public static double counter(final Counter counter) {
        return SMOOTHED_COUNTS[counter.ordinal()];
    }

    public static double libraryCpuMs() {
        double total = 0.0;
        for (final Section section : SECTIONS) {
            total += SMOOTHED_MS[section.ordinal()];
        }
        return total;
    }

    public static double unattributedMs() {
        return Math.max(0.0, smoothedFrameMs - libraryCpuMs());
    }

    public static int sparklineCount() {
        return Math.min(historyFilled, SPARKLINE_FRAMES);
    }

    public static float sparklineAt(final int index) {
        return historyAt(historyFilled - sparklineCount() + index);
    }

    public static int historyCount() {
        return historyFilled;
    }

    public static float historyAt(final int index) {
        final int start = (historyHead - historyFilled + HISTORY) % HISTORY;
        return FRAME_HISTORY[(start + index) % HISTORY];
    }

    public static boolean isBypassed(final Bypass bypass) {
        return WalksyLibConstants.DEV_BUILD && enabled && BYPASSED[bypass.ordinal()];
    }

    public static boolean bypassFlipped(final Bypass bypass) {
        return BYPASSED[bypass.ordinal()];
    }

    public static void toggleBypass(final Bypass bypass) {
        if (!WalksyLibConstants.DEV_BUILD) {
            return;
        }
        BYPASSED[bypass.ordinal()] = !BYPASSED[bypass.ordinal()];
    }

    public static void clearBypasses() {
        Arrays.fill(BYPASSED, false);
    }

    public static boolean anyBypassed() {
        for (final boolean flag : BYPASSED) {
            if (flag) {
                return true;
            }
        }
        return false;
    }

    public static void takeBaseline() {
        baselineMs = smoothedFrameMs;
    }

    public static void clearBaseline() {
        baselineMs = 0.0;
    }

    public static boolean hasBaseline() {
        return baselineMs > 0.0;
    }

    public static double baselineMs() {
        return baselineMs;
    }
}
