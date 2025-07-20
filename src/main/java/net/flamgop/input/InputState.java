package net.flamgop.input;

import org.lwjgl.glfw.GLFW;

public class InputState {
    private boolean[] keys = new boolean[GLFW.GLFW_KEY_LAST];

    public void handleKey(int key, int action) {
        keys[key] = action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT;
    }

    public boolean isKeyDown(int key) {
        return keys[key];
    }
}
