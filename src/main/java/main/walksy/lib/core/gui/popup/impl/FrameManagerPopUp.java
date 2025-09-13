package main.walksy.lib.core.gui.popup.impl;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.type.PixelGrid;
import main.walksy.lib.core.config.local.options.type.PixelGridAnimation;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.popup.PopUp;
import main.walksy.lib.core.gui.widgets.ButtonWidget;
import main.walksy.lib.core.utils.MainColors;
import main.walksy.lib.core.utils.Scroller;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class FrameManagerPopUp extends PopUp {
    private Option<PixelGridAnimation> option;
    private final CopyOnWriteArrayList<ButtonWidget> buttons = new CopyOnWriteArrayList<>();
    private final Map<Integer, Point> framePreviewPositions = new HashMap<>();
    private final List<List<PixelGrid>> undoStack = new ArrayList<>();
    private final ButtonWidget undoButton;
    private final ButtonWidget undoAllButton;
    private final ButtonWidget doneButton;
    private final Scroller scroller;
    private final Consumer<PixelGrid> onGridRemoval;

    public FrameManagerPopUp(WalksyLibConfigScreen screen, Option<PixelGridAnimation> option, Runnable onDone, Consumer<PixelGrid> onGridRemoval) {
        super(screen, "Edit Frames", 200, 250);
        this.option = option;
        this.onGridRemoval = onGridRemoval;
        this.scroller = new Scroller(0, 2);
        this.undoButton = new ButtonWidget(x + 5, y + height - 21, 40, 16, false, "Undo", this::undo);
        this.undoAllButton = new ButtonWidget(x + 50, y + height - 21, 60, 16, false, "Undo All", this::undoAll);
        this.doneButton = new ButtonWidget(x + width - 51, y + height - 21, 40, 16, false, "Done", () -> {
            onDone.run();
            screen.popUp = null;
        });
        rebuildButtons();
    }

    private void rebuildButtons() {
        buttons.clear();
        framePreviewPositions.clear();

        List<PixelGrid> frames = option.getValue().getFrames();
        int centerX = x + (width / 2) - 50;
        int startY = y + 32;
        int spacing = 50;

        for (int i = 0; i < frames.size(); i++) {
            int frameY = startY + i * spacing;

            int finalI = i;
            ButtonWidget addBefore = new ButtonWidget(centerX + 30, frameY - 25, 40, 20, false, "+", () -> addFrameAt(finalI));
            buttons.add(addBefore);

            PixelGrid frame = frames.get(i);
            ButtonWidget removeBtn = new ButtonWidget(centerX - 25, frameY, 20, 20, false, "-", () -> removeFrame(frame));
            removeBtn.setOutlineColor(new Color(255, 0, 0, 130).getRGB(), new Color(255, 0, 0, 160).getRGB());
            buttons.add(removeBtn);

            ButtonWidget frameBtn = new ButtonWidget(centerX, frameY, 100, 20, false, "Frame " + (i + 1), null);
            frameBtn.overrideHover = true;
            buttons.add(frameBtn);

            framePreviewPositions.put(i, new Point(centerX + 110, frameY + 2));
        }

        int yAfterLast = startY + frames.size() * spacing;
        ButtonWidget addLast = new ButtonWidget(centerX + 30, yAfterLast - 25, 40, 20, false, "+", () -> addFrameAt(frames.size()));
        buttons.add(addLast);
    }

    private List<PixelGrid> deepCopyFrames(List<PixelGrid> original) {
        List<PixelGrid> copy = new ArrayList<>();
        for (PixelGrid frame : original) {
            copy.add(frame.copy());
        }
        return copy;
    }

    private void addFrameAt(int index) {
        PixelGridAnimation animation = option.getValue();
        undoStack.add(deepCopyFrames(animation.getFrames()));
        PixelGrid newFrame = PixelGrid.create(15, 15).build();
        animation.getFrames().add(index, newFrame);
        updateFrameNumbers();
        rebuildButtons();
    }

    private void removeFrame(PixelGrid frame) {
        if (option.getValue().getFrames().size() == 1) return;
        PixelGridAnimation animation = option.getValue();
        undoStack.add(deepCopyFrames(animation.getFrames()));
        animation.getFrames().remove(frame);
        if (this.onGridRemoval != null) {
            this.onGridRemoval.accept(frame);
        }
        updateFrameNumbers();
        rebuildButtons();
        updateScrollerBounds();
    }

    private void updateFrameNumbers() {
        // Optional: update metadata if needed (like per-frame labels, durations, etc.)
    }

    private void undo() {
        PixelGridAnimation animation = option.getValue();
        if (!undoStack.isEmpty()) {
            List<PixelGrid> lastState = undoStack.remove(undoStack.size() - 1);
            animation.getFrames().clear();
            animation.getFrames().addAll(lastState);
            updateFrameNumbers();
            rebuildButtons();
        }
    }

    private void undoAll() {
        PixelGridAnimation animation = option.getValue();
        if (!undoStack.isEmpty()) {
            List<PixelGrid> firstState = undoStack.get(0);
            undoStack.clear();
            animation.getFrames().clear();
            animation.getFrames().addAll(firstState);
            updateFrameNumbers();
            rebuildButtons();
        }
    }

    @Override
    public void render(DrawContext context, double mouseX, double mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.enableScissor(x, y + 2, x + width, y + height - 25);
        for (ButtonWidget btn : buttons) {
            btn.scrollY = (float) scroller.getValue();
            if (btn.getMessage().getString().equals("-"))
            {
                btn.setEnabled(option.getValue().getFrames().size() != 1);
            }
            btn.render(context, (int) mouseX, (int) mouseY, delta);
        }

        List<PixelGrid> frames = option.getValue().getFrames();
        for (Map.Entry<Integer, Point> entry : framePreviewPositions.entrySet()) {
            int index = entry.getKey();
            if (index >= 0 && index < frames.size()) {
                PixelGrid grid = frames.get(index);
                Point pos = entry.getValue();
                WalksyLib.getInstance().get2DRenderer().renderGridTexture(
                        context, grid,
                        pos.x - 1,
                        (int) (pos.y - 6 - scroller.getValue()),
                        2, 0, -1
                );
            }
        }
        context.disableScissor();
        context.drawHorizontalLine(x + 2, x + width - 3, y + height - 25, MainColors.OUTLINE_WHITE.getRGB());
        doneButton.render(context, (int) mouseX, (int) mouseY, delta);
        undoButton.render(context, (int) mouseX, (int) mouseY, delta);
        undoAllButton.render(context, (int) mouseX, (int) mouseY, delta);
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        for (ButtonWidget btn : buttons) {
            btn.onClick(mouseX, mouseY);
        }
        undoButton.onClick(mouseX, mouseY);
        undoAllButton.onClick(mouseX, mouseY);
        doneButton.onClick(mouseX, mouseY);
    }

    @Override
    public void onScroll(double mouseX, double mouseY, double verticalAmount) {
        super.onScroll(mouseX, mouseY, verticalAmount);
        scroller.onScroll(verticalAmount);
        updateScrollerBounds();
    }

    @Override
    public void layout(int x1, int y1) {
        super.layout(x1, y1);
        if (this.buttons != null && loaded) {
            this.rebuildButtons();
            this.undoButton.setPosition(x + 5, y + height - 21);
            this.undoAllButton.setPosition(x + 50, y + height - 21);
            this.doneButton.setPosition(x + width - 51, y + height - 21);
        }
    }

    private void updateScrollerBounds() {
        if (buttons.isEmpty()) {
            scroller.setBounds(0, 0);
            return;
        }
        scroller.setBounds(0, Math.max(0, y + 32 + (buttons.size() / 3) * 50 - (y + height - 25)));
    }
}
