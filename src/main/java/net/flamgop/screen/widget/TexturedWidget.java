package net.flamgop.screen.widget;

import net.flamgop.gpu.texture.GPUTexture;
import net.flamgop.gpu.Window;
import net.flamgop.input.InputState;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;

public class TexturedWidget implements Widget {

    private final GPUTexture texture;
    private final Window window;
    private final Vector2f position;
    private final Vector2i size;
    private final Runnable onClick;

    private boolean hovered;

    public TexturedWidget(GPUTexture texture, Window window, Vector2f position, Vector2i size, Runnable onClick) {
        this.texture = texture;
        this.window = window;
        this.position = position;
        this.size = size;
        this.onClick = onClick;
    }

    @Override
    public Vector2i position() {
        return new Vector2i((int) (window.width() * position.x()), (int) (window.height() * position.y()));
    }

    @Override
    public Vector2i size() {
        return size;
    }

    @Override
    public void hovered(boolean hovered) {
        this.hovered = hovered;
    }

    @Override
    public void click() {
        this.onClick.run();
    }

    @Override
    public void update(InputState inputState, double delta) {

    }

    @Override
    public void draw(Vector2i position) {
        this.texture.blit(position.x, position.y, (int) size.x, (int) size.y, hovered ? new Vector3f(1.5f) : new Vector3f(1f));
    }
}
