package net.flamgop.text;

import net.flamgop.gpu.vertex.Attribute;
import net.flamgop.gpu.vertex.VertexFormat;
import net.flamgop.util.ResourceHelper;
import net.flamgop.gpu.buffer.GPUBuffer;
import net.flamgop.gpu.ShaderProgram;
import net.flamgop.gpu.vertex.VertexArray;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class TextRenderer {

    private static final int FLOATS_PER_INSTANCE = 8;
    private static final VertexFormat VERTEX_FORMAT = VertexFormat.builder()
            .attribute(0, Attribute.of(Attribute.Type.FLOAT, 2, false))
            .attribute(1, Attribute.of(Attribute.Type.FLOAT, 2, false))
            .attribute(2, Attribute.of(Attribute.Type.FLOAT, 4, false, 1, 1))
            .attribute(3, Attribute.of(Attribute.Type.FLOAT, 2, false, 1, 1))
            .attribute(4, Attribute.of(Attribute.Type.FLOAT, 2, false, 1, 1))
            .build();

    private final VertexArray unitQuad;
    private final ShaderProgram textShader;
    private final int textColorUniformLocation;
    private final int textProjectionUniformLocation;

    private final GPUBuffer textUVBuffer;
    private final Matrix4f projection = new Matrix4f();

    public TextRenderer(int width, int height) {
        textShader = new ShaderProgram();
        textShader.attachShaderSource("Text Vertex Shader", ResourceHelper.loadFileContentsFromResource("shaders/text.vertex.glsl"), ShaderProgram.ShaderType.VERTEX);
        textShader.attachShaderSource("Text Fragment Shader", ResourceHelper.loadFileContentsFromResource("shaders/text.fragment.glsl"), ShaderProgram.ShaderType.FRAGMENT);
        textShader.link();
        textShader.label("Text Program");

        textColorUniformLocation = textShader.getUniformLocation("text_color");
        textProjectionUniformLocation = textShader.getUniformLocation("projection");

        projection.ortho(0, width, 0, height, 0f, 1f);
        textShader.uniformMatrix4fv(textProjectionUniformLocation, false, projection);

        textUVBuffer = new GPUBuffer(GPUBuffer.BufferUsage.DYNAMIC_DRAW);
        textUVBuffer.label("Text UV Buffer");

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer vertices = MemoryUtil.memByteBuffer(stack.floats(
                    0, 1, 0, 0,
                    0, 0, 0, 1,
                    1, 0, 1, 1,
                    1, 1, 1, 0
            ));
            ByteBuffer indices = stack.bytes(new byte[] {0, 1, 2, 0, 2, 3});

            unitQuad = new VertexArray(VERTEX_FORMAT);
            unitQuad.data(vertices, 0, 0);
            unitQuad.elementData(indices, VertexArray.IndexType.UNSIGNED_BYTE, indices.capacity());

            unitQuad.buffer(textUVBuffer, 1, 0);

            unitQuad.label("Text Quad");
        }
    }

    public void resize(int width, int height) {
        projection.identity().ortho(0, width, 0, height, 0f, 1f);
        textShader.uniformMatrix4fv(textProjectionUniformLocation, false, projection);
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
        textShader.uniform3f(textColorUniformLocation, color);
        font.atlas().bindToUnit(0);
        textUVBuffer.allocate(buffer);
        unitQuad.drawInstanced(VertexArray.DrawMode.TRIANGLES, buffer.remaining() / FLOATS_PER_INSTANCE);
    }

    public FloatBuffer computeTextBuffer(Font font, String text, float x, float y, float scale) {
        float originX = x;

        int maxInstances = text.length();
        FloatBuffer instanceData = MemoryUtil.memAllocFloat(8 * maxInstances);

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
        FloatBuffer instanceData = MemoryUtil.memAllocFloat(8 * maxInstances);

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
