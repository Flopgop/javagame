package net.flamgop.gpu.uniform;

import net.flamgop.gpu.buffer.BufferSerializable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

public class CameraUniformData implements BufferSerializable {
    public Matrix4f view = new Matrix4f();
    public Matrix4f projection = new Matrix4f();
    public Vector3f position = new Vector3f();
    // 1 byte padding

    @Override
    public void encode(ByteBuffer buf) {
        view.get(0, buf);
        projection.get(16 * Float.BYTES, buf);

        position.get(2 * 16 * Float.BYTES, buf);
        buf.putFloat(2 * 16 * Float.BYTES + 3 * Float.BYTES, 0f);
    }

    @Override
    public int length() {
        return 16 * Float.BYTES + 16 * Float.BYTES + 4 * Float.BYTES;
    }
}
