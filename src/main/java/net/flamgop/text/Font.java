package net.flamgop.text;

import net.flamgop.gpu.GPUTexture;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL30.GL_R8;
import static org.lwjgl.opengl.GL45.glTextureParameteri;
import static org.lwjgl.opengl.GL45.glTextureSubImage2D;

public class Font {

    private static long freeType = 0L;

    private static void initFreeType() {
        PointerBuffer pFreeType = MemoryUtil.memAllocPointer(1);
        if (FreeType.FT_Init_FreeType(pFreeType) != 0) {
            throw new IllegalStateException("Couldn't initialize freetype!");
        }
        freeType = pFreeType.get(0);
    }

    public static void destroyFreeType() {
        if (freeType != 0L) {
            FreeType.FT_Done_FreeType(freeType);
        }
    }

    private final Map<Character, Glyph> glyphs = new HashMap<>();
    private final int textAtlasWidth, textAtlasHeight, lineHeight;
    private final GPUTexture textAtlasTexture;
    private final ByteBuffer textAtlasBuffer;
    private final FT_Face typeface;

    public Font(ByteBuffer ttf, int glyphCount, int characterPadding, int textAtlasWidth, int textAtlasHeight) {
        this.textAtlasWidth = textAtlasWidth;
        this.textAtlasHeight = textAtlasHeight;

        if (freeType == 0L) {
            initFreeType();
        }

        typeface = loadTypeFace(ttf);

        FreeType.FT_Set_Pixel_Sizes(typeface, 0, 48);

        textAtlasBuffer = MemoryUtil.memAlloc(textAtlasWidth * textAtlasHeight);
        generateAtlas(glyphCount, characterPadding);

        textAtlasTexture = new GPUTexture(GPUTexture.TextureTarget.TEXTURE_2D);
        textAtlasTexture.storage(1, GL_R8, textAtlasWidth, textAtlasHeight);
        glTextureSubImage2D(textAtlasTexture.handle(), 0, 0, 0, textAtlasWidth, textAtlasHeight, GL_RED, GL_UNSIGNED_BYTE, textAtlasBuffer);
        glTextureParameteri(textAtlasTexture.handle(), GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTextureParameteri(textAtlasTexture.handle(), GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTextureParameteri(textAtlasTexture.handle(), GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTextureParameteri(textAtlasTexture.handle(), GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        FT_Size_Metrics metrics = typeface.size().metrics();
        long ascent = metrics.ascender() >> 6;
        long descent = metrics.descender() >> 6;
        long lineGap = (metrics.height() >> 6) - ascent + descent;

        this.lineHeight = (int)(ascent - descent + lineGap);
    }

    public GPUTexture atlas() {
        return textAtlasTexture;
    }

    @SuppressWarnings("resource")
    private void generateAtlas(int glyphCount, int characterPadding) {
        int x = 0;
        int y = 0;
        int rowHeight = 0;

        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        for (char c = 0; c < glyphCount; c++) {
            if (FreeType.FT_Load_Char(typeface, c, FreeType.FT_LOAD_RENDER) != 0) {
                throw new IllegalStateException("Couldn't load char " + c);
            }

            FT_GlyphSlot glyph = typeface.glyph();
            FT_Bitmap bitmap = glyph.bitmap();

            int w = bitmap.width();
            int h = bitmap.rows();

            if (w == 0 || h == 0) {
                this.glyphs.put(c, new Glyph(new Vector4f(0), new Vector2f(w, h), new Vector2f(glyph.bitmap_left(), glyph.bitmap_top()), (int) glyph.advance().x(), true));
                continue;
            }

            if (x + w >= textAtlasWidth) {
                x = 0;
                y += rowHeight;
                rowHeight = 0;
            }

            if (y + h >= textAtlasHeight) {
                throw new RuntimeException("Text atlas too small!");
            }

            ByteBuffer buffer = bitmap.buffer(w * h);
            for (int row = 0; row < h; row++) {
                int atlasPos = (y + row) * textAtlasWidth + x;
                for (int col = 0; col < w; col++) {
                    byte pixel = buffer.get(row * w + col);
                    textAtlasBuffer.put(atlasPos + col, pixel);
                }
            }

            float u0 = (float) x / textAtlasWidth;
            float v0 = (float) y / textAtlasHeight;
            float u1 = (float)(x + w) / textAtlasWidth;
            float v1 = (float)(y + h) / textAtlasHeight;

            this.glyphs.put(c, new Glyph(new Vector4f(u0, v0, u1-u0, v1-v0), new Vector2f(w, h), new Vector2f(glyph.bitmap_left(), glyph.bitmap_top()), (int) glyph.advance().x(), false));

            x += w + characterPadding;
            if (h > rowHeight) {
                rowHeight = h;
            }
        }
    }

    private FT_Face loadTypeFace(ByteBuffer ttf) {
        PointerBuffer pTypeFace = MemoryUtil.memAllocPointer(1);
        int ret = FreeType.FT_New_Memory_Face(freeType, ttf, 0, pTypeFace);
        if (ret != 0) {
            throw new IllegalStateException("Couldn't create my font typeface! code: " + ret);
        }
        return FT_Face.create(pTypeFace.get(0));
    }

    public int lineHeight() {
        return this.lineHeight;
    }

    public Map<Character, Glyph> glyphs() {
        return glyphs;
    }

    public void writeAtlasToDisk(File file) {
        BufferedImage image = new BufferedImage(textAtlasWidth, textAtlasHeight, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < textAtlasWidth; i++) {
            for (int j = 0; j < textAtlasHeight; j++) {
                int atlasPos = j * textAtlasWidth + i;
                int b = Byte.toUnsignedInt(textAtlasBuffer.get(atlasPos));
                int color = (b << 24) | 0xFFFFFF;
                image.setRGB(i, j, color);
            }
        }
        try {
            ImageIO.write(image, "png", file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void destroy() {
        FreeType.FT_Done_Face(typeface);
    }
}
