package net.flamgop.uniform;

import net.flamgop.gpu.buffer.BufferSerializable;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;

public class ModelUniformData implements BufferSerializable {
    public Matrix4f model = new Matrix4f();

    @Override
    public void encode(ByteBuffer buf) {
        model.get(0, buf);
    }

    @Override
    public int length() {
        return 16 * Float.BYTES;
    }
}
