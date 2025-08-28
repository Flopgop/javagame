package net.flamgop.util;

import org.lwjgl.system.MemoryUtil;

import java.io.*;
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

    public static ByteBuffer loadFileFromAssets(String path) {
        try (FileInputStream fis = new FileInputStream("./assets/" + path)) {
            byte[] bytes = fis.readAllBytes();
            ByteBuffer buf = MemoryUtil.memAlloc(bytes.length);
            buf.put(0, bytes);
            return buf;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String loadFileContentsFromAssets(String path) {
        try (FileInputStream fis = new FileInputStream("./assets/" + path)) {
            return new String(fis.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // loads from resources, unless it exists in assets
    public static ByteBuffer loadFileFromAssetsOrResources(String path) {
        File file = new File("./assets/" + path);
        if (file.exists()) return loadFileFromAssets(path);
        else return loadFileFromResource(path);
    }

    public static String loadFileContentsFromAssetsOrResources(String path) {
        File file = new File("./assets/" + path);
        if (file.exists()) return loadFileContentsFromAssets(path);
        else return loadFileContentsFromResource(path);
    }
}
