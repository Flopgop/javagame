package net.flamgop.text;

import net.flamgop.ResourceHelper;
import net.flamgop.gpu.GPUBuffer;
import net.flamgop.gpu.ShaderProgram;
import net.flamgop.gpu.VertexBuffer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL31.glDrawElementsInstanced;
import static org.lwjgl.opengl.GL45.glBindTextureUnit;

public class TextRenderer {

    private static final int FLOATS_PER_INSTANCE = 8;

    private final VertexBuffer unitQuad;
    private final ShaderProgram textShader;
    private final int textColorUniformLocation;

    private final GPUBuffer textUVBuffer;

    public TextRenderer() {
        textShader = new ShaderProgram();
        textShader.attachShaderSource(ResourceHelper.loadFileContentsFromResource("text.vertex.glsl"), GL_VERTEX_SHADER);
        textShader.attachShaderSource(ResourceHelper.loadFileContentsFromResource("text.fragment.glsl"), GL_FRAGMENT_SHADER);
        textShader.link();

        textColorUniformLocation = glGetUniformLocation(textShader.handle(), "text_color");
        final int textProjectionUniformLocation = glGetUniformLocation(textShader.handle(), "projection");

        textShader.use();
        Matrix4f orthogonal = new Matrix4f();
        orthogonal.ortho(0, 1280, 0, 720, 0f, 1f);
        glUniformMatrix4fv(textProjectionUniformLocation, false, orthogonal.get(new float[16]));

        unitQuad = new VertexBuffer();
        unitQuad.data(new float[]{
                0, 1, 0, 0,
                0, 0, 0, 1,
                1, 0, 1, 1,
                1, 1, 1, 0
        }, 4 * Float.BYTES, new int[]{
                0, 1, 2,
                0, 2, 3
        });

        textUVBuffer = new GPUBuffer(GPUBuffer.BufferUsage.DYNAMIC_DRAW);
        unitQuad.buffer(textUVBuffer, 1, 0, 8 * Float.BYTES);

        unitQuad.attribute(0, 2, GL_FLOAT, false, 0);
        unitQuad.attribute(1, 2, GL_FLOAT, false, 2 * Float.BYTES);
        unitQuad.attribute(2, 4, GL_FLOAT, false, 0, 1, 1);
        unitQuad.attribute(3, 2, GL_FLOAT, false, 4 * Float.BYTES, 1, 1);
        unitQuad.attribute(4, 2, GL_FLOAT, false, 6 * Float.BYTES, 1, 1);
    }

    private float addCharacterToBuffer(Font font, char c, float x, float y, float scale, FloatBuffer buffer) {
        Glyph glyph = font.glyphs().get(c);
        if (glyph == null) {
            return (font.glyphs().get(' ').advance() >> 6) * scale;
        }

        if (!glyph.isEmpty()) {
            float xPos = x + glyph.bearing().x * scale;
            float yPos = y - (glyph.size().y - glyph.bearing().y) * scale;

            float w = glyph.size().x * scale;
            float h = glyph.size().y * scale;
            buffer.put(glyph.uv().x());
            buffer.put(glyph.uv().y());
            buffer.put(glyph.uv().z());
            buffer.put(glyph.uv().w());
            buffer.put(xPos);
            buffer.put(yPos);
            buffer.put(w);
            buffer.put(h);
        }
        return (glyph.advance() >> 6) * scale;
    }

    public void drawText(Font font, String text, float x, float y, float scale, Vector3f color) {
        FloatBuffer instanceData = computeTextBuffer(font, text, x, y, scale);

        drawBufferedText(font, instanceData, color);

        MemoryUtil.memFree(instanceData);
    }

    public void drawTextWrapped(Font font, String text, float x, float y, float scale, Vector3f color, float maxWidth) {
        FloatBuffer instanceData = computeTextBufferWrapped(font, text, x, y, scale, maxWidth);

        drawBufferedText(font, instanceData, color);

        MemoryUtil.memFree(instanceData);
    }

    // this avoids safety checks for speed reasons
    public void drawBufferedText(Font font, FloatBuffer buffer, Vector3f color) {
        textShader.use();
        glUniform3f(textColorUniformLocation, color.x, color.y, color.z);
        glBindVertexArray(unitQuad.handle());
        glBindTextureUnit(0, font.atlas().handle());
        textUVBuffer.allocate(buffer);
        glDrawElementsInstanced(GL_TRIANGLES, unitQuad.indexCount(), GL_UNSIGNED_INT, 0, buffer.remaining() / FLOATS_PER_INSTANCE);
        glBindVertexArray(0);
        glBindTextureUnit(0, 0);
    }

    public FloatBuffer computeTextBuffer(Font font, String text, float x, float y, float scale) {
        float originX = x;

        int maxInstances = text.length();
        FloatBuffer instanceData = MemoryUtil.memAllocFloat(8 * Float.BYTES * maxInstances);

        for (char c : text.toCharArray()) {
            if (c == '\n') {
                y -= font.lineHeight() * scale;
                x = originX;
                continue;
            }
            float ascent = addCharacterToBuffer(font, c, x, y, scale, instanceData);
            x += ascent;
        }
        instanceData.flip();
        return instanceData;
    }

    public FloatBuffer computeTextBufferWrapped(Font font, String text, float x, float y, float scale, float maxWidth) {
        float originX = x;

        int maxInstances = text.length();
        FloatBuffer instanceData = MemoryUtil.memAllocFloat(8 * Float.BYTES * maxInstances);

        for (char c : text.toCharArray()) {
            if (c == '\n' || x - originX > maxWidth) {
                y -= font.lineHeight() * scale;
                x = originX;
                if (c == '\n') continue;
            }
            float ascent = addCharacterToBuffer(font, c, x, y, scale, instanceData);
            x += ascent;
        }
        instanceData.flip();
        return instanceData;
    }
}
