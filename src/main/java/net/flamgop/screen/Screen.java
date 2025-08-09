package net.flamgop.screen;

import net.flamgop.gpu.Window;
import net.flamgop.screen.widget.Widget;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class Screen {

    private final Window window;
    private final List<Widget> widgets = new ArrayList<>();

    public Screen(Window window) {
        this.window = window;
    }

    public void addWidget(Widget widget) {
        widgets.add(widget);
    }

    public void update(double delta) {
        Vector2f mousePos = window.inputState().mousePosition();
        for (Widget widget : widgets) {
            widget.update(window.inputState(), delta);
            Vector2i position = widget.position();
            Vector2i size = widget.size();
            boolean widgetHovered = mousePos.x > position.x && mousePos.y < position.y && mousePos.x < position.x + size.x && mousePos.y > position.y - size.y;
            widget.hovered(widgetHovered);
            if (widgetHovered && window.inputState().wasMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_LEFT)) {
                widget.click();
            }
        }
    }

    public void drawBackground() {}

    public void render(double delta) {
        drawBackground();
        for (Widget widget : widgets) {
            widget.draw(widget.position());
        }
    }
}
