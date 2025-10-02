package main.walksy.lib.core.gui.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.util.Pool;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BaseScreen extends Screen {

    private final Screen parent;
    private final Pool shaderPool = new Pool(3);
    public int tickCount = 0;

    protected BaseScreen(String title, Screen parent) {
        super(Text.of(title));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void close() {
        super.close();
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public void tick() {
        super.tick();
        tickCount++;
    }

    public void addWidget(ClickableWidget widget)
    {
        this.addDrawableChild(widget);
    }

    protected void renderBlurEffect() {
        PostEffectProcessor blur = client.getShaderLoader().loadPostEffect(Identifier.ofVanilla("blur"), DefaultFramebufferSet.MAIN_ONLY);
        if (blur != null) {
            blur.setUniforms("Radius", 12);
            blur.render(client.getFramebuffer(), shaderPool);
        }
        client.getFramebuffer().beginWrite(false);
    }

    protected void renderBackgroundLayer(DrawContext context, float delta) {
        if (this.client.world == null) {
            this.renderPanoramaBackground(context, delta);
        }
        this.renderDarkening(context);
    }
}
