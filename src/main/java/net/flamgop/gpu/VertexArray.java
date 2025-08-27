package net.flamgop.gpu;

import net.flamgop.gpu.buffer.GPUBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL46.*;

public class VertexArray {

    private final int vao;
    private final GPUBuffer[] buffers;

    private GPUBuffer elementBuffer;
    private int indexCount = 0;

    public static VertexArray withDefaultVertexFormat(Vertex[] vertexData, int[] indexData) {
        VertexArray buffer = new VertexArray();
        buffer.data(vertexData, indexData);
        buffer.attribute(0, 3, GL_FLOAT, false, 0); // position
        buffer.attribute(1, 2, GL_FLOAT, false, 3 * Float.BYTES); // uv
        buffer.attribute(2, 4, GL_INT_2_10_10_10_REV, true, 5 * Float.BYTES); // normal
        buffer.attribute(3, 4, GL_INT_2_10_10_10_REV, true, 5 * Float.BYTES + Integer.BYTES); // tangent
        return buffer;
    }

    public VertexArray() {
        this.vao = glCreateVertexArrays();
        this.buffers = new GPUBuffer[glGetInteger(GL_MAX_VERTEX_ATTRIBS)];
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

    private void data(Vertex[] vertexData, int[] indexData) {
        ByteBuffer vertices = MemoryUtil.memAlloc(vertexData.length * Vertex.BYTES);
        for (Vertex vertex : vertexData) {
            vertex.get(vertices);
        }
        vertices.flip();
        IntBuffer indices = MemoryUtil.memAllocInt(indexData.length * Integer.BYTES);
        indices.put(indexData).flip();

        GPUBuffer vbo = new GPUBuffer(GPUBuffer.BufferUsage.STATIC_DRAW);
        GPUBuffer ebo = new GPUBuffer(GPUBuffer.BufferUsage.STATIC_DRAW);
        vbo.allocate(vertices);
        ebo.allocate(indices);

        buffer(vbo, 0, 0, Vertex.BYTES);
        elementBuffer(ebo);

        this.indexCount(indexData.length);

        MemoryUtil.memFree(vertices);
        MemoryUtil.memFree(indices);
    }

    public void data(float[] vertexData, int vertexStride, int[] indexData) {
        FloatBuffer vertices = MemoryUtil.memAllocFloat(vertexData.length * Float.BYTES);
        vertices.put(vertexData).flip();
        IntBuffer indices = MemoryUtil.memAllocInt(indexData.length * Integer.BYTES);
        indices.put(indexData).flip();
        data(vertices, vertexStride, indices, indexData.length);
        MemoryUtil.memFree(vertices);
        MemoryUtil.memFree(indices);
    }

    public void data(FloatBuffer vertexData, int vertexStride, IntBuffer indexData, int indexCount) {
        GPUBuffer vbo = new GPUBuffer(GPUBuffer.BufferUsage.STATIC_DRAW);
        GPUBuffer ebo = new GPUBuffer(GPUBuffer.BufferUsage.STATIC_DRAW);
        vbo.allocate(vertexData);
        ebo.allocate(indexData);

        buffer(vbo, 0, 0, vertexStride);
        elementBuffer(ebo);

        this.indexCount(indexCount);
    }

    public void indexCount(int indexCount) {
        this.indexCount = indexCount;
    }

    public void buffer(GPUBuffer buffer, int bindingIndex, int offset, int stride) {
        buffers[bindingIndex] = buffer;
        glVertexArrayVertexBuffer(vao, bindingIndex, buffer.handle(), offset, stride);
    }

    public void elementBuffer(GPUBuffer buffer) {
        this.elementBuffer = buffer;
        glVertexArrayElementBuffer(vao, buffer.handle());
    }

    public void attribute(int index, int size, int type, boolean normalized, int relativeOffset) {
        attribute(index, size, type, normalized, relativeOffset, 0, 0);
    }

    public void attribute(int index, int size, int type, boolean normalized, int relativeOffset, int bindingIndex, int bindingDivisor) {
        glEnableVertexArrayAttrib(vao, index);
        glVertexArrayAttribFormat(vao, index, size, type, normalized, relativeOffset);
        glVertexArrayAttribBinding(vao, index, bindingIndex);
        glVertexArrayBindingDivisor(vao, bindingIndex, bindingDivisor);
    }

    public void draw() {
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
    }

    public void destroy() {
        glDeleteVertexArrays(vao);
        this.elementBuffer.destroy();
        for (GPUBuffer buffer : buffers) {
            if (buffer != null) buffer.destroy();
        }
    }
}
