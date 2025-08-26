package net.flamgop.screen;

import net.flamgop.Game;
import net.flamgop.asset.AssetKey;
import net.flamgop.asset.AssetLoader;
import net.flamgop.asset.AssetType;
import net.flamgop.gpu.GPUTexture;
import net.flamgop.gpu.Window;
import net.flamgop.screen.widget.TexturedWidget;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.io.FileNotFoundException;

public class PauseScreen extends Screen {

    private static final AssetKey RESUME_BUTTON_TEXTURE = new AssetKey(AssetType.RESOURCE, "cocount.jpg");

    public PauseScreen(Window window, AssetLoader assetLoader) throws FileNotFoundException {
        super(window);

        this.addWidget(new TexturedWidget(GPUTexture.loadFromBytes(assetLoader.load(RESUME_BUTTON_TEXTURE)), window, new Vector2f(0.5f, 0.5f), new Vector2i(128, 32), () -> {
            Game.INSTANCE.unpause();
        }));
    }

    @Override
    public void render(double delta) {
        super.render(delta);
    }
}
