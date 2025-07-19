package net.flamgop.gpu;

import java.nio.ByteBuffer;

public interface UniformSerializable {
    void encode(ByteBuffer buf);
    int length();
}
