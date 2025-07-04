package walksy.lib.core.gui.widgets;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import org.joml.Vector2d;
import walksy.lib.core.config.impl.Option;
import walksy.lib.core.config.impl.options.groups.OptionGroup;
import walksy.lib.core.gui.impl.WalksyLibConfigScreen;

import java.util.ArrayList;
import java.util.List;

public class OptionGroupWidget extends OptionWidget {

    private final OptionGroup group;
    private final List<OptionWidget> children = new ArrayList<>();
    private final Vector2d vec2d;

    private final WalksyLibConfigScreen parent;

    public OptionGroupWidget(int x, int y, int width, int height, OptionGroup group, WalksyLibConfigScreen parent) {
        super(x, y, width, height, group.getName());
        this.parent = parent;
        this.group = group;
        this.vec2d = new Vector2d(x, y);

        int yOff = 15;
        for (Option<?> option : group.getOptions())
        {
            children.add(option.createWidget(x + 15, yOff, width, height));
            yOff+= 15;
        }
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta)
    {
        MinecraftClient client = MinecraftClient.getInstance();
        context.drawCenteredTextWithShadow(client.textRenderer, group.getName(), this.parent.width / 2, (int) this.vec2d.y(), -1);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

    public List<OptionWidget> getChildren()
    {
        return children;
    }
}
