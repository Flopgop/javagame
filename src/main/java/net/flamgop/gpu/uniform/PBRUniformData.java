package net.flamgop.gpu.uniform;

import net.flamgop.gpu.buffer.BufferSerializable;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;

public class PBRUniformData implements BufferSerializable {
    public Vector4f ambient = new Vector4f();
    public Vector4f lightColor = new Vector4f();
    public Vector3f lightDirection = new Vector3f();
    public float lightCount = 0;
    // 48 bytes total.

    @Override
    public void encode(ByteBuffer buffer) {
        ambient.get(0, buffer);
        lightColor.get(4 * Float.BYTES, buffer);
        lightDirection.get(2 * (4 * Float.BYTES), buffer);
        buffer.putFloat(3 * (4 * Float.BYTES) - Float.BYTES, (int)lightCount); // stupid packing semantics
    }

    @Override
    public int length() {
        return 3 * (4 * Float.BYTES);
    }
}
