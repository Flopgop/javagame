package net.flamgop.gpu.uniform;

import net.flamgop.gpu.buffer.BufferSerializable;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;

public class PBRUniformData implements BufferSerializable {
    public Vector4f ambient = new Vector4f();
    public Vector3f lightDirection = new Vector3f();
    public int lightCount = 0;
    public Vector4f lightColor = new Vector4f();
    // 48 bytes total.

    @Override
    public void encode(ByteBuffer buffer) {
        ambient.get(0, buffer);
        lightDirection.get(4 * Float.BYTES, buffer);
        buffer.putInt(2 * (4 * Float.BYTES) - Float.BYTES, lightCount);
        lightColor.get(2 * (4 * Float.BYTES), buffer);
    }

    @Override
    public int length() {
        return 3 * (4 * Float.BYTES);
    }
}
