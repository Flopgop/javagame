package net.flamgop.asset;

import net.flamgop.util.ResourceHelper;
import org.lwjgl.system.MemoryUtil;

import java.io.*;
import java.nio.ByteBuffer;

public class AssetLoader {

    private final String assetPath;

    public AssetLoader(String assetPath) {
        if (!assetPath.endsWith(File.separator)) {
            assetPath += File.separator;
        }
        this.assetPath = assetPath;
    }

    public ByteBuffer load(String identifier) throws FileNotFoundException {
        String standard = identifier.toLowerCase();
        int colonIndex = standard.indexOf(":");
        if (colonIndex == -1) throw new IllegalStateException("Asset identifier \"" + identifier + "\" does not follow valid format of \"type:path\".");
        String type = standard.substring(0, colonIndex);
        String filePath = identifier.substring(colonIndex + 1);
        return switch (type) {
            case "resource" -> loadFromResource(filePath);
            case "file" -> loadFromAsset(filePath);
            default -> throw new IllegalStateException("Unexpected resource type: " + type);
        };
    }

    private ByteBuffer loadFromResource(String path) {
        return ResourceHelper.loadFileFromResource(path);
    }

    private ByteBuffer loadFromAsset(String path) throws FileNotFoundException {
        File file = new File(assetPath + path);
        if (!file.exists()) {
            throw new FileNotFoundException();
        }
        try (InputStream is = new FileInputStream(file)) {
            byte[] bytes = is.readAllBytes();
            ByteBuffer buf = MemoryUtil.memAlloc(bytes.length);
            buf.put(0, bytes);
            return buf;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
