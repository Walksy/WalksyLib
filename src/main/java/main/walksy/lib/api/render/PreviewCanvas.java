package main.walksy.lib.api.render;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.render.DevicePixels;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.element.WLElement;
import main.walksy.lib.core.render.element.WLElementKind;
import main.walksy.lib.core.render.element.WLRect;
import main.walksy.lib.core.render.element.WLVertexSink;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.utils.ColorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PreviewCanvas {
    private static final float FRAME_FILL = 0.42f;
    private static final float ITEM_PIXELS = 16f;
    private static final float LINE_DEPTH_BIAS = 1.0E-3f;
    private static final float MIN_LINE_PIXELS = 1f;
    private static final int OCCLUSION_STEPS = 24;
    private static final float OCCLUSION_EPSILON = 1.0E-3f;
    private static final float TOGGLE_SIZE = 20f;
    private static final float TOGGLE_MARGIN = 6f;
    private final WLGraphics2D graphics;
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private final float alpha;
    private final float uiScale;
    private final float time;
    private final float yaw;
    private final float pitch;
    private final List<Line> lines = new ArrayList<>();
    private final List<Face> faces = new ArrayList<>();
    private final List<Toggle> toggles = new ArrayList<>();
    private float scale = 1f;
    private float centerX;
    private float centerY;
    private float centerZ;
    private @Nullable Entity entity;

    private record Quad(float[] points, int[] colors, float depth) {
    }

    private record Line(float[] from, float[] to, int fromColor, int toColor, float width) {
    }

    private record Face(float[] corners, int[] colors) {
    }

    public record Toggle(float x, float y, float size, String itemId, boolean on, Runnable action) {
    }

    public PreviewCanvas(final WLGraphics2D graphics, final float x, final float y, final float width, final float height, final float alpha, final float uiScale, final float time, final float yaw, final float pitch) {
        this.graphics = graphics;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.alpha = alpha;
        this.uiScale = uiScale;
        this.time = time;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public WLGraphics2D graphics() {
        return this.graphics;
    }

    public float x() {
        return this.x;
    }

    public float y() {
        return this.y;
    }

    public float width() {
        return this.width;
    }

    public float height() {
        return this.height;
    }

    public float alpha() {
        return this.alpha;
    }

    public float uiScale() {
        return this.uiScale;
    }

    public float time() {
        return this.time;
    }

    public float pixelsPerBlock() {
        return this.scale;
    }

    public int labelColor() {
        return Theme.get().label();
    }

    public float textWidth(final String text, final float size) {
        return Fonts.SFPRO_DISPLAY.width(text, size);
    }

    public void text(final String text, final float x, final float baselineY, final float size, final int color) {
        Fonts.SFPRO_DISPLAY.extract(this.graphics, text, x, Fonts.SFPRO_DISPLAY.topForBaseline(baselineY, size), size, QuadBatch.fade(color, this.alpha));
    }

    public void centeredText(final String text, final float centerX, final float baselineY, final float size, final int color) {
        this.text(text, centerX - this.textWidth(text, size) / 2f, baselineY, size, color);
    }

    public void blit(final Identifier texture, final float x, final float y, final float width, final float height, final float u0, final float v0, final float u1, final float v1, final int color) {
        this.graphics.blitUv(texture, x, y, width, height, u0, v0, u1, v1, QuadBatch.fade(color, this.alpha));
    }

    public void toggle(final String itemId, final boolean on, final Runnable action) {
        final float size = TOGGLE_SIZE * this.uiScale;
        final float margin = TOGGLE_MARGIN * this.uiScale;
        final float toggleX = this.x + this.width - margin - size - this.toggles.size() * (size + margin);
        this.toggles.add(new Toggle(toggleX, this.y + margin, size, itemId, on, action));
    }

    public List<Toggle> toggles() {
        return List.copyOf(this.toggles);
    }

    public void frame(final float minX, final float minY, final float minZ, final float maxX, final float maxY, final float maxZ) {
        this.centerX = (minX + maxX) / 2f;
        this.centerY = (minY + maxY) / 2f;
        this.centerZ = (minZ + maxZ) / 2f;
        final float dx = maxX - minX;
        final float dy = maxY - minY;
        final float dz = maxZ - minZ;
        final float radius = Math.max(1.0E-3f, 0.5f * (float) Math.sqrt(dx * dx + dy * dy + dz * dz));
        this.scale = Math.min(this.width, this.height) * FRAME_FILL / radius;
    }

    public void line(final float x1, final float y1, final float z1, final float x2, final float y2, final float z2, final int from, final int to, final float lineWidth) {
        this.lines.add(new Line(new float[] {x1, y1, z1}, new float[] {x2, y2, z2}, from, to, lineWidth));
    }

    public void entity(final Entity entity) {
        this.entity = entity;
    }

    public void quad(final float x1, final float y1, final float z1, final int c1, final float x2, final float y2, final float z2, final int c2, final float x3, final float y3, final float z3, final int c3, final float x4, final float y4, final float z4, final int c4) {
        this.faces.add(new Face(new float[] {x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4}, new int[] {c1, c2, c3, c4}));
    }

    public void box(final float minX, final float minY, final float minZ, final float maxX, final float maxY, final float maxZ, final int bottom, final int top, final float lineWidth) {
        this.line(minX, minY, minZ, maxX, minY, minZ, bottom, bottom, lineWidth);
        this.line(maxX, minY, minZ, maxX, minY, maxZ, bottom, bottom, lineWidth);
        this.line(maxX, minY, maxZ, minX, minY, maxZ, bottom, bottom, lineWidth);
        this.line(minX, minY, maxZ, minX, minY, minZ, bottom, bottom, lineWidth);
        this.line(minX, maxY, minZ, maxX, maxY, minZ, top, top, lineWidth);
        this.line(maxX, maxY, minZ, maxX, maxY, maxZ, top, top, lineWidth);
        this.line(maxX, maxY, maxZ, minX, maxY, maxZ, top, top, lineWidth);
        this.line(minX, maxY, maxZ, minX, maxY, minZ, top, top, lineWidth);
        this.line(minX, minY, minZ, minX, maxY, minZ, bottom, top, lineWidth);
        this.line(maxX, minY, minZ, maxX, maxY, minZ, bottom, top, lineWidth);
        this.line(maxX, minY, maxZ, maxX, maxY, maxZ, bottom, top, lineWidth);
        this.line(minX, minY, maxZ, minX, maxY, maxZ, bottom, top, lineWidth);
    }

    public void filledBox(final float minX, final float minY, final float minZ, final float maxX, final float maxY, final float maxZ, final int bottom, final int top) {
        this.quad(minX, minY, minZ, bottom, maxX, minY, minZ, bottom, maxX, minY, maxZ, bottom, minX, minY, maxZ, bottom);
        this.quad(minX, maxY, minZ, top, minX, maxY, maxZ, top, maxX, maxY, maxZ, top, maxX, maxY, minZ, top);
        this.quad(minX, minY, minZ, bottom, minX, maxY, minZ, top, maxX, maxY, minZ, top, maxX, minY, minZ, bottom);
        this.quad(minX, minY, maxZ, bottom, maxX, minY, maxZ, bottom, maxX, maxY, maxZ, top, minX, maxY, maxZ, top);
        this.quad(minX, minY, minZ, bottom, minX, minY, maxZ, bottom, minX, maxY, maxZ, top, minX, maxY, minZ, top);
        this.quad(maxX, minY, minZ, bottom, maxX, maxY, minZ, top, maxX, maxY, maxZ, top, maxX, minY, maxZ, bottom);
    }

    public void item(final ItemStack stack, final float centerX, final float centerY, final float size) {
        this.flush();
        final float itemScale = size / ITEM_PIXELS;
        this.graphics.pose().pushMatrix();
        this.graphics.pose().translate(centerX - size / 2f, centerY - size / 2f);
        this.graphics.pose().scale(itemScale, itemScale);
        this.graphics.item(stack, 0, 0);
        this.graphics.pose().popMatrix();
    }

    public void flush() {
        final Entity drawn = this.entity;
        this.entity = null;
        final Occluder occluder = drawn == null ? null : this.occluder(drawn);
        final List<Quad> behind = new ArrayList<>();
        final List<Quad> front = new ArrayList<>();
        for (final Face face : this.faces) {
            this.addFace(face, occluder, behind, front);
        }
        for (final Line line : this.lines) {
            this.addLine(line, occluder, behind, front);
        }
        this.faces.clear();
        this.lines.clear();
        behind.sort(Comparator.comparingDouble(Quad::depth));
        front.sort(Comparator.comparingDouble(Quad::depth));
        this.submit(behind);
        if (drawn != null) {
            final Quaternionf rotation = new Quaternionf().rotateX((float) Math.PI + this.pitch).rotateY(this.yaw);
            final Vector3f translation = rotation.transform(new Vector3f(-this.centerX, -this.centerY, -this.centerZ));
            try {
                WalksyLib.getInstance().getPreviewEntities().render(this.graphics, drawn, this.x + this.width / 2f, this.y + this.height / 2f, this.scale, translation, rotation, this.x, this.y, this.width, this.height, this.alpha);
            } catch (final RuntimeException broken) {
                WalksyLib.getInstance().getPreviewEntities().forget(drawn);
            }
        }
        this.submit(front);
    }

    private void addFace(final Face face, @Nullable final Occluder occluder, final List<Quad> behind, final List<Quad> front) {
        final float[] c = face.corners();
        final float[] points = new float[8];
        float depth = 0f;
        for (int i = 0; i < 4; i++) {
            final float[] projected = this.project(c[i * 3], c[i * 3 + 1], c[i * 3 + 2]);
            points[i * 2] = projected[0];
            points[i * 2 + 1] = projected[1];
            depth += projected[2] / 4f;
        }
        final boolean hidden = occluder != null && occluder.hides((c[0] + c[3] + c[6] + c[9]) / 4f, (c[1] + c[4] + c[7] + c[10]) / 4f, (c[2] + c[5] + c[8] + c[11]) / 4f);
        (hidden ? behind : front).add(new Quad(points, face.colors(), depth));
    }

    private void addLine(final Line line, @Nullable final Occluder occluder, final List<Quad> behind, final List<Quad> front) {
        if (occluder == null) {
            this.addSegment(line, 0f, 1f, front);
            return;
        }
        float runStart = 0f;
        boolean runHidden = hidden(line, occluder, 0.5f / OCCLUSION_STEPS);
        for (int step = 1; step < OCCLUSION_STEPS; step++) {
            final boolean hidden = hidden(line, occluder, (step + 0.5f) / OCCLUSION_STEPS);
            if (hidden != runHidden) {
                final float runEnd = (float) step / OCCLUSION_STEPS;
                this.addSegment(line, runStart, runEnd, runHidden ? behind : front);
                runStart = runEnd;
                runHidden = hidden;
            }
        }
        this.addSegment(line, runStart, 1f, runHidden ? behind : front);
    }

    private static boolean hidden(final Line line, final Occluder occluder, final float t) {
        final float[] a = line.from();
        final float[] b = line.to();
        return occluder.hides(a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t);
    }

    private void addSegment(final Line line, final float t0, final float t1, final List<Quad> target) {
        final float[] from = line.from();
        final float[] to = line.to();
        final float[] start = this.project(from[0], from[1], from[2]);
        final float[] end = this.project(to[0], to[1], to[2]);
        final float dx = end[0] - start[0];
        final float dy = end[1] - start[1];
        final float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1.0E-4f) {
            return;
        }
        final float devicePixels = this.devicePixels();
        final float windowScale = this.height * devicePixels / Math.max(1, Minecraft.getInstance().getWindow().getHeight());
        final float half = Math.max(MIN_LINE_PIXELS, line.width() * windowScale) * 0.5f / devicePixels;
        final float ux = dx / length * half;
        final float uy = dy / length * half;
        final float startCap = t0 <= 0f ? 1f : 0f;
        final float endCap = t1 >= 1f ? 1f : 0f;
        final float ax = start[0] + dx * t0 - ux * startCap;
        final float ay = start[1] + dy * t0 - uy * startCap;
        final float bx = start[0] + dx * t1 + ux * endCap;
        final float by = start[1] + dy * t1 + uy * endCap;
        final float depth = start[2] + (end[2] - start[2]) * (t0 + t1) / 2f;
        final int colorA = ColorUtils.lerpColor(line.fromColor(), line.toColor(), t0);
        final int colorB = ColorUtils.lerpColor(line.fromColor(), line.toColor(), t1);
        target.add(new Quad(new float[] {ax - uy, ay + ux, bx - uy, by + ux, bx + uy, by - ux, ax + uy, ay - ux}, new int[] {colorA, colorB, colorB, colorA}, depth + LINE_DEPTH_BIAS));
    }

    private Occluder occluder(final Entity entity) {
        final AABB box = entity.getBoundingBox().move(-entity.getX(), -entity.getY(), -entity.getZ());
        final Vector3f toViewer = new Quaternionf().rotateX(this.pitch).rotateY(this.yaw).conjugate().transform(new Vector3f(0f, 0f, 1f));
        final float size = (float) Math.max(box.getXsize(), Math.max(box.getYsize(), box.getZsize()));
        return new Occluder(box, toViewer, OCCLUSION_EPSILON * Math.max(1f, size));
    }

    private void submit(final List<Quad> batch) {
        if (batch.isEmpty()) {
            return;
        }
        final Matrix3x2f pose = new Matrix3x2f(this.graphics.pose());
        final WLRect area = new WLRect((int) Math.floor(this.x), (int) Math.floor(this.y), (int) Math.ceil(this.width), (int) Math.ceil(this.height)).transformMaxBounds(pose);
        final WLRect clip = WLRect.clipped(area, this.graphics.scissor());
        if (clip == null) {
            return;
        }
        this.graphics.submit(new QuadBatch(pose, List.copyOf(batch), this.alpha, clip));
    }

    private float devicePixels() {
        final Matrix3x2fc pose = this.graphics.pose();
        final float poseScale = (float) Math.sqrt(pose.m00() * pose.m00() + pose.m01() * pose.m01());
        return Math.max(1.0E-3f, DevicePixels.perLogicalX() * poseScale);
    }

    private float[] project(final float px, final float py, final float pz) {
        final float lx = px - this.centerX;
        final float ly = py - this.centerY;
        final float lz = pz - this.centerZ;
        final float cosYaw = (float) Math.cos(this.yaw);
        final float sinYaw = (float) Math.sin(this.yaw);
        final float rx = lx * cosYaw + lz * sinYaw;
        final float rz = -lx * sinYaw + lz * cosYaw;
        final float cosPitch = (float) Math.cos(this.pitch);
        final float sinPitch = (float) Math.sin(this.pitch);
        final float ry = ly * cosPitch - rz * sinPitch;
        final float depth = ly * sinPitch + rz * cosPitch;
        return new float[] {this.x + this.width / 2f + rx * this.scale, this.y + this.height / 2f - ry * this.scale, depth};
    }

    private record Occluder(AABB box, Vector3f toViewer, float epsilon) {
        private boolean hides(final float px, final float py, final float pz) {
            final float[] point = {px, py, pz};
            final float[] direction = {this.toViewer.x(), this.toViewer.y(), this.toViewer.z()};
            final double[] min = {this.box.minX, this.box.minY, this.box.minZ};
            final double[] max = {this.box.maxX, this.box.maxY, this.box.maxZ};
            float enter = 0f;
            float exit = Float.POSITIVE_INFINITY;
            for (int axis = 0; axis < 3; axis++) {
                if (Math.abs(direction[axis]) < 1.0E-6f) {
                    if (point[axis] < min[axis] - this.epsilon || point[axis] > max[axis] + this.epsilon) {
                        return false;
                    }
                    continue;
                }
                final float near = (float) ((min[axis] - point[axis]) / direction[axis]);
                final float far = (float) ((max[axis] - point[axis]) / direction[axis]);
                enter = Math.max(enter, Math.min(near, far));
                exit = Math.min(exit, Math.max(near, far));
            }
            return exit - enter > this.epsilon;
        }
    }

    private record QuadBatch(Matrix3x2fc pose, List<Quad> quads, float alpha, @Nullable WLRect scissorArea) implements WLElement {
        @Override
        public WLElementKind kind() {
            return WLElementKind.FLAT;
        }

        @Override
        public @Nullable WLRect bounds() {
            return this.scissorArea;
        }

        @Override
        public void emit(final WLVertexSink sink) {
            for (final Quad quad : this.quads) {
                for (int i = 0; i < 4; i++) {
                    sink.posed(this.pose, quad.points()[i * 2], quad.points()[i * 2 + 1]).color(fade(quad.colors()[i], this.alpha));
                }
            }
        }

        private static int fade(final int argb, final float alpha) {
            if (alpha >= 1f) {
                return argb;
            }
            return (Math.round(((argb >>> 24) & 0xFF) * alpha) << 24) | (argb & 0x00FFFFFF);
        }
    }
}
