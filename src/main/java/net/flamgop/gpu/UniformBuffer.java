package net.flamgop.gpu;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL46.*;

public class UniformBuffer {

    private final GPUBuffer buffer;

    public UniformBuffer(GPUBuffer.UpdateHint hint) {
        this.buffer = new GPUBuffer(hint == GPUBuffer.UpdateHint.STATIC ? GPUBuffer.BufferUsage.STATIC_DRAW : hint == GPUBuffer.UpdateHint.DYNAMIC ? GPUBuffer.BufferUsage.DYNAMIC_DRAW : GPUBuffer.BufferUsage.STREAM_DRAW);
    }

    public void allocate(UniformSerializable uniformData) {
        ByteBuffer buf = MemoryUtil.memAlloc(uniformData.length());
        uniformData.encode(buf);
        System.out.println(buf);
        this.buffer.allocate(buf);
        MemoryUtil.memFree(buf);
    }

    public void store(UniformSerializable uniformData) {
        ByteBuffer buf = MemoryUtil.memAlloc(uniformData.length());
        uniformData.encode(buf);
        this.buffer.store(buf, 0);
        MemoryUtil.memFree(buf);
    }

    public void bind(int index) {
        glBindBufferBase(GL_UNIFORM_BUFFER, index, buffer.handle());
    }

}
