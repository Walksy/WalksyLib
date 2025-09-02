package main.walksy.lib.core.gui.popup.impl;

import main.walksy.lib.core.config.local.options.type.PixelGrid;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.popup.PopUp;
import main.walksy.lib.core.gui.widgets.ButtonWidget;
import main.walksy.lib.core.utils.MainColors;
import main.walksy.lib.core.utils.Scroller;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.*;
import java.util.function.Consumer;
public class GridEditorPopUp extends PopUp {

    private final PixelGrid currentGrid;
    private final ButtonWidget undoButton;
    private final ButtonWidget undoAllButton;
    private final ButtonWidget doneButton;
    private final ButtonWidget clearButton;
    private boolean isMouseDown = false;
    private boolean drawState = true;
    private final Set<Point> modifiedCells = new HashSet<>();

    private record UndoAction(Point point, boolean previousState) { }

    private final Deque<UndoAction> undoStack = new LinkedList<>();
    private final Map<Point, Boolean> fullBackup = new HashMap<>();

    private final Scroller scroller;

    public GridEditorPopUp(WalksyLibConfigScreen screen, PixelGrid grid, Consumer<PixelGrid> onDone) {
        super(screen, "Grid Editor", 280, 320);
        this.currentGrid = grid.copy();
        this.scroller = new Scroller(0, 2);
        this.undoButton = new ButtonWidget(x + 5, y + height - 21, 40, 16, false, "Undo", null);
        this.undoAllButton = new ButtonWidget(x + 50, y + height - 21, 60, 16, false, "Undo All", null);
        this.doneButton = new ButtonWidget(x + width - 51, y + height - 21, 40, 16, false, "Done", () -> {
            if (onDone != null) {
                onDone.accept(currentGrid); //TODO ALT Z
            }
            screen.popUp = null;
        });
        this.clearButton = new ButtonWidget(x + 115, y + height - 21, 40, 16, false, "Clear", () -> {
            for (int py = 0; py < currentGrid.getHeight(); py++) {
                for (int px = 0; px < currentGrid.getWidth(); px++) {
                    boolean previous = currentGrid.getPixel(px, py);
                    undoStack.push(new UndoAction(new Point(px, py), previous));
                    currentGrid.setPixel(px, py, false);
                }
            }
            modifiedCells.clear();
        });

        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                fullBackup.put(new Point(x, y), grid.getPixel(x, y));
            }
        }
    }

    @Override
    public void render(DrawContext context, double mouseX, double mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, "Editing Frame: " + "idk", (parent.width) / 2, y + 8, -1);
        context.enableScissor(x, y + 20, x + width, y + height - 25);
        if (this.renderGridOutline(context, this.currentGrid, x + 6, (int) (y + 21 - scroller.getValue()), 16, 2, MainColors.OUTLINE_WHITE.getRGB(), true, mouseX, mouseY))
        {
            scroller.active = false;
        }
        context.disableScissor();
        handleDrag(mouseX, mouseY);
        context.drawHorizontalLine(x + 2, x + width - 3, y + height - 25, MainColors.OUTLINE_WHITE.getRGB());
        doneButton.render(context, (int) mouseX, (int) mouseY, delta);
        undoButton.render(context, (int) mouseX, (int) mouseY, delta);
        undoAllButton.render(context, (int) mouseX, (int) mouseY, delta);
        clearButton.render(context, (int) mouseX, (int) mouseY, delta);
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        if (undoButton.isHovered()) {
            if (!undoStack.isEmpty()) {
                UndoAction action = undoStack.pop();
                currentGrid.setPixel(action.point.x, action.point.y, action.previousState);
            }
            return;
        }

        if (undoAllButton.isHovered()) {
            for (Map.Entry<Point, Boolean> entry : fullBackup.entrySet()) {
                currentGrid.setPixel(entry.getKey().x, entry.getKey().y, entry.getValue());
            }
            undoStack.clear();
            return;
        }

        doneButton.onClick(mouseX, mouseY);
        clearButton.onClick(mouseX, mouseY);

        int pixelSize = 16;
        int gapSize = 2;

        for (int py = 0; py < currentGrid.getHeight(); py++) {
            for (int px = 0; px < currentGrid.getWidth(); px++) {
                int cellX = x + 6 + px * (pixelSize + gapSize);
                int cellY = (int) ((y + 21 - scroller.getValue()) + py * (pixelSize + gapSize));
                if ((mouseX >= cellX && mouseX < cellX + pixelSize && mouseY >= cellY && mouseY < cellY + pixelSize) && !doneButton.isHovered() && !clearButton.isHovered() && !undoButton.isHovered() && !undoAllButton.isHovered()) {
                    boolean current = currentGrid.getPixel(px, py);
                    drawState = !current;

                    undoStack.push(new UndoAction(new Point(px, py), current));
                    currentGrid.setPixel(px, py, drawState);
                    modifiedCells.clear();
                    modifiedCells.add(new Point(px, py));
                    isMouseDown = true;
                    return;
                }
            }
        }
    }

    @Override
    public void onMouseRelease(double mouseX, double mouseY, int button) {
        isMouseDown = false;
        modifiedCells.clear();
    }

    @Override
    public void onScroll(double mouseX, double mouseY, double verticalAmount) {
        super.onScroll(mouseX, mouseY, verticalAmount);
        scroller.onScroll(verticalAmount);
        scroller.setBounds(0, 74);
    }

    @Override
    public void layout(int width, int height) {
        super.layout(width, height);
        if (loaded) {
            this.undoButton.setPosition(x + 5, y + height - 21);
            this.undoAllButton.setPosition(x + 50, y + height - 21);
            this.doneButton.setPosition(x + width - 51, y + height - 21);
            this.clearButton.setPosition(x + 115, y + height - 21);
        }
    }

    private void handleDrag(double mouseX, double mouseY) {
        if (!isMouseDown) return;

        int pixelSize = 16;
        int gapSize = 2;

        for (int py = 0; py < currentGrid.getHeight(); py++) {
            for (int px = 0; px < currentGrid.getWidth(); px++) {
                int cellX = x + 6 + px * (pixelSize + gapSize);
                int cellY = (int) ((y + 21 - scroller.getValue()) + py * (pixelSize + gapSize));
                if (mouseX >= cellX && mouseX < cellX + pixelSize && mouseY >= cellY && mouseY < cellY + pixelSize) {
                    Point point = new Point(px, py);
                    if (modifiedCells.contains(point)) return;

                    boolean previous = currentGrid.getPixel(px, py);
                    currentGrid.setPixel(px, py, drawState);
                    undoStack.push(new UndoAction(point, previous));
                    modifiedCells.add(point);
                    return;
                }
            }
        }
    }

    private boolean renderGridOutline(DrawContext context, PixelGrid grid, int x1, int y1, int pixelSize, int gapSize, int outlineColor, boolean markCenter, double mouseX, double mouseY) {
        boolean rtrn = true;
        if (markCenter) {
            int centerX = grid.getWidth() / 2;
            int centerY = grid.getHeight() / 2;

            int px = x1 + centerX * (pixelSize + gapSize);
            int py = y1 + centerY * (pixelSize + gapSize);

            int centerColor = new Color(255, 100, 100, 100).getRGB();

            context.fill(px + 1, py + 1, px + pixelSize - 1, py + pixelSize - 1, centerColor);
        }

        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                int px = x1 + x * (pixelSize + gapSize);
                int py = y1 + y * (pixelSize + gapSize);
                if (!context.scissorContains(px, py))
                {
                    rtrn = false;
                }
                boolean on = grid.getPixel(x, y);
                int fillColor = on ? Color.WHITE.getRGB() : new Color(0, 0, 0, 0).getRGB();

                context.fill(px + 1, py + 1, px + pixelSize - 1, py + pixelSize - 1, fillColor);

                boolean hovered = mouseX >= px && mouseX < px + pixelSize && mouseY >= py && mouseY < py + pixelSize;
                int actualOutlineColor = hovered ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : outlineColor;

                context.fill(px + 1, py, px + pixelSize - 1, py + 1, actualOutlineColor); // top
                context.fill(px + 1, py + pixelSize - 1, px + pixelSize - 1, py + pixelSize, actualOutlineColor); // bottom
                context.fill(px, py, px + 1, py + pixelSize, actualOutlineColor); // left
                context.fill(px + pixelSize - 1, py, px + pixelSize, py + pixelSize, actualOutlineColor); // right
            }
        }
        return rtrn;
    }
}

