package net.flamgop.gpu.framebuffer;

import net.flamgop.gpu.state.FramebufferBit;
import net.flamgop.gpu.texture.GPUTexture;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static org.lwjgl.opengl.GL46.*;

public class GPUFramebuffer {

    public enum Target {
        READ(GL_READ_FRAMEBUFFER),
        WRITE(GL_DRAW_FRAMEBUFFER),
        ALL(GL_FRAMEBUFFER),

        /**
         * Note: NONE is not a OpenGL framebuffer target, this simply tells GPUFramebuffer#bind(Target) to unbind this framebuffer.
         */
        NONE(-1)
        ;
        final int glQualifier;
        Target(int glQualifier) {
            this.glQualifier = glQualifier;
        }
    }

    public enum Attachment {
        COLOR0(GL_COLOR_ATTACHMENT0),
        COLOR1(GL_COLOR_ATTACHMENT1),
        COLOR2(GL_COLOR_ATTACHMENT2),
        COLOR3(GL_COLOR_ATTACHMENT3),
        COLOR4(GL_COLOR_ATTACHMENT4),
        COLOR5(GL_COLOR_ATTACHMENT5),
        COLOR6(GL_COLOR_ATTACHMENT6),
        COLOR7(GL_COLOR_ATTACHMENT7),
        COLOR8(GL_COLOR_ATTACHMENT8),
        COLOR9(GL_COLOR_ATTACHMENT9),
        COLOR10(GL_COLOR_ATTACHMENT10),
        COLOR11(GL_COLOR_ATTACHMENT11),
        COLOR12(GL_COLOR_ATTACHMENT12),
        COLOR13(GL_COLOR_ATTACHMENT13),
        COLOR14(GL_COLOR_ATTACHMENT14),
        COLOR15(GL_COLOR_ATTACHMENT15),
        COLOR16(GL_COLOR_ATTACHMENT16),
        COLOR17(GL_COLOR_ATTACHMENT17),
        COLOR18(GL_COLOR_ATTACHMENT18),
        COLOR19(GL_COLOR_ATTACHMENT19),
        COLOR20(GL_COLOR_ATTACHMENT20),
        COLOR21(GL_COLOR_ATTACHMENT21),
        COLOR22(GL_COLOR_ATTACHMENT22),
        COLOR23(GL_COLOR_ATTACHMENT23),
        COLOR24(GL_COLOR_ATTACHMENT24),
        COLOR25(GL_COLOR_ATTACHMENT25),
        COLOR26(GL_COLOR_ATTACHMENT26),
        COLOR27(GL_COLOR_ATTACHMENT27),
        COLOR28(GL_COLOR_ATTACHMENT28),
        COLOR29(GL_COLOR_ATTACHMENT29),
        COLOR30(GL_COLOR_ATTACHMENT30),
        COLOR31(GL_COLOR_ATTACHMENT31),
        DEPTH(GL_DEPTH_ATTACHMENT),
        STENCIL(GL_STENCIL_ATTACHMENT),
        DEPTH_STENCIL(GL_DEPTH_STENCIL_ATTACHMENT),

        ;
        final int glQualifier;
        Attachment(int glQualifier) {
            this.glQualifier = glQualifier;
        }
    }

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
        this.bindRead();
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
        glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, FramebufferBit.DEPTH, GL_NEAREST);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void copyDepthToBuffer(GPUFramebuffer framebuffer, int width, int height) {
        this.bindRead();
        framebuffer.bindWrite();
        glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, FramebufferBit.DEPTH, GL_NEAREST);
        framebuffer.bindAll();
    }

    private void init(int width, int height) {
        initCallback.init(this, width, height);
        checkCompleteness();
    }

    public void drawBuffers(Attachment[] attachments) {
        int[] buffers = new int[attachments.length];
        for (int i = 0; i < attachments.length; i++) buffers[i] = attachments[i].glQualifier;
        glNamedFramebufferDrawBuffers(this.handle, buffers);
    }

    public int handle() {
        return handle;
    }

    public void texture(GPUTexture texture, Attachment attachment, int mipmapLevel) {
        glNamedFramebufferTexture(this.handle, attachment.glQualifier, texture.handle(), mipmapLevel);
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
        resizeNoViewport(width, height);
        glViewport(0,0,width,height);
    }

    public void resizeNoViewport(int width, int height) {
        cleanupAttachments();
        this.init(width, height);
    }

    public void bindAll() {
        bind(Target.ALL);
    }
    public void bindRead() {
        bind(Target.READ);
    }
    public void bindWrite() {
        bind(Target.WRITE);
    }

    public void bind(@NotNull Target target) {
        if (target == Target.NONE) {
            this.unbind(Target.ALL);
        } else {
            glBindFramebuffer(target.glQualifier, handle);
        }
    }

    public void unbind(@NotNull Target target) {
        glBindFramebuffer(target.glQualifier, 0);
    }

    public void clear(int bits) {
        this.bindAll();
        glClear(bits);
    }

    public void label(String label) {
        glObjectLabel(GL_FRAMEBUFFER, handle, label);
    }

    public void cleanupAttachments() {
        this.cleanupCallback.accept(this);
    }

    public void destroy() {
        cleanupAttachments();
        glDeleteFramebuffers(handle);
    }
}
