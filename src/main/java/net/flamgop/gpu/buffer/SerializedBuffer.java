package net.flamgop.gpu.buffer;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class SerializedBuffer {
    private final GPUBuffer buffer;

    public SerializedBuffer(GPUBuffer.UpdateHint hint) {
        this.buffer = new GPUBuffer(hint == GPUBuffer.UpdateHint.STATIC ? GPUBuffer.BufferUsage.STATIC_DRAW : hint == GPUBuffer.UpdateHint.DYNAMIC ? GPUBuffer.BufferUsage.DYNAMIC_DRAW : GPUBuffer.BufferUsage.STREAM_DRAW);
    }

    public SerializedBuffer(GPUBuffer.BufferUsage usage) {
        this.buffer = new GPUBuffer(usage);
    }

    public void allocate(BufferSerializable bufferData) {
        ByteBuffer buf = MemoryUtil.memAlloc(bufferData.length());
        bufferData.encode(buf);
        this.buffer.allocate(buf);
        MemoryUtil.memFree(buf);
    }

    public void store(BufferSerializable bufferData) {
        ByteBuffer buf = MemoryUtil.memAlloc(bufferData.length());
        bufferData.encode(buf);
        this.buffer.store(buf, 0);
        MemoryUtil.memFree(buf);
    }

    public GPUBuffer buffer() {
        return this.buffer;
    }
}
