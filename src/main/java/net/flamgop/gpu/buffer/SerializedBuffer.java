package net.flamgop.gpu.buffer;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class SerializedBuffer {
    private final GPUBuffer buffer;

    private ByteBuffer intermediaryBuffer;

    public SerializedBuffer(GPUBuffer.UpdateHint hint) {
        this.buffer = new GPUBuffer(hint == GPUBuffer.UpdateHint.STATIC ? GPUBuffer.BufferUsage.STATIC_DRAW : hint == GPUBuffer.UpdateHint.DYNAMIC ? GPUBuffer.BufferUsage.DYNAMIC_DRAW : GPUBuffer.BufferUsage.STREAM_DRAW);
    }

    public SerializedBuffer(GPUBuffer.BufferUsage usage) {
        this.buffer = new GPUBuffer(usage);
    }

    public void allocate(BufferSerializable bufferData) {
        this.ensureIntermediaryCapacity(bufferData.length());
        bufferData.encode(intermediaryBuffer);
        this.buffer.allocate(intermediaryBuffer);
        intermediaryBuffer.clear();
    }

    public void store(BufferSerializable bufferData) {
        if (intermediaryBuffer == null) {
            throw new IllegalStateException("This buffer has not had data allocated yet!");
        }
        bufferData.encode(intermediaryBuffer);
        this.buffer.store(intermediaryBuffer, 0);
        intermediaryBuffer.clear();
    }

    public GPUBuffer buffer() {
        return this.buffer;
    }

    public void bind(GPUBuffer.Target target, int index) {
        this.buffer.bind(target, index);
    }

    private void ensureIntermediaryCapacity(int size) {
        if (intermediaryBuffer == null || intermediaryBuffer.capacity() != size) {
            if (intermediaryBuffer != null) MemoryUtil.memFree(intermediaryBuffer);
            intermediaryBuffer = MemoryUtil.memAlloc(size);
        } else {
            intermediaryBuffer.clear();
        }
    }

    public void destroy() {
        this.buffer.destroy();
        MemoryUtil.memFree(intermediaryBuffer);
    }
}
