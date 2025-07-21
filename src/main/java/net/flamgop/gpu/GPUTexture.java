package net.flamgop.gpu;

import org.lwjgl.assimp.AITexel;
import org.lwjgl.assimp.AITexture;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL46.*;

public class GPUTexture {

    public enum TextureTarget {
        TEXTURE_1D(GL_TEXTURE_1D),
        TEXTURE_2D(GL_TEXTURE_2D),
        TEXTURE_3D(GL_TEXTURE_3D),
        TEXTURE_RECTANGLE(GL_TEXTURE_RECTANGLE),
        TEXTURE_BUFFER(GL_TEXTURE_BUFFER),
        TEXTURE_CUBE_MAP(GL_TEXTURE_CUBE_MAP),
        TEXTURE_1D_ARRAY(GL_TEXTURE_1D_ARRAY),
        TEXTURE_2D_ARRAY(GL_TEXTURE_2D_ARRAY),
        TEXTURE_CUBE_MAP_ARRAY(GL_TEXTURE_CUBE_MAP_ARRAY),
        TEXTURE_2D_MULTISAMPLE(GL_TEXTURE_2D_MULTISAMPLE),
        TEXTURE_2D_MULTISAMPLE_ARRAY(GL_TEXTURE_2D_MULTISAMPLE_ARRAY),

        ;

        final int glQualifier;
        TextureTarget(int glQualifier) {
            this.glQualifier = glQualifier;
        }
    }

    public static GPUTexture loadFromBytes(ByteBuffer data) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GPUTexture texture = new GPUTexture(TextureTarget.TEXTURE_2D);
            IntBuffer x = stack.mallocInt(1);
            IntBuffer y = stack.mallocInt(1);
            IntBuffer channelsInFile = stack.mallocInt(1);
            ByteBuffer out = STBImage.stbi_load_from_memory(data, x, y, channelsInFile, 4);
            if (out == null) throw new IllegalStateException("Bad texture data passed to loadFromBytes");
            int w = x.get(0), h = y.get(0);
            texture.storage(1, GL_RGBA8, w, h);
            glTextureSubImage2D(texture.handle(), 0, 0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE, out);
            STBImage.stbi_image_free(out);
            glTextureParameteri(texture.handle(), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTextureParameteri(texture.handle(), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            return texture;
        }
    }

    public static GPUTexture loadFromRawBytes(ByteBuffer data, int color, int format, int type, int width, int height) {
        GPUTexture texture = new GPUTexture(TextureTarget.TEXTURE_2D);
        texture.storage(1, color, width, height);
        glTextureSubImage2D(texture.handle(), 0, 0, 0, width, height, format, type, data);
        glTextureParameteri(texture.handle(), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTextureParameteri(texture.handle(), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
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

    public static GPUTexture MISSING_TEXTURE;

    public static void loadMissingTexture() {
        MISSING_TEXTURE = new GPUTexture(TextureTarget.TEXTURE_2D);
        MISSING_TEXTURE.storage(1, GL_RGBA8, 2, 2);
        ByteBuffer buffer = MemoryUtil.memAlloc(Integer.BYTES*2*2);
        buffer.putInt(0x000000FF);
        buffer.putInt(0xFF00FFFF);
        buffer.putInt(0xFF00FFFF);
        buffer.putInt(0x000000FF);
        glTextureSubImage2D(MISSING_TEXTURE.handle(), 0, 0, 0, 2, 2, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        glTextureParameteri(MISSING_TEXTURE.handle(), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTextureParameteri(MISSING_TEXTURE.handle(), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTextureParameteri(MISSING_TEXTURE.handle, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTextureParameteri(MISSING_TEXTURE.handle, GL_TEXTURE_WRAP_T, GL_REPEAT);
    }

    private final int handle;
    private final TextureTarget target;

    public GPUTexture(TextureTarget target) {
        this.handle = glCreateTextures(target.glQualifier);
        if (handle == 0) {
            throw new IllegalStateException("Failed to create a new GPU texture");
        }
        this.target = target;
    }

    public void storage(int levels, int color, int width) {
        glTextureStorage1D(handle, levels, color, width);
    }

    public void storage(int levels, int color, int width, int height) {
        glTextureStorage2D(handle, levels, color, width, height);
    }

    public void storage(int levels, int color, int width, int height, int depth) {
        glTextureStorage3D(handle, levels, color, width, height, depth);
    }

    public int handle() {
        return handle;
    }

    public int target() {
        return target.glQualifier;
    }

    public void destroy() {
        glDeleteTextures(handle);
    }
}
