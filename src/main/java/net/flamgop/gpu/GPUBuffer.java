package net.flamgop.gpu;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL46.*;

public class GPUBuffer {

    public enum UpdateHint {
        STATIC,
        DYNAMIC,
        STREAM,

        ;
    }

    public enum BufferUsage {
        STATIC_DRAW(GL_STATIC_DRAW),
        DYNAMIC_DRAW(GL_DYNAMIC_DRAW),
        STREAM_DRAW(GL_STREAM_DRAW),

        STATIC_READ(GL_STATIC_READ),
        DYNAMIC_READ(GL_DYNAMIC_READ),
        STREAM_READ(GL_STREAM_READ),

        STATIC_COPY(GL_STATIC_COPY),
        DYNAMIC_COPY(GL_DYNAMIC_COPY),
        STREAM_COPY(GL_STREAM_COPY),

        ;

        final int glQualifier;
        BufferUsage(int glQualifier) {
            this.glQualifier = glQualifier;
        }
    }

    private final int handle;
    private final BufferUsage usage;

    public GPUBuffer(BufferUsage usage) {
        this.handle = glCreateBuffers();
        this.usage = usage;
    }

    public void allocate(ByteBuffer buffer) {
        glNamedBufferData(this.handle, buffer, this.usage.glQualifier);
    }

    public void allocate(IntBuffer buffer) {
        glNamedBufferData(this.handle, buffer, this.usage.glQualifier);
    }

    public void allocate(FloatBuffer buffer) {
        glNamedBufferData(this.handle, buffer, this.usage.glQualifier);
    }

    public void store(ByteBuffer buffer, int offset) {
        glNamedBufferSubData(this.handle, offset, buffer);
    }

    public void store(IntBuffer buffer, int offset) {
        glNamedBufferSubData(this.handle, offset, buffer);
    }

    public void store(FloatBuffer buffer, int offset) {
        glNamedBufferSubData(this.handle, offset, buffer);
    }

    public int handle() {
        return this.handle;
    }

    public int usage() {
        return this.usage.glQualifier;
    }

    public void destroy() {
        glDeleteBuffers(this.handle);
    }
}
