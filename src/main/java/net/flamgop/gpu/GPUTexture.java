package net.flamgop.gpu;

import net.flamgop.Game;
import net.flamgop.asset.AssetIdentifier;
import net.flamgop.asset.AssetManager;
import net.flamgop.gpu.buffer.GPUBuffer;
import net.flamgop.util.ResourceHelper;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.Buffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL46.*;

public class GPUTexture {

    public enum Target {
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
        Target(int glQualifier) {
            this.glQualifier = glQualifier;
        }
    }

    public enum DepthStencilTextureMode {
        DEPTH_COMPONENT(GL_DEPTH_COMPONENT),
        STENCIL_INDEX(GL_STENCIL_INDEX),

        ;

        final int glQualifier;
        DepthStencilTextureMode(int glQualifier) {
            this.glQualifier = glQualifier;
        }

        public static DepthStencilTextureMode wrap(int glQualifier) {
            return switch (glQualifier) {
                case GL_DEPTH_COMPONENT -> DepthStencilTextureMode.DEPTH_COMPONENT;
                case GL_STENCIL_INDEX -> DepthStencilTextureMode.STENCIL_INDEX;
                default -> throw new IllegalArgumentException("Unknown depth stencil texture mode: " + glQualifier);
            };
        }
    }

    public enum CompareFunc {
        LEQUAL(GL_LEQUAL),
        GEQUAL(GL_GEQUAL),
        LESS(GL_LESS),
        GREATER(GL_GREATER),
        EQUAL(GL_EQUAL),
        NOTEQUAL(GL_NOTEQUAL),
        ALWAYS(GL_ALWAYS),
        NEVER(GL_NEVER),

        ;
        final int glQualifier;
        CompareFunc(int glQualifier) {
            this.glQualifier = glQualifier;
        }

        public static CompareFunc wrap(int glQualifier) {
            return switch (glQualifier) {
                case GL_LEQUAL -> LEQUAL;
                case GL_GEQUAL -> GEQUAL;
                case GL_LESS -> LESS;
                case GL_GREATER -> GREATER;
                case GL_EQUAL -> EQUAL;
                case GL_NOTEQUAL -> NOTEQUAL;
                case GL_ALWAYS -> ALWAYS;
                case GL_NEVER -> NEVER;
                default -> throw new IllegalArgumentException("Unknown texture compare func: " + glQualifier);
            };
        }
    }

    public enum CompareMode {
        COMPARE_REF_TO_TEXTURE(GL_COMPARE_REF_TO_TEXTURE),
        NONE(GL_NONE),

        ;
        final int glQualifier;
        CompareMode(int glQualifier) {
            this.glQualifier = glQualifier;
        }
        public static CompareMode wrap(int glQualifier) {
            return switch (glQualifier) {
                case GL_COMPARE_REF_TO_TEXTURE -> COMPARE_REF_TO_TEXTURE;
                case GL_NONE -> NONE;
                default -> throw new IllegalArgumentException("Unknown texture compare mode: " + glQualifier);
            };
        }
    }

    public enum MinFilter {
        NEAREST(GL_NEAREST),
        LINEAR(GL_LINEAR),
        NEAREST_MIPMAP_NEAREST(GL_NEAREST_MIPMAP_NEAREST),
        LINEAR_MIPMAP_NEAREST(GL_LINEAR_MIPMAP_NEAREST),
        NEAREST_MIPMAP_LINEAR(GL_NEAREST_MIPMAP_LINEAR),
        LINEAR_MIPMAP_LINEAR(GL_LINEAR_MIPMAP_LINEAR),

        ;
        final int glQualifier;
        MinFilter(int glQualifier) {
            this.glQualifier = glQualifier;
        }
        public static MinFilter wrap(int glQualifier) {
            return switch (glQualifier) {
                case GL_NEAREST -> NEAREST;
                case GL_LINEAR -> LINEAR;
                case GL_NEAREST_MIPMAP_NEAREST -> NEAREST_MIPMAP_NEAREST;
                case GL_LINEAR_MIPMAP_NEAREST -> LINEAR_MIPMAP_NEAREST;
                case GL_NEAREST_MIPMAP_LINEAR -> NEAREST_MIPMAP_LINEAR;
                case GL_LINEAR_MIPMAP_LINEAR -> LINEAR_MIPMAP_LINEAR;
                default -> throw new IllegalArgumentException("Unknown texture min filter: " + glQualifier);
            };
        }
    }

    public enum MagFilter {
        NEAREST(GL_NEAREST),
        LINEAR(GL_LINEAR),

        ;
        final int glQualifier;
        MagFilter(int glQualifier) {
            this.glQualifier = glQualifier;
        }
        public static MagFilter wrap(int glQualifier) {
            return switch (glQualifier) {
                case GL_NEAREST -> NEAREST;
                case GL_LINEAR -> LINEAR;
                default -> throw new IllegalArgumentException("Unknown texture mag filter: " + glQualifier);
            };
        }
    }

    public enum Swizzle {
        RED(GL_RED),
        GREEN(GL_GREEN),
        BLUE(GL_BLUE),
        ALPHA(GL_ALPHA),
        ZERO(GL_ZERO),
        ONE(GL_ONE),

        ;
        final int glQualifier;
        Swizzle(int glQualifier) {
            this.glQualifier = glQualifier;
        }
        public static Swizzle wrap(int glQualifier) {
            return switch (glQualifier) {
                case GL_RED -> RED;
                case GL_GREEN -> GREEN;
                case GL_BLUE -> BLUE;
                case GL_ALPHA -> ALPHA;
                case GL_ZERO -> ZERO;
                case GL_ONE -> ONE;
                default -> throw new IllegalArgumentException("Unknown texture swizzle: " + glQualifier);
            };
        }
    }
    public enum Wrap {
        CLAMP_TO_EDGE(GL_CLAMP_TO_EDGE),
        CLAMP_TO_BORDER(GL_CLAMP_TO_BORDER),
        MIRRORED_REPEAT(GL_MIRRORED_REPEAT),
        REPEAT(GL_REPEAT),
        MIRROR_CLAMP_TO_EDGE(GL_MIRROR_CLAMP_TO_EDGE),

        ;
        final int glQualifier;
        Wrap(int glQualifier) {
            this.glQualifier = glQualifier;
        }
        public static Wrap wrap(int glQualifier) {
            return switch (glQualifier) {
                case GL_CLAMP_TO_EDGE -> CLAMP_TO_EDGE;
                case GL_CLAMP_TO_BORDER -> CLAMP_TO_BORDER;
                case GL_MIRRORED_REPEAT -> MIRRORED_REPEAT;
                case GL_REPEAT -> REPEAT;
                case GL_MIRROR_CLAMP_TO_EDGE -> MIRROR_CLAMP_TO_EDGE;
                default -> throw new IllegalArgumentException("Unknown texture wrap: " + glQualifier);
            };
        }
    }

    public static GPUTexture MISSING_TEXTURE;
    public static GPUTexture MISSING_NORMAL;

    public static void loadMissingTexture(AssetManager assetManager) {
        MISSING_TEXTURE = assetManager.loadSync(new AssetIdentifier("missing.png"), GPUTexture.class).get();
        MISSING_TEXTURE.label("Missing Texture");
        MISSING_NORMAL = assetManager.loadSync(new AssetIdentifier("missing_normal.png"), GPUTexture.class).get();
        MISSING_NORMAL.label("Missing Normal");
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
    private final Target target;

    public GPUTexture(Target target) {
        this.handle = glCreateTextures(target.glQualifier);
        if (handle == 0) {
            throw new IllegalStateException("Failed to create a new GPU texture");
        }
        this.target = target;
    }

    public void bindToUnit(int unit) {
        glBindTextureUnit(unit, handle);
    }

    public void depthStencilTextureMode(DepthStencilTextureMode mode) {
        glTextureParameteri(this.handle, GL_DEPTH_STENCIL_TEXTURE_MODE, mode.glQualifier);
    }

    public DepthStencilTextureMode depthStencilTextureMode() {
        return DepthStencilTextureMode.wrap(glGetTextureParameteri(this.handle, GL_DEPTH_STENCIL_TEXTURE_MODE));
    }

    public void baseLevel(int baseLevel) {
        if (this.target == Target.TEXTURE_RECTANGLE && baseLevel != 0) {
            throw new IllegalArgumentException("Texture.TEXTURE_RECTANGLE doesn't support a base level other than 0. (GL_INVALID_OPERATION)");
        }
        if ((this.target == Target.TEXTURE_2D_MULTISAMPLE || this.target == Target.TEXTURE_2D_MULTISAMPLE_ARRAY) && baseLevel != 0) {
            throw new IllegalArgumentException("Multisample textures do not support a base level other than 0. (GL_INVALID_OPERATION)");
        }
        glTextureParameteri(this.handle, GL_TEXTURE_BASE_LEVEL, baseLevel);
    }

    public int baseLevel() {
        return glGetTextureParameteri(this.handle, GL_TEXTURE_BASE_LEVEL);
    }

    public void borderColor(float r, float g, float b, float a) {
        glTextureParameterfv(this.handle, GL_TEXTURE_BORDER_COLOR, new float[]{r, g, b, a});
    }

    public void borderColor(Vector4f color) {
        this.borderColor(color.x, color.y, color.z, color.w);
    }

    public Vector4f borderColor() {
        float[] out = new float[4];
        glGetTextureParameterfv(this.handle, GL_TEXTURE_BORDER_COLOR, out);
        return new Vector4f(out[0], out[1], out[2], out[3]);
    }

    public void compareFunc(CompareFunc func) {
        glTextureParameteri(this.handle, GL_TEXTURE_COMPARE_FUNC, func.glQualifier);
    }

    public CompareFunc compareFunc() {
        return CompareFunc.wrap(glGetTextureParameteri(this.handle, GL_TEXTURE_COMPARE_FUNC));
    }

    public void compareMode(CompareMode mode) {
        glTextureParameteri(this.handle, GL_TEXTURE_COMPARE_MODE, mode.glQualifier);
    }

    public CompareMode compareMode() {
        return CompareMode.wrap(glGetTextureParameteri(this.handle, GL_TEXTURE_COMPARE_MODE));
    }

    public void lodBias(float bias) {
        glTextureParameterf(this.handle, GL_TEXTURE_LOD_BIAS, bias);
    }

    public float lodBias() {
        return glGetTextureParameterf(this.handle, GL_TEXTURE_LOD_BIAS);
    }

    public void minFilter(MinFilter minFilter) {
        if (this.target == Target.TEXTURE_RECTANGLE && (minFilter != MinFilter.NEAREST && minFilter != MinFilter.LINEAR)) {
            throw new IllegalArgumentException("Target.TEXTURE_RECTANGLE does not support mipmapping. (GL_INVALID_ENUM)");
        }
        glTextureParameteri(this.handle, GL_TEXTURE_MIN_FILTER, minFilter.glQualifier);
    }

    public MinFilter minFilter() {
        return MinFilter.wrap(glGetTextureParameteri(this.handle, GL_TEXTURE_MIN_FILTER));
    }

    public void magFilter(MagFilter magFilter) {
        glTextureParameteri(this.handle, GL_TEXTURE_MAG_FILTER, magFilter.glQualifier);
    }

    public MagFilter magFilter() {
        return MagFilter.wrap(glGetTextureParameteri(this.handle, GL_TEXTURE_MAG_FILTER));
    }

    public void minLod(float minLod) {
        glTextureParameterf(this.handle, GL_TEXTURE_MIN_LOD, minLod);
    }

    public float minLod() {
        return glGetTextureParameterf(this.handle, GL_TEXTURE_MIN_LOD);
    }

    public void maxLod(float maxLod) {
        glTextureParameterf(this.handle, GL_TEXTURE_MAX_LOD, maxLod);
    }

    public float maxLod() {
        return glGetTextureParameterf(this.handle, GL_TEXTURE_MAX_LOD);
    }

    public void maxLevel(int level) {
        glTextureParameteri(this.handle, GL_TEXTURE_MAX_LEVEL, level);
    }

    public int maxLevel() {
        return glGetTextureParameteri(this.handle, GL_TEXTURE_MAX_LEVEL);
    }

    public void swizzleR(Swizzle swizzle) {
        glTextureParameteri(this.handle, GL_TEXTURE_SWIZZLE_R, swizzle.glQualifier);
    }

    public Swizzle swizzleR() {
        return Swizzle.wrap(glGetTextureParameteri(this.handle, GL_TEXTURE_SWIZZLE_R));
    }

    public void swizzleG(Swizzle swizzle) {
        glTextureParameteri(this.handle, GL_TEXTURE_SWIZZLE_G, swizzle.glQualifier);
    }

    public Swizzle swizzleG() {
        return Swizzle.wrap(glGetTextureParameteri(this.handle, GL_TEXTURE_SWIZZLE_G));
    }

    public void swizzleB(Swizzle swizzle) {
        glTextureParameteri(this.handle, GL_TEXTURE_SWIZZLE_B, swizzle.glQualifier);
    }

    public Swizzle swizzleB() {
        return Swizzle.wrap(glGetTextureParameteri(this.handle, GL_TEXTURE_SWIZZLE_B));
    }

    public void swizzleA(Swizzle swizzle) {
        glTextureParameteri(this.handle, GL_TEXTURE_SWIZZLE_A, swizzle.glQualifier);
    }

    public Swizzle swizzleA() {
        return Swizzle.wrap(glGetTextureParameteri(this.handle, GL_TEXTURE_SWIZZLE_A));
    }

    public void swizzleRGBA(Swizzle r, Swizzle g, Swizzle b, Swizzle a) {
        glTextureParameteriv(this.handle, GL_TEXTURE_SWIZZLE_RGBA, new int[]{r.glQualifier,g.glQualifier,b.glQualifier,a.glQualifier});
    }

    public Swizzle[] swizzleRGBA() {
        Swizzle[] rgba = new Swizzle[4];
        int[] raw = new int[4];
        glGetTextureParameteriv(this.handle, GL_TEXTURE_SWIZZLE_RGBA, raw);
        for (int i = 0; i < 4; i++) {
            rgba[i] = Swizzle.wrap(raw[i]);
        }
        return rgba;
    }

    public void wrapS(Wrap wrap) {
        if (this.target == Target.TEXTURE_RECTANGLE && (wrap == Wrap.MIRROR_CLAMP_TO_EDGE || wrap == Wrap.MIRRORED_REPEAT || wrap == Wrap.REPEAT)) {
            throw new IllegalArgumentException("Target.TEXTURE_RECTANGLE does not support mirror clamp to edge, mirrored repeat, or repeat. (GL_INVALID_ENUM)");
        }
        glTextureParameteri(this.handle, GL_TEXTURE_WRAP_S, wrap.glQualifier);
    }

    public Wrap wrapS() {
        return Wrap.wrap(glGetTextureParameteri(this.handle, GL_TEXTURE_WRAP_S));
    }

    public void wrapT(Wrap wrap) {
        if (this.target == Target.TEXTURE_RECTANGLE && (wrap == Wrap.MIRROR_CLAMP_TO_EDGE || wrap == Wrap.MIRRORED_REPEAT || wrap == Wrap.REPEAT)) {
            throw new IllegalArgumentException("Target.TEXTURE_RECTANGLE does not support mirror clamp to edge, mirrored repeat, or repeat. (GL_INVALID_ENUM)");
        }
        glTextureParameteri(this.handle, GL_TEXTURE_WRAP_T, wrap.glQualifier);
    }

    public Wrap wrapT() {
        return Wrap.wrap(glGetTextureParameteri(this.handle, GL_TEXTURE_WRAP_T));
    }

    public void wrapR(Wrap wrap) {
        glTextureParameteri(this.handle, GL_TEXTURE_WRAP_R, wrap.glQualifier);
    }

    public Wrap wrapR() {
        return Wrap.wrap(glGetTextureParameteri(this.handle, GL_TEXTURE_WRAP_R));
    }

    public void maxAnisotropy(float maxAnisotropy) {
        glTextureParameterf(this.handle, GL_TEXTURE_MAX_ANISOTROPY, maxAnisotropy);
    }

    public float maxAnisotropy() {
        return glGetTextureParameterf(this.handle, GL_TEXTURE_MAX_ANISOTROPY);
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

    public void subimage(int level, int xoffset, int width, int format, int type, long pixels) {
        glTextureSubImage1D(this.handle, level, xoffset, width, format, type, pixels);
    }

    public void subimage(int level, int xoffset, int yoffset, int width, int height, int format, int type, long pixels) {
        glTextureSubImage2D(this.handle, level, xoffset, yoffset, width, height, format, type, pixels);
    }

    public void subimage(int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, long pixels) {
        glTextureSubImage3D(this.handle, level, xoffset, yoffset, zoffset, width, height, depth, format, type, pixels);
    }

    public void subimage(int level, int xoffset, int width, int format, int type, Buffer pixels) {
        glTextureSubImage1D(this.handle, level, xoffset, width, format, type, MemoryUtil.memAddress(pixels));
    }

    public void subimage(int level, int xoffset, int yoffset, int width, int height, int format, int type, Buffer pixels) {
        glTextureSubImage2D(this.handle, level, xoffset, yoffset, width, height, format, type, MemoryUtil.memAddress(pixels));
    }

    public void subimage(int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, Buffer pixels) {
        glTextureSubImage3D(this.handle, level, xoffset, yoffset, zoffset, width, height, depth, format, type, MemoryUtil.memAddress(pixels));
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
