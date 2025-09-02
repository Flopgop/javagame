package net.flamgop.gpu.buffer;

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

    public enum Target {
        ARRAY(GL_ARRAY_BUFFER),
        ATOMIC_COUNTER(GL_ATOMIC_COUNTER_BUFFER),
        COPY_READ(GL_COPY_READ_BUFFER),
        COPY_WRITE(GL_COPY_WRITE_BUFFER),
        DISPATCH_INDIRECT(GL_DISPATCH_INDIRECT_BUFFER),
        DRAW_INDIRECT(GL_DRAW_INDIRECT_BUFFER),
        ELEMENT_ARRAY(GL_ELEMENT_ARRAY_BUFFER),
        PIXEL_PACK(GL_PIXEL_PACK_BUFFER),
        PIXEL_UNPACK(GL_PIXEL_UNPACK_BUFFER),
        QUERY(GL_QUERY_BUFFER),
        SHADER_STORAGE(GL_SHADER_STORAGE_BUFFER),
        TEXTURE(GL_TEXTURE_BUFFER),
        TRANSFORM_FEEDBACK(GL_TRANSFORM_FEEDBACK_BUFFER),
        UNIFORM(GL_UNIFORM_BUFFER),

        ;
        final int glQualifier;
        Target(int glQualifier) {
            this.glQualifier = glQualifier;
        }
    }

    private final int handle;
    private final BufferUsage usage;

    public GPUBuffer(BufferUsage usage) {
        this.handle = glCreateBuffers();
        this.usage = usage;
    }

    public void allocate(long size) {
        glNamedBufferData(this.handle, size, this.usage.glQualifier);
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

    public void bind(Target target, int index) {
        glBindBufferBase(target.glQualifier, index, this.handle);
    }

    public void label(String label) {
        glObjectLabel(GL_BUFFER, this.handle, label);
    }

    public int handle() {
        return this.handle;
    }

    public int usage() {
        return this.usage.glQualifier;
    }

    /**
     * @implNote If this GPUBuffer is a child of a SerializedBuffer this will leak the intermediary buffer used for copying the data to the GPU. <br/> This is usually very small amounts, but if you're bypassing SerializedBuffer#destroy() often, this could add up.
     */
    public void destroy() {
        glDeleteBuffers(this.handle);
    }
}
