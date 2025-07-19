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

    public void drawText(Font font, String text, float x, float y, float scale, Vector3f color) {
        textShader.use();
        glUniform3f(textColorUniformLocation, color.x, color.y, color.z);
        glBindVertexArray(unitQuad.handle());
        glBindTextureUnit(0, font.atlas().handle());

        int maxInstances = text.length();
        FloatBuffer instanceData = MemoryUtil.memAllocFloat(8 * Float.BYTES * maxInstances);

        int instanceCount = 0;

        for (char c : text.toCharArray()) {
            Glyph glyph = font.glyphs().get(c);
            if (glyph == null) {
                x += (font.glyphs().get(' ').advance() >> 6) * scale;
                continue;
            }

            if (!glyph.isEmpty()) {
                float xPos = x + glyph.bearing().x * scale;
                float yPos = y - (glyph.size().y - glyph.bearing().y) * scale;

                float w = glyph.size().x * scale;
                float h = glyph.size().y * scale;
                instanceData.put(glyph.uv().x());
                instanceData.put(glyph.uv().y());
                instanceData.put(glyph.uv().z());
                instanceData.put(glyph.uv().w());
                instanceData.put(xPos);
                instanceData.put(yPos);
                instanceData.put(w);
                instanceData.put(h);
                instanceCount++;
            }
            x += (glyph.advance() >> 6) * scale;
        }
        instanceData.flip();

        textUVBuffer.allocate(instanceData);
        glDrawElementsInstanced(GL_TRIANGLES, unitQuad.indexCount(), GL_UNSIGNED_INT, 0, instanceCount);
        MemoryUtil.memFree(instanceData);

        glBindVertexArray(0);
        glBindTextureUnit(0, 0);
    }
}
