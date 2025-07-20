package net.flamgop.gpu.buffer;

import java.nio.ByteBuffer;

public interface BufferSerializable {
    void encode(ByteBuffer buf);
    int length();
}
