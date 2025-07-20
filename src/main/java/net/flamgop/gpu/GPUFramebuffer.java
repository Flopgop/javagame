package net.flamgop.gpu;

import static org.lwjgl.opengl.GL46.*;

public class GPUFramebuffer {

    private final int handle;
    private int renderbuffer;
    private GPUTexture texture;

    public GPUFramebuffer(int width, int height) {
        this.handle = glCreateFramebuffers();
        init(width, height);
    }

    private void init(int width, int height) {
        this.renderbuffer = glCreateRenderbuffers();
        this.texture = new GPUTexture(GPUTexture.TextureTarget.TEXTURE_2D);
        this.texture.storage(1, GL_RGBA32F, width, height);
        glTextureParameteri(this.texture.handle(), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTextureParameteri(this.texture.handle(), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTextureParameteri(this.texture.handle(), GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTextureParameteri(this.texture.handle(), GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glNamedRenderbufferStorage(this.renderbuffer, GL_DEPTH24_STENCIL8, width, height);
        glNamedFramebufferRenderbuffer(this.handle, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, renderbuffer);
        glNamedFramebufferTexture(this.handle, GL_COLOR_ATTACHMENT0, texture.handle(), 0);

        int status = glCheckNamedFramebufferStatus(this.handle, GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Framebuffer is incomplete: " + status);
        }
    }

    public void resize(int width, int height) {
        this.texture.destroy();
        glDeleteRenderbuffers(this.renderbuffer);
        init(width, height);
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

    public GPUTexture texture() {
        return texture;
    }

    public void destroy() {
        glDeleteFramebuffers(handle);
        glDeleteRenderbuffers(renderbuffer);
        texture.destroy();
    }
}
