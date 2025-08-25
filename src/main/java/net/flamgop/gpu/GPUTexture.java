package net.flamgop.gpu;

import net.flamgop.Game;
import net.flamgop.gpu.buffer.GPUBuffer;
import net.flamgop.util.ResourceHelper;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.assimp.AITexel;
import org.lwjgl.assimp.AITexture;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
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
        MISSING_TEXTURE = GPUTexture.loadFromBytes(ResourceHelper.loadFileFromResource("missing.png"));
        MISSING_TEXTURE.label("Missing Texture");
    }


    private static VertexArray ATLAS_COMPATIBLE_UNIT_QUAD;
    private static ShaderProgram BLIT_SHADER;
    private static int PROJECTION_LOCATION;
    private static int TINT_LOCATION;
    private static GPUBuffer UV_BUFFER;
    public static void loadBlit() {
        ATLAS_COMPATIBLE_UNIT_QUAD = new VertexArray();
        ATLAS_COMPATIBLE_UNIT_QUAD.data(new float[]{
                0, 1, 0, 0,
                0, 0, 0, 1,
                1, 0, 1, 1,
                1, 1, 1, 0
        }, 4 * Float.BYTES, new int[]{
                0, 1, 2,
                0, 2, 3
        });

        ATLAS_COMPATIBLE_UNIT_QUAD.attribute(0, 2, GL_FLOAT, false, 0);
        ATLAS_COMPATIBLE_UNIT_QUAD.attribute(1, 2, GL_FLOAT, false, 2 * Float.BYTES);
        ATLAS_COMPATIBLE_UNIT_QUAD.attribute(2, 4, GL_FLOAT, false, 0, 1, 1);
        ATLAS_COMPATIBLE_UNIT_QUAD.attribute(3, 2, GL_FLOAT, false, 4 * Float.BYTES, 1, 1);
        ATLAS_COMPATIBLE_UNIT_QUAD.attribute(4, 2, GL_FLOAT, false, 6 * Float.BYTES, 1, 1);
        ATLAS_COMPATIBLE_UNIT_QUAD.label("Atlas Compatible Unit Quad");

        UV_BUFFER = new GPUBuffer(GPUBuffer.BufferUsage.DYNAMIC_DRAW);
        UV_BUFFER.label("Blit Buffer");
        ATLAS_COMPATIBLE_UNIT_QUAD.buffer(UV_BUFFER, 1, 0, 8 * Float.BYTES);

        BLIT_SHADER = new ShaderProgram();
        BLIT_SHADER.attachShaderSource("Blit Vertex Shader", ResourceHelper.loadFileContentsFromResource("shaders/blit.vertex.glsl"), GL_VERTEX_SHADER);
        BLIT_SHADER.attachShaderSource("Blit Fragment Shader", ResourceHelper.loadFileContentsFromResource("shaders/blit.fragment.glsl"), GL_FRAGMENT_SHADER);
        BLIT_SHADER.link();
        BLIT_SHADER.label("Atlas Compatible Instanced Partial Blit Program");

        TINT_LOCATION = BLIT_SHADER.getUniformLocation("tint");
        PROJECTION_LOCATION = BLIT_SHADER.getUniformLocation("projection");
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

    public void label(String label) {
        glObjectLabel(GL_TEXTURE, this.handle, label);
    }

    public void blit(int x, int y, int w, int h) {
        blit(x, y, w, h, new Vector3f(1));
    }

    public void blit(int x, int y, int w, int h, Vector3f tint) {
        blit(0,0,1,1,x,y,w,h,tint);
    }

    public void blit(int atlasX, int atlasY, int atlasW, int atlasH, int x, int y, int w, int h, Vector3f tint) {
        FloatBuffer buffer = MemoryUtil.memAllocFloat(8 * Float.BYTES);
        buffer.put(atlasX);
        buffer.put(atlasY);
        buffer.put(atlasW);
        buffer.put(atlasH);
        buffer.put(x);
        buffer.put(y);
        buffer.put(w);
        buffer.put(h);
        buffer.flip();
        blitInstanced(1, buffer, Game.INSTANCE.window().ortho(), tint);
        MemoryUtil.memFree(buffer);
    }

    public void blitInstanced(int instanceCount, FloatBuffer instances, Matrix4f projection, Vector3f tint) {
        BLIT_SHADER.use();
        glUniform3f(TINT_LOCATION, tint.x, tint.y, tint.z);
        glUniformMatrix4fv(PROJECTION_LOCATION, false, projection.get(new float[16]));
        UV_BUFFER.allocate(instances);
        glBindTextureUnit(0, this.handle);
        glBindVertexArray(ATLAS_COMPATIBLE_UNIT_QUAD.handle());
        glDrawElementsInstanced(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0, instances.remaining() / 8);
        glBindVertexArray(0);
        glBindTextureUnit(0,0);
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
