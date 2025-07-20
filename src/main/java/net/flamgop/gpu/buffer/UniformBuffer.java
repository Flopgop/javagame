package net.flamgop.gpu.buffer;

import static org.lwjgl.opengl.GL46.*;

public class UniformBuffer extends SerializedBuffer {

    public UniformBuffer(GPUBuffer.UpdateHint hint) {
        super(hint);
    }

    public void bind(int index) {
        glBindBufferBase(GL_UNIFORM_BUFFER, index, buffer().handle());
    }
}
