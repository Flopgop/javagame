package net.flamgop.screen.widget;

import net.flamgop.input.InputState;
import org.joml.Vector2f;
import org.joml.Vector2i;

public interface Widget {
    Vector2i position();
    Vector2i size();
    void hovered(boolean hovered);
    void click();
    void update(InputState inputState, double delta);
    void draw(Vector2i position);
}
