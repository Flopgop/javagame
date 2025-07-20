package net.flamgop.gpu.buffer;

import static org.lwjgl.opengl.GL46.*;

public class ShaderStorageBuffer extends SerializedBuffer {
    public ShaderStorageBuffer(GPUBuffer.UpdateHint hint) {
        super(hint);
    }

    public void bind(int index) {
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, index, buffer().handle());
    }
}
