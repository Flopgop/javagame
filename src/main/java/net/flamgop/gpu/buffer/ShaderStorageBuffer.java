package net.flamgop.gpu.buffer;

public class ShaderStorageBuffer extends SerializedBuffer {
    public ShaderStorageBuffer(GPUBuffer.UpdateHint hint) {
        super(hint);
    }

    public ShaderStorageBuffer(GPUBuffer.BufferUsage usage) {
        super(usage);
    }

    @Override
    public void bind(GPUBuffer.Target target, int index) {
        if (target != GPUBuffer.Target.SHADER_STORAGE) throw new IllegalArgumentException("Cannot bind a shader storage buffer to a target other than SHADER_STORAGE!");
        super.bind(target, index);
    }

    public void bind(int index) {
        super.bind(GPUBuffer.Target.SHADER_STORAGE, index);
    }
}
