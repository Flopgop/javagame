package net.flamgop.gpu;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.par.ParShapes;
import org.lwjgl.util.par.ParShapesMesh;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL46.*;

public class VertexBuffer {

    private final int vao;
    private final List<GPUBuffer> buffers = new ArrayList<>();

    private GPUBuffer elementBuffer;
    private int indexCount = 0;

    public static VertexBuffer fromParShapesMesh(@NotNull ParShapesMesh mesh) {
        ParShapes.par_shapes_compute_normals(mesh);

        FloatBuffer points = mesh.points(mesh.npoints() * 3);
        FloatBuffer normals = mesh.normals(mesh.npoints() * 3);
        FloatBuffer uvs = mesh.tcoords(mesh.npoints() * 2);
        IntBuffer tris = mesh.triangles(mesh.ntriangles() * 3);

        boolean hasUVs = (uvs != null && uvs.capacity() >= mesh.npoints() * 2);

        float[] vertData = new float[mesh.npoints() * 8];
        for (int i = 0; i < mesh.npoints(); i++) {
            int pi  = i * 3;
            int ui  = i * 2;
            int vi  = i * 8;

            vertData[vi    ] = points.get(pi);
            vertData[vi + 1] = points.get(pi + 1);
            vertData[vi + 2] = points.get(pi + 2);

            vertData[vi + 3] = normals.get(pi);
            vertData[vi + 4] = normals.get(pi + 1);
            vertData[vi + 5] = normals.get(pi + 2);

            if (hasUVs) {
                vertData[vi + 6] = uvs.get(ui);
                vertData[vi + 7] = uvs.get(ui + 1);
            } else {
                // Default fallback UVs to (0, 0)
                vertData[vi + 6] = 0.0f;
                vertData[vi + 7] = 0.0f;
            }
        }

        if (!hasUVs) {
            for (int i = 0; i < mesh.ntriangles(); i += 2) {
                if (i + 1 >= mesh.ntriangles()) break;

                int i0 = tris.get(i * 3);
                int i1 = tris.get(i * 3 + 1);
                int i2 = tris.get(i * 3 + 2);

                int i3 = tris.get((i + 1) * 3 + 1); // typical for quad from triangles

                // Assign UVs to the quad's four corners
                vertData[i0 * 8 + 6] = 0.0f; vertData[i0 * 8 + 7] = 0.0f;
                vertData[i1 * 8 + 6] = 1.0f; vertData[i1 * 8 + 7] = 0.0f;
                vertData[i2 * 8 + 6] = 1.0f; vertData[i2 * 8 + 7] = 1.0f;
                vertData[i3 * 8 + 6] = 0.0f; vertData[i3 * 8 + 7] = 1.0f;
            }
        }

        // Extract triangle indices
        int[] indexData = new int[3 * mesh.ntriangles()];
        for (int i = 0; i < 3 * mesh.ntriangles(); i++) {
            indexData[i] = tris.get(i);
        }

        return withDefaultVertexFormat(vertData, indexData);
    }

    public static VertexBuffer withDefaultVertexFormat(float[] vertexData, int[] indexData) {
        VertexBuffer buffer = new VertexBuffer();
        buffer.data(vertexData, 8 * Float.BYTES, indexData);
        buffer.attribute(0, 3, GL_FLOAT, false, 0);
        buffer.attribute(1, 3, GL_FLOAT, false, 3 * Float.BYTES);
        buffer.attribute(2, 2, GL_FLOAT, false, 6 * Float.BYTES);
        return buffer;
    }

    public VertexBuffer() {
        this.vao = glCreateVertexArrays();
    }

    public int handle() {
        return vao;
    }

    public int indexCount() {
        return indexCount;
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
        buffers.add(buffer);
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
        this.buffers.forEach(GPUBuffer::destroy);
    }
}
