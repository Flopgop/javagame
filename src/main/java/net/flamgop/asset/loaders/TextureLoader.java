package net.flamgop.asset.loaders;

import net.flamgop.asset.AssetIdentifier;
import net.flamgop.asset.Loader;
import net.flamgop.gpu.GPUTexture;
import net.flamgop.util.ResourceHelper;
import org.lwjgl.assimp.AITexel;
import org.lwjgl.assimp.AITexture;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;

public class TextureLoader implements Loader<GPUTexture> {
    @Override
    public GPUTexture load(AssetIdentifier path) {
        return loadFromBytes(ResourceHelper.loadFileFromAssetsOrResources(path.path()));
    }

    private static GPUTexture loadFromBytes(ByteBuffer data) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GPUTexture texture = new GPUTexture(GPUTexture.Target.TEXTURE_2D);
            IntBuffer x = stack.mallocInt(1);
            IntBuffer y = stack.mallocInt(1);
            IntBuffer channelsInFile = stack.mallocInt(1);
            ByteBuffer out = STBImage.stbi_load_from_memory(data, x, y, channelsInFile, 4);
            if (out == null) throw new IllegalStateException("Bad texture data passed to loadFromBytes");
            int w = x.get(0), h = y.get(0);
            texture.storage(1, GL_RGBA8, w, h);
            texture.subimage(0, 0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE, out);
            STBImage.stbi_image_free(out);
            texture.minFilter(GPUTexture.MinFilter.NEAREST);
            texture.magFilter(GPUTexture.MagFilter.NEAREST);
            return texture;
        }
    }

    public static GPUTexture loadFromRawBytes(ByteBuffer data, int color, int format, int type, int width, int height) {
        GPUTexture texture = new GPUTexture(GPUTexture.Target.TEXTURE_2D);
        texture.storage(1, color, width, height);
        texture.subimage(0, 0, 0, width, height, format, type, data);
        texture.minFilter(GPUTexture.MinFilter.NEAREST);
        texture.magFilter(GPUTexture.MagFilter.NEAREST);
        return texture;
    }

    public static GPUTexture loadFromAssimpTexture(AITexture texture) {
        if (texture.mHeight() != 0) {
            AITexel.Buffer buffer = texture.pcData();
            ByteBuffer data = MemoryUtil.memAlloc(texture.mWidth() * texture.mHeight() * 4);
            for (AITexel texel : buffer) {
                data.put(texel.r());
                data.put(texel.g());
                data.put(texel.b());
                data.put(texel.a());
            }
            data.flip();
            GPUTexture tex = loadFromRawBytes(data, GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, texture.mWidth(), texture.mHeight());
            MemoryUtil.memFree(data);
            return tex;
        } else {
            return loadFromBytes(texture.pcDataCompressed());
        }
    }

    @Override
    public void dispose(GPUTexture asset) {
        asset.destroy();
    }
}
