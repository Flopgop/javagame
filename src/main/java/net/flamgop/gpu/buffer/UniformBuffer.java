package net.flamgop.gpu.buffer;

public class UniformBuffer extends SerializedBuffer {

    public UniformBuffer(GPUBuffer.UpdateHint hint) {
        super(hint);
    }

    @Override
    public void bind(GPUBuffer.Target target, int index) {
        if (target != GPUBuffer.Target.UNIFORM) throw new IllegalArgumentException("Cannot bind a uniform buffer to a target other than UNIFORM!");
        super.bind(target, index);
    }

    public void bind(int index) {
        super.bind(GPUBuffer.Target.UNIFORM, index);
    }
}
