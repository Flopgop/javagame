package net.flamgop.gpu.state;

import org.lwjgl.glfw.GLFW;

import static org.lwjgl.opengl.GL46.*;

// opengl you are a huge fucking state machine
// this is implemented on the basis of "as needed"
public class StateManager {
    protected StateManager() {}

    public static void enable(Capability capability) {
        glEnable(capability.glQualifier);
    }

    public static boolean isEnabled(Capability capability) {
        return glIsEnabled(capability.glQualifier);
    }

    public static void disable(Capability capability) {
        glDisable(capability.glQualifier);
    }

    public static void cullFace(CullFace mode) {
        glCullFace(mode.glQualifier);
    }

    public static void frontFace(FrontFace mode) {
        glFrontFace(mode.glQualifier);
    }

    public static void viewport(int x, int y, int width, int height) {
        glViewport(x, y, width, height);
    }

    public static void clearColor(float r, float g, float b, float a) {
        glClearColor(r, g, b, a);
    }

    public static void blendFunc(BlendParameter srcFactor, BlendParameter dstFactor) {
        glBlendFunc(srcFactor.glQualifier, dstFactor.glQualifier);
    }

    public static void pixelStorei(PixelStore name, int i) {
        glPixelStorei(name.glQualifier, i);
    }

    // giant fucking state machine
    // todo: write a monolith enum for this
    public static int getStateInteger(int glQualifier) {
        if (GLFW.glfwGetCurrentContext() == 0) return -1; // no current context
        return glGetInteger(glQualifier);
    }

    public static class DebugGroupPopper implements AutoCloseable {
        static final DebugGroupPopper INSTANCE = new DebugGroupPopper();
        @Override
        public void close() {
            glPopDebugGroup();
        }
    }

    public static DebugGroupPopper pushDebugGroup(DebugSource source, int id, String message) {
        glPushDebugGroup(source.glQualifier, id, message);
        return DebugGroupPopper.INSTANCE;
    }

    // this has a check instead of a class because it would just be a waste of memory.
    public static void clear(int clear) {
        if ((clear & ~(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT)) != 0) throw new IllegalArgumentException("Invalid value for StateManager#clear");
        glClear(clear);
    }
}
