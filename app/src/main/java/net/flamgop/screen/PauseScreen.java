package net.flamgop.screen;

import net.flamgop.Game;
import net.flamgop.asset.*;
import net.flamgop.gpu.texture.GPUTexture;
import net.flamgop.gpu.Window;
import net.flamgop.screen.widget.TexturedWidget;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.io.FileNotFoundException;

public class PauseScreen extends Screen {

    private static final AssetIdentifier RESUME_BUTTON_TEXTURE = new AssetIdentifier("cocount.jpg");

    public PauseScreen(Window window, AssetManager assetManager) throws FileNotFoundException {
        super(window);

        this.addWidget(new TexturedWidget(assetManager.loadSync(RESUME_BUTTON_TEXTURE, GPUTexture.class).get(), window, new Vector2f(0.5f, 0.5f), new Vector2i(128, 32), () -> {
            Game.INSTANCE.unpause();
        }));
    }

    @Override
    public void render(double delta) {
        super.render(delta);
    }
}
