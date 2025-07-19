package net.flamgop.uniform;

import net.flamgop.gpu.UniformSerializable;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;

public class PBRUniformData implements UniformSerializable {
    public Vector4f ambient = new Vector4f();
    public Vector3f lightDirection = new Vector3f();
    // 1 byte padding
    public Vector4f lightColor = new Vector4f();
    // 48 bytes total.

    @Override
    public void encode(ByteBuffer buffer) {
        ambient.get(0, buffer);
        lightDirection.get(4 * Float.BYTES, buffer);
        buffer.putFloat(2 * (4 * Float.BYTES) - Float.BYTES, 0); // pad
        lightColor.get(2 * (4 * Float.BYTES), buffer);
    }

    @Override
    public int length() {
        return 3 * (4 * Float.BYTES);
    }
}
