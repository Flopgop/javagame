package net.flamgop.gpu;

import net.flamgop.input.InputSequenceHandler;
import net.flamgop.input.InputState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.function.BiConsumer;

@SuppressWarnings("resource")
public class Window {
    private int width, height;
    private int x, y;
    private String title;

    private @Nullable BiConsumer<Integer, Integer> resizeCallback;

    private final long handle;
    private final InputState inputState;
    private final InputSequenceHandler inputSequenceHandler;

    public Window(String title, int width, int height, Map<Integer, Integer> flags) {
        this.title = title;
        this.width = width;
        this.height = height;

        GLFW.glfwDefaultWindowHints();
        for (Map.Entry<Integer, Integer> entry : flags.entrySet()) {
            GLFW.glfwWindowHint(entry.getKey(), entry.getValue());
        }

        inputState = new InputState();
        inputSequenceHandler = new InputSequenceHandler();

        this.handle = GLFW.glfwCreateWindow(width, height, title, 0, 0);
        GLFW.glfwSetWindowPosCallback(handle, (_, x, y) -> this.handleMove(x, y));
        GLFW.glfwSetWindowSizeCallback(handle, (_, w, h) -> this.handleResize(w, h));
        GLFW.glfwSetKeyCallback(handle, (_, key, scancode, action, mods) -> this.handleKeyInput(key, scancode, action, mods));
        GLFW.glfwSetMouseButtonCallback(handle, (_, button, action, mods) -> this.handleMouseButtonInput(button, action, mods));
        GLFW.glfwSetCursorPosCallback(handle, (_, x, y) -> this.handleMouseMove(x,y));
    }

    public long handle() {
        return handle;
    }

    public Matrix4f ortho() {
        return new Matrix4f().identity().ortho(0, width, 0, height, 0, 1);
    }

    public void update() {
        inputState.update();
    }

    public void setResizeCallback(@Nullable BiConsumer<Integer, Integer> resizeCallback) {
        this.resizeCallback = resizeCallback;
    }

    public void setCursorMode(int mode) {
        GLFW.glfwSetInputMode(this.handle, GLFW.GLFW_CURSOR, mode);
    }

    public void makeCurrent() {
        GLFW.glfwMakeContextCurrent(handle);
    }

    public void show() {
        GLFW.glfwShowWindow(handle);
    }

    public void hide() {
        GLFW.glfwHideWindow(handle);
    }

    public void swap() {
        GLFW.glfwSwapBuffers(handle);
    }

    public void destroy() {
        GLFW.glfwDestroyWindow(handle);
    }

    public InputState inputState() {
        return inputState;
    }

    public InputSequenceHandler inputSequenceHandler() {
        return inputSequenceHandler;
    }

    public void title(String title) {
        this.title = title;
        GLFW.glfwSetWindowTitle(handle, title);
    }

    public String title() {
        return title;
    }

    public void position(int x, int y) {
        this.x = x;
        this.y = y;
        GLFW.glfwSetWindowPos(handle, x, y);
    }

    public Vector2i position() {
        return new Vector2i(x, y);
    }

    public void size(int width, int height) {
        this.width = width;
        this.height = height;
        GLFW.glfwSetWindowSize(handle, width, height);
    }

    public Vector2i size() {
        return new Vector2i(width, height);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean shouldClose() {
        return GLFW.glfwWindowShouldClose(handle);
    }

    private void handleKeyInput(int key, int scancode, int action, int mods) {
        inputSequenceHandler.handleKey(key, mods, action);
        inputState.handleKey(key, action);
    }

    private void handleMouseButtonInput(int button, int action, int mods) {
        inputState.handleMouseButton(button, action);
    }

    private void handleMouseMove(double x, double y) {
        inputState.handleMouse(x, y);
    }

    private void handleMove(int x, int y) {
        this.x = x;
        this.y = y;
    }

    private void handleResize(int width, int height) {
        this.width = width;
        this.height = height;
        if (this.resizeCallback != null) this.resizeCallback.accept(width, height);
    }
}
