package net.flamgop.screen.widget;

import net.flamgop.input.InputState;
import org.joml.Vector2f;

public interface Widget {
    Vector2f position();
    Vector2f size();
    boolean hovered();
    void hovered(boolean hovered);
    void click();
    void update(InputState inputState, double delta);
    void draw(Vector2f position);
}
