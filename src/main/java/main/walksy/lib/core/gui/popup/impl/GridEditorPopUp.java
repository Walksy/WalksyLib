package main.walksy.lib.core.gui.popup.impl;

import main.walksy.lib.core.config.local.options.type.PixelGrid;
import main.walksy.lib.core.gui.Graphics;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.popup.PopUp;
import main.walksy.lib.core.gui.widgets.ButtonWidget;
import main.walksy.lib.core.utils.Clipboard;
import main.walksy.lib.core.utils.MainColors;
import main.walksy.lib.core.utils.Scroller;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.awt.*;
import java.util.*;
import java.util.function.Consumer;

public class GridEditorPopUp extends PopUp {

    private final PixelGrid currentGrid;
    private final ButtonWidget undoButton;
    private final ButtonWidget undoAllButton;
    private final ButtonWidget doneButton;
    private final ButtonWidget clearButton;
    private final ButtonWidget copyButton;
    private final ButtonWidget pasteButton;
    private boolean isMouseDown = false;
    private boolean drawState = true;
    private final Set<Point> modifiedCells = new HashSet<>();

    private record UndoAction(Point point, boolean previousState) { }

    private final Deque<UndoAction> undoStack = new LinkedList<>();
    private final Map<Point, Boolean> fullBackup = new HashMap<>();

    private final Scroller scroller;
    private final int index;

    public GridEditorPopUp(final WalksyLibConfigScreen screen, final PixelGrid grid, final Consumer<PixelGrid> onDone, final int currentIndex) {
        super(screen, "Grid Editor", 280, 320);
        this.currentGrid = grid.copy();
        this.index = currentIndex;
        this.scroller = new Scroller(0, 2);

        this.undoButton = new ButtonWidget(this.x + 5, this.y + this.height - 21, 40, 16, false, "Undo", null);
        this.undoAllButton = new ButtonWidget(this.x + 50, this.y + this.height - 21, 60, 16, false, "Undo All", null);
        this.doneButton = new ButtonWidget(this.x + this.width - 51, this.y + this.height - 21, 40, 16, false, "Done", () -> {
            if (onDone != null) {
                onDone.accept(this.currentGrid);
            }
            screen.popUp.close();
        });
        this.clearButton = new ButtonWidget(this.x + 115, this.y + this.height - 21, 40, 16, false, "Clear", () -> {
            for (int py = 0; py < this.currentGrid.getHeight(); py++) {
                for (int px = 0; px < this.currentGrid.getWidth(); px++) {
                    final boolean previous = this.currentGrid.getPixel(px, py);
                    this.undoStack.push(new UndoAction(new Point(px, py), previous));
                    this.currentGrid.setPixel(px, py, false);
                }
            }
            this.modifiedCells.clear();
        });

        this.copyButton = new ButtonWidget(this.x + this.width - 44, this.y + 5, 18, 12, false, "C", () -> {
            Clipboard.grid = this.currentGrid.copy();
        });

        this.pasteButton = new ButtonWidget(this.x + this.width - 22, this.y + 5, 18, 12, false, "P", () -> {
            final PixelGrid clipboard = Clipboard.grid;
            if (clipboard != null && clipboard.getWidth() == this.currentGrid.getWidth() && clipboard.getHeight() == this.currentGrid.getHeight()) {
                for (int py = 0; py < clipboard.getHeight(); py++) {
                    for (int px = 0; px < clipboard.getWidth(); px++) {
                        final boolean previous = this.currentGrid.getPixel(px, py);
                        final boolean newVal = clipboard.getPixel(px, py);
                        if (previous != newVal) {
                            this.undoStack.push(new UndoAction(new Point(px, py), previous));
                            this.currentGrid.setPixel(px, py, newVal);
                        }
                    }
                }
                this.modifiedCells.clear();
            }
        });

        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                this.fullBackup.put(new Point(x, y), grid.getPixel(x, y));
            }
        }
    }

    @Override
    public void extract(final Graphics graphics, final double mouseX, final double mouseY, final float delta) {
        super.extract(graphics, mouseX, mouseY, delta);
        GuiGraphicsExtractor extractor = graphics.extractor();
        extractor.centeredText(Minecraft.getInstance().font, "Editing Frame: " + this.index, (this.parent.width) / 2, this.y + 8, -1);
        extractor.enableScissor(this.x, this.y + 20, this.x + this.width, this.y + this.height - 25);
        if (this.renderGridOutline(extractor, this.currentGrid, this.x + 6, (int) (this.y + 21 - this.scroller.getValue()), 16, 2, MainColors.OUTLINE_WHITE.getRGB(), true, mouseX, mouseY)) {
            this.scroller.active = false;
        }
        extractor.disableScissor();
        this.handleDrag(mouseX, mouseY);
        extractor.horizontalLine(this.x + 2, this.x + this.width - 3, this.y + this.height - 25, MainColors.OUTLINE_WHITE.getRGB());
        this.doneButton.extractRenderState(extractor, (int) mouseX, (int) mouseY, delta);
        this.undoButton.extractRenderState(extractor, (int) mouseX, (int) mouseY, delta);
        this.undoAllButton.extractRenderState(extractor, (int) mouseX, (int) mouseY, delta);
        this.clearButton.extractRenderState(extractor, (int) mouseX, (int) mouseY, delta);
        this.copyButton.extractRenderState(extractor, (int) mouseX, (int) mouseY, delta);
        this.pasteButton.extractRenderState(extractor, (int) mouseX, (int) mouseY, delta);
        this.pasteButton.setEnabled(Clipboard.grid != null);
    }

    @Override
    public void onClick(final MouseButtonEvent click, final boolean doubled) {
        final double mouseX = click.x();
        final double mouseY = click.y();
        if (this.copyButton.isHovered()) {
            this.copyButton.onClick(click, doubled);
            return;
        }
        if (this.pasteButton.isHovered()) {
            this.pasteButton.onClick(click, doubled);
            return;
        }
        if (this.undoButton.isHovered()) {
            if (!this.undoStack.isEmpty()) {
                final UndoAction action = this.undoStack.pop();
                this.currentGrid.setPixel(action.point.x, action.point.y, action.previousState);
            }
            return;
        }
        if (this.undoAllButton.isHovered()) {
            for (final Map.Entry<Point, Boolean> entry : this.fullBackup.entrySet()) {
                this.currentGrid.setPixel(entry.getKey().x, entry.getKey().y, entry.getValue());
            }
            this.undoStack.clear();
            return;
        }

        this.doneButton.onClick(click, doubled);
        this.clearButton.onClick(click, doubled);

        final int pixelSize = 16;
        final int gapSize = 2;

        for (int py = 0; py < this.currentGrid.getHeight(); py++) {
            for (int px = 0; px < this.currentGrid.getWidth(); px++) {
                final int cellX = this.x + 6 + px * (pixelSize + gapSize);
                final int cellY = (int) ((this.y + 21 - this.scroller.getValue()) + py * (pixelSize + gapSize));
                if ((mouseX >= cellX && mouseX < cellX + pixelSize && mouseY >= cellY && mouseY < cellY + pixelSize)
                        && !this.doneButton.isHovered() && !this.clearButton.isHovered() && !this.undoButton.isHovered() && !this.undoAllButton.isHovered()) {
                    final boolean current = this.currentGrid.getPixel(px, py);
                    this.drawState = !current;

                    this.undoStack.push(new UndoAction(new Point(px, py), current));
                    this.currentGrid.setPixel(px, py, this.drawState);
                    this.modifiedCells.clear();
                    this.modifiedCells.add(new Point(px, py));
                    this.isMouseDown = true;
                    return;
                }
            }
        }
    }

    @Override
    public void onMouseRelease(final MouseButtonEvent click) {
        this.isMouseDown = false;
        this.modifiedCells.clear();
    }

    @Override
    public void onScroll(final double mouseX, final double mouseY, final double verticalAmount) {
        super.onScroll(mouseX, mouseY, verticalAmount);
        this.scroller.onScroll(verticalAmount);
        this.scroller.setBounds(0, 74);
    }

    @Override
    public boolean canClose() {
        return false;
    }

    @Override
    protected void onClose() {}

    @Override
    public void layout(final int width, final int height) {
        super.layout(width, height);
        if (this.loaded) {
            this.undoButton.setPosition(this.x + 5, this.y + height - 21);
            this.undoAllButton.setPosition(this.x + 50, this.y + height - 21);
            this.doneButton.setPosition(this.x + width - 51, this.y + height - 21);
            this.clearButton.setPosition(this.x + 115, this.y + height - 21);
            this.copyButton.setPosition(this.x + width - 44, this.y + 5);
            this.pasteButton.setPosition(this.x + width - 22, this.y + 5);
        }
    }

    private void handleDrag(final double mouseX, final double mouseY) {
        if (!this.isMouseDown) return;

        final int pixelSize = 16;
        final int gapSize = 2;

        for (int py = 0; py < this.currentGrid.getHeight(); py++) {
            for (int px = 0; px < this.currentGrid.getWidth(); px++) {
                final int cellX = this.x + 6 + px * (pixelSize + gapSize);
                final int cellY = (int) ((this.y + 21 - this.scroller.getValue()) + py * (pixelSize + gapSize));
                if (mouseX >= cellX && mouseX < cellX + pixelSize && mouseY >= cellY && mouseY < cellY + pixelSize) {
                    final Point point = new Point(px, py);
                    if (this.modifiedCells.contains(point)) return;

                    final boolean previous = this.currentGrid.getPixel(px, py);
                    this.currentGrid.setPixel(px, py, this.drawState);
                    this.undoStack.push(new UndoAction(point, previous));
                    this.modifiedCells.add(point);
                    return;
                }
            }
        }
    }

    private boolean renderGridOutline(final GuiGraphicsExtractor extractor, final PixelGrid grid, final int x1, final int y1, final int pixelSize, final int gapSize, final int outlineColor, final boolean markCenter, final double mouseX, final double mouseY) {
        boolean rtrn = true;
        if (markCenter) {
            final int centerX = grid.getWidth() / 2;
            final int centerY = grid.getHeight() / 2;

            final int px = x1 + centerX * (pixelSize + gapSize);
            final int py = y1 + centerY * (pixelSize + gapSize);

            final int centerColor = new Color(255, 100, 100, 100).getRGB();
            extractor.fill(px + 1, py + 1, px + pixelSize - 1, py + pixelSize - 1, centerColor);
        }

        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                final int px = x1 + x * (pixelSize + gapSize);
                final int py = y1 + y * (pixelSize + gapSize);
                if (!extractor.containsPointInScissor(px, py)) {
                    rtrn = false;
                }

                final boolean on = grid.getPixel(x, y);
                final int fillColor = on ? Color.WHITE.getRGB() : new Color(0, 0, 0, 0).getRGB();
                extractor.fill(px + 1, py + 1, px + pixelSize - 1, py + pixelSize - 1, fillColor);

                final boolean hovered = mouseX >= px && mouseX < px + pixelSize && mouseY >= py && mouseY < py + pixelSize;
                final int actualOutlineColor = hovered ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : outlineColor;

                extractor.fill(px + 1, py, px + pixelSize - 1, py + 1, actualOutlineColor);
                extractor.fill(px + 1, py + pixelSize - 1, px + pixelSize - 1, py + pixelSize, actualOutlineColor);
                extractor.fill(px, py, px + 1, py + pixelSize, actualOutlineColor);
                extractor.fill(px + pixelSize - 1, py, px + pixelSize, py + pixelSize, actualOutlineColor);
            }
        }
        return rtrn;
    }
}
