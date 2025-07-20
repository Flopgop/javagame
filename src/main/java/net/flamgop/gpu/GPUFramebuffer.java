package net.flamgop.gpu;

import static org.lwjgl.opengl.GL46.*;

public class GPUFramebuffer {

    public enum RenderBufferType {
        COLOR(false),
        DEPTH(true),
        STENCIL(true),
        DEPTH_STENCIL(true),

        ;

        final boolean special;
        RenderBufferType(boolean special) {
            this.special = special;
        }
    }

    private final int handle;
    private final FramebufferInitializer initCallback;

    private final int[] colorRenderBuffers;
    private final GPUTexture[] textures;

    // special cases.
    private int depthAttachment = 0;
    private int stencilAttachment = 0;
    private int depthStencilAttachment = 0;

    public GPUFramebuffer(int width, int height, FramebufferInitializer initCallback) {
        this.handle = glCreateFramebuffers();
        this.colorRenderBuffers = new int[glGetInteger(GL_MAX_COLOR_ATTACHMENTS)];
        this.textures = new GPUTexture[glGetInteger(GL_MAX_COLOR_ATTACHMENTS)];
        this.initCallback = initCallback;
        this.init(width, height);
    }

    private void init(int width, int height) {
        initCallback.init(this, width, height);
        checkCompleteness();
    }

    public void drawBuffers(int[] attachments) {
        glNamedFramebufferDrawBuffers(this.handle, attachments);
    }

    /**
     * Attach a texture to this framebuffer
     *
     * @param texture texture to attach to this attachment slot
     * @param attachment GL_COLOR_ATTACHMENT0 + attachment slot
     * @param mipmapLevel mipmap level of this texture to attach to this slot
     */
    public void texture(GPUTexture texture, int attachment, int mipmapLevel) {
        int index = attachment - GL_COLOR_ATTACHMENT0;
        if (this.textures[index] != null) {
            this.textures[index].destroy();
        }
        this.textures[index] = texture;

        glNamedFramebufferTexture(this.handle, attachment, texture.handle(), mipmapLevel);
    }

    /**
     * Attach a renderbuffer to this framebuffer
     *
     * @param internalFormat GL_DEPTH24_STENCIL8, or any valid renderbuffer format.
     * @param attachment GL_COLOR_ATTACHMENT0 + attachment slot
     * @param width width of the renderbuffer
     * @param height height of the renderbuffer
     */
    public void renderbuffer(int internalFormat, int attachment, int width, int height) {
        RenderBufferType type = renderBufferType(attachment);
        int renderbuffer;
        if (!type.special) {
            int index = attachment - GL_COLOR_ATTACHMENT0;
            if (this.colorRenderBuffers[index] != 0) {
                glDeleteRenderbuffers(this.colorRenderBuffers[index]);
            }
            this.colorRenderBuffers[index] = glCreateRenderbuffers();
            renderbuffer = this.colorRenderBuffers[index];
        } else {
            switch (type) {
                case DEPTH -> {
                    if (depthAttachment != 0) glDeleteRenderbuffers(depthAttachment);
                    depthAttachment = glCreateRenderbuffers();
                    renderbuffer = depthAttachment;
                }
                case STENCIL -> {
                    if (stencilAttachment != 0) glDeleteRenderbuffers(stencilAttachment);
                    stencilAttachment = glCreateRenderbuffers();
                    renderbuffer = stencilAttachment;
                }
                case DEPTH_STENCIL -> {
                    if (depthStencilAttachment != 0) glDeleteRenderbuffers(depthStencilAttachment);
                    depthStencilAttachment = glCreateRenderbuffers();
                    renderbuffer = depthStencilAttachment;
                }
                default -> throw new IllegalStateException("Unexpected value: " + type);
            }
        }
        glNamedRenderbufferStorage(renderbuffer, internalFormat, width, height);
        glNamedFramebufferRenderbuffer(this.handle, attachment, GL_RENDERBUFFER, renderbuffer);
    }

    private RenderBufferType renderBufferType(int attachment) {
        if (attachment >= GL_COLOR_ATTACHMENT0 && attachment <= GL_COLOR_ATTACHMENT31) {
            return RenderBufferType.COLOR;
        }
        else if (attachment == GL_DEPTH_ATTACHMENT) return RenderBufferType.DEPTH;
        else if (attachment == GL_STENCIL_ATTACHMENT) return RenderBufferType.STENCIL;
        else if (attachment == GL_DEPTH_STENCIL_ATTACHMENT) return RenderBufferType.DEPTH_STENCIL;
        else throw new IllegalStateException("Unsupported render buffer attachment: " + attachment);
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

    public void clear(float red, float green, float blue, float alpha) {
        this.bind();
        glClearColor(red, green, blue, alpha);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
    }

    public void cleanupAttachments() {
        for (int i = 0; i < colorRenderBuffers.length; i++) {
            int buffer = colorRenderBuffers[i];
            if (buffer != 0) glDeleteRenderbuffers(buffer);
            colorRenderBuffers[i] = 0;
        }
        if (depthAttachment != 0) glDeleteRenderbuffers(depthAttachment);
        if (stencilAttachment != 0) glDeleteRenderbuffers(stencilAttachment);
        if (depthStencilAttachment != 0) glDeleteRenderbuffers(depthStencilAttachment);
        for (int i = 0; i < textures.length; i++) {
            GPUTexture texture = textures[i];
            if (texture != null) texture.destroy();
            textures[i] = null;
        }
    }

    public void destroy() {
        glDeleteFramebuffers(handle);
        cleanupAttachments();
    }
}
