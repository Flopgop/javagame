package net.flamgop.gpu.uniform;

import net.flamgop.gpu.buffer.BufferSerializable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;

public class ModelUniformData implements BufferSerializable {
    public Matrix4f model = new Matrix4f();
    public Matrix4f normal = new Matrix4f();

    public void computeNormal() {
        this.normal = new Matrix4f(this.model).invert().transpose();
    }

    @Override
    public void encode(ByteBuffer buf) {
        model.get(0, buf);
        normal.get(16 * Float.BYTES, buf);
    }

    @Override
    public int length() {
        return 16 * Float.BYTES + 16 * Float.BYTES;
    }
}
