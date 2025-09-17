package net.flamgop.input;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

public class InputState {
    private final boolean[] keys = new boolean[GLFW.GLFW_KEY_LAST];
    private final boolean[] previousKeys = new boolean[GLFW.GLFW_KEY_LAST];

    private final boolean[] mouseButtons = new boolean[GLFW.GLFW_MOUSE_BUTTON_LAST];
    private final boolean[] previousMouseButtons = new boolean[GLFW.GLFW_MOUSE_BUTTON_LAST];

    private double mouseX, mouseY, lastMouseX, lastMouseY, deltaMouseX, deltaMouseY;

    public void update() {
        System.arraycopy(keys, 0, previousKeys, 0, GLFW.GLFW_KEY_LAST);
        System.arraycopy(mouseButtons, 0, previousMouseButtons, 0, GLFW.GLFW_MOUSE_BUTTON_LAST);
        deltaMouseX = mouseX - lastMouseX;
        deltaMouseY = mouseY - lastMouseY;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    public void handleKey(int key, int action) {
        keys[key] = action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT;
    }

    public void handleMouse(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    public void handleMouseButton(int button, int action) {
        mouseButtons[button] = action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT;
    }

    public boolean isKeyDown(int key) {
        return keys[key];
    }

    public boolean wasKeyPressed(int key) {
        return keys[key] && !previousKeys[key];
    }

    public boolean isMouseButtonDown(int button) {
        return mouseButtons[button];
    }

    public boolean wasMouseButtonPressed(int button) {
        return mouseButtons[button] && !previousMouseButtons[button];
    }

    public Vector2f deltaMousePosition() {
        return new Vector2f((float) deltaMouseX, (float) deltaMouseY);
    }

    public Vector2f mousePosition() {
        return new Vector2f((float) mouseX, (float) mouseY);
    }
}
