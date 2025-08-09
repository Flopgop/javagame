package net.flamgop.gpu;

import java.util.function.Consumer;

import static org.lwjgl.opengl.GL46.*;

public class GPUFramebuffer {

    private final int handle;
    private final FramebufferInitializer initCallback;
    private final Consumer<GPUFramebuffer> cleanupCallback;


    public GPUFramebuffer(int width, int height, FramebufferInitializer initCallback, Consumer<GPUFramebuffer> cleanupCallback) {
        this.handle = glCreateFramebuffers();
        this.initCallback = initCallback;
        this.cleanupCallback = cleanupCallback;
        this.init(width, height);
    }

    public void copyDepthToBackBuffer(int width, int height) {
        glBindFramebuffer(GL_READ_FRAMEBUFFER, handle);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
        glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private void init(int width, int height) {
        initCallback.init(this, width, height);
        checkCompleteness();
    }

    public void drawBuffers(int[] attachments) {
        glNamedFramebufferDrawBuffers(this.handle, attachments);
    }

    public int handle() {
        return handle;
    }

    /**
     * Attach a texture to this framebuffer
     *
     * @param texture texture to attach to this attachment slot
     * @param attachment GL_COLOR_ATTACHMENT0 + attachment slot
     * @param mipmapLevel mipmap level of this texture to attach to this slot
     */
    public void texture(GPUTexture texture, int attachment, int mipmapLevel) {
        glNamedFramebufferTexture(this.handle, attachment, texture.handle(), mipmapLevel);
    }

    public void checkCompleteness() {
        int status = glCheckNamedFramebufferStatus(this.handle, GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            String statusName = switch (status) {
                case GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> "INCOMPLETE_ATTACHMENT";
                case GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> "INCOMPLETE_MISSING_ATTACHMENT";
                case GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER -> "INCOMPLETE_DRAW_BUFFER";
                case GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER -> "INCOMPLETE_READ_BUFFER";
                case GL_FRAMEBUFFER_UNSUPPORTED -> "UNSUPPORTED";
                case GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE -> "INCOMPLETE_MULTISAMPLE";
                case GL_FRAMEBUFFER_UNDEFINED -> "UNDEFINED";
                default -> "" + status;
            };
            throw new IllegalStateException("Framebuffer is incomplete: " + statusName);
        }
    }

    public void resize(int width, int height) {
        cleanupAttachments();
        this.init(width, height);
        glViewport(0,0,width,height);
    }

    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, handle);
    }

    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void clear(int bits) {
        this.bind();
        glClear(bits);
    }

    public void label(String label) {
        glObjectLabel(GL_FRAMEBUFFER, handle, label);
    }

    public void cleanupAttachments() {
        this.cleanupCallback.accept(this);
    }

    public void destroy() {
        glDeleteFramebuffers(handle);
        cleanupAttachments();
    }
}
