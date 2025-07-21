package net.flamgop.util;

import org.lwjgl.system.MemoryUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ResourceHelper {
    public static String loadFileContentsFromResource(String resource) {
        try (InputStream is = ResourceHelper.class.getClassLoader().getResourceAsStream(resource)) {
            if (is == null) throw new FileNotFoundException(resource);
            return new String(is.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ByteBuffer loadFileFromResource(String resource) {
        try (InputStream is = ResourceHelper.class.getClassLoader().getResourceAsStream(resource)) {
            if (is == null) throw new FileNotFoundException(resource);
            byte[] bytes = is.readAllBytes();
            ByteBuffer buf = MemoryUtil.memAlloc(bytes.length);
            buf.put(0, bytes);
            return buf;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
