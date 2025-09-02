package net.flamgop.gpu.vertex;

import net.flamgop.gpu.buffer.GPUBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL46.*;

public class VertexArray {

    public enum IndexType {
        UNSIGNED_BYTE(GL_UNSIGNED_BYTE),
        UNSIGNED_SHORT(GL_UNSIGNED_SHORT),
        UNSIGNED_INT(GL_UNSIGNED_INT),

        ;
        final int glQualifier;
        IndexType(int glQualifier) {
            this.glQualifier = glQualifier;
        }
    }

    public enum DrawMode {
        POINTS(GL_POINTS),
        LINE_STRIP(GL_LINE_STRIP),
        LINE_LOOP(GL_LINE_LOOP),
        LINES(GL_LINES),
        LINE_STRIP_ADJACENCY(GL_LINE_STRIP_ADJACENCY),
        LINES_ADJACENCY(GL_LINES_ADJACENCY),
        TRIANGLE_STRIP(GL_TRIANGLE_STRIP),
        TRIANGLE_FAN(GL_TRIANGLE_FAN),
        TRIANGLES(GL_TRIANGLES),
        TRIANGLE_STRIP_ADJACENCY(GL_TRIANGLE_STRIP_ADJACENCY),
        TRIANGLES_ADJACENCY(GL_TRIANGLES_ADJACENCY),
        PATCHES(GL_PATCHES),

        ;
        final int glQualifier;
        DrawMode(int glQualifier) {
            this.glQualifier = glQualifier;
        }
    }

    private final int vao;
    private final GPUBuffer[] buffers;
    private final VertexFormat format;

    private GPUBuffer elementBuffer;
    private IndexType indexType;
    private int indexCount = 0;

    public VertexArray(VertexFormat format) {
        this.vao = glCreateVertexArrays();
        this.buffers = new GPUBuffer[glGetInteger(GL_MAX_VERTEX_ATTRIBS)];
        this.format = format;
        this.format.setup(this);
    }

    // I don't think there's a way to validate buffer against VertexFormat, as much as I'd like to.
    public void data(ByteBuffer data, int bindingIndex, int bufferOffset) {
        if (buffers[bindingIndex] != null) buffers[bindingIndex].destroy();
        GPUBuffer buffer = new GPUBuffer(GPUBuffer.BufferUsage.STATIC_DRAW);
        buffer.allocate(data);

        buffer(buffer, bindingIndex, bufferOffset);
        buffers[bindingIndex] = buffer;
    }

    public void elementData(ByteBuffer data, IndexType indexType, int indexCount) {
        if (this.elementBuffer != null) this.elementBuffer.destroy();
        this.elementBuffer = new GPUBuffer(GPUBuffer.BufferUsage.STATIC_DRAW);
        this.elementBuffer.allocate(data);

        this.elementBuffer(this.elementBuffer, indexType, indexCount);
    }

    public static VertexArray withDefaultVertexFormat(DefaultVertex[] vertices, int[] indices) {
        VertexArray buffer = new VertexArray(DefaultVertex.FORMAT);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer vertexData = MemoryUtil.memAlloc(vertices.length * DefaultVertex.BYTES);
            ByteBuffer indexData = MemoryUtil.memByteBuffer(stack.ints(indices));
            for (DefaultVertex vertex : vertices) vertex.get(vertexData);
            vertexData.flip();

            buffer.data(vertexData, 0, 0);
            buffer.elementData(indexData, IndexType.UNSIGNED_INT, indices.length);
            return buffer;
        }
    }

    public void label(String label) {
        glObjectLabel(GL_VERTEX_ARRAY, this.vao, label);
        for (int i = 0; i < this.buffers.length; i++) {
            GPUBuffer buffer = this.buffers[i];
            if (buffer != null)
                buffer.label(label + " Buffer " + i);
        }
        this.elementBuffer.label(label + " Element Buffer");
    }

    public int handle() {
        return vao;
    }

    public int indexCount() {
        return indexCount;
    }

    public void indexCount(int indexCount) {
        this.indexCount = indexCount;
    }

    public void buffer(GPUBuffer buffer, int bindingIndex, int offset) {
        buffers[bindingIndex] = buffer;
        glVertexArrayVertexBuffer(vao, bindingIndex, buffer.handle(), offset, this.format.stride(bindingIndex));
    }

    public void elementBuffer(GPUBuffer buffer, IndexType indexType, int indexCount) {
        this.elementBuffer = buffer;
        this.indexType = indexType;
        this.indexCount = indexCount;
        glVertexArrayElementBuffer(vao, buffer.handle());
    }

    protected void attribute(int index, int size, int type, boolean normalized, int relativeOffset, int bindingIndex, int bindingDivisor) {
        glEnableVertexArrayAttrib(vao, index);
        glVertexArrayAttribFormat(vao, index, size, type, normalized, relativeOffset);
        glVertexArrayAttribBinding(vao, index, bindingIndex);
        glVertexArrayBindingDivisor(vao, bindingIndex, bindingDivisor);
    }

    public void draw(DrawMode mode) {
        glBindVertexArray(vao);
        glDrawElements(mode.glQualifier, indexCount, indexType.glQualifier, 0);
    }

    public void drawInstanced(DrawMode mode, int instances) {
        glBindVertexArray(vao);
        glDrawElementsInstanced(mode.glQualifier, indexCount, indexType.glQualifier, 0, instances);
    }

    public void destroy() {
        glDeleteVertexArrays(vao);
        if (this.elementBuffer != null) this.elementBuffer.destroy();
        for (GPUBuffer buffer : buffers) {
            if (buffer != null) buffer.destroy();
        }
    }
}
