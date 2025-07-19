package net.flamgop.gpu;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

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

    private final int handle;
    private final TextureTarget target;

    public GPUTexture(TextureTarget target) {
        this.handle = glCreateTextures(target.glQualifier);
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
