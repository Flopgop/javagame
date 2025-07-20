package net.flamgop;

import net.flamgop.gpu.buffer.BufferSerializable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class LightArray implements BufferSerializable {

    public List<Light> lights = new ArrayList<>();

    public LightArray() {}

    @Override
    public void encode(ByteBuffer buf) {
        for (Light light : lights) {
            light.encode(buf);
        }

        buf.position(0).limit(length());
    }

    @Override
    public int length() {
        return lights.size() * Light.BYTES;
    }
}
