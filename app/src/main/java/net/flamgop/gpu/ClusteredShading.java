package net.flamgop.gpu;

import net.flamgop.Game;
import net.flamgop.gpu.buffer.BufferSerializable;
import net.flamgop.gpu.buffer.GPUBuffer;
import net.flamgop.gpu.buffer.ShaderStorageBuffer;
import net.flamgop.util.ResourceHelper;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL46.*;

public class ClusteredShading {
    public static final int GRID_SIZE_X = 12;
    public static final int GRID_SIZE_Y = 12;
    public static final int GRID_SIZE_Z = 24;
    private static final int NUM_CLUSTERS = GRID_SIZE_X * GRID_SIZE_Y * GRID_SIZE_Z;

    public static class Cluster implements BufferSerializable {

        static final int BYTES = 4 + 4 * Float.BYTES + Integer.BYTES + 128 * Integer.BYTES + 3 * Integer.BYTES;

        Vector4f minPoint; // 0
        Vector4f maxPoint; // 16
        int count; // 32
        final int[] lightIndices = new int[128]; // 36
        int[] _pad = new int[3]; // 548
        // size = 560

        @Override
        public void encode(ByteBuffer buf) {
            int idx = 0;
            minPoint.get(idx, buf);
            idx += 4 * Float.BYTES;
            maxPoint.get(idx, buf);
            idx += 4 * Float.BYTES;
            buf.putInt(idx, count);
            idx += Integer.BYTES;
            for (final int lightIndex : lightIndices) {
                buf.putInt(idx, lightIndex);
                idx += Integer.BYTES;
            }
            for (final int _ : _pad) {
                buf.putInt(idx, 0);
                idx += Integer.BYTES;
            }
        }

        @Override
        public int length() {
            return 4 + 4 * Float.BYTES + Integer.BYTES + lightIndices.length * Integer.BYTES + _pad.length * Integer.BYTES;
        }
    }

    private final ShaderProgram gatherClustersProgram;
    private final ShaderProgram cullLightsProgram;
    private final ShaderStorageBuffer clusterGridSSBO;

    private final ShaderStorageBuffer lightSSBO;

    private final Query gatherQuery;
    private final Query cullQuery;

    private long gatherTimeNs;
    private long cullTimeNs;

    public ClusteredShading(ShaderStorageBuffer lightSSBO) {
        gatherClustersProgram = new ShaderProgram();
        gatherClustersProgram.attachShaderSource("Gather Clusters Compute Shader", ResourceHelper.loadFileContentsFromResource("shaders/gather_clusters.compute.glsl"), ShaderProgram.ShaderType.COMPUTE);
        gatherClustersProgram.link();
        gatherClustersProgram.label("Gather Clusters Program");

        cullLightsProgram = new ShaderProgram();
        cullLightsProgram.attachShaderSource("Cull Lights Compute Shader", ResourceHelper.loadFileContentsFromResource("shaders/cull_lights.compute.glsl"), ShaderProgram.ShaderType.COMPUTE);
        cullLightsProgram.link();
        cullLightsProgram.label("Cull Lights Program");

        clusterGridSSBO = new ShaderStorageBuffer(GPUBuffer.BufferUsage.STATIC_COPY);
        clusterGridSSBO.buffer().allocate(Cluster.BYTES * NUM_CLUSTERS);
        clusterGridSSBO.buffer().label("Cluster Grid SSBO");

        this.lightSSBO = lightSSBO;

        this.gatherQuery = new Query(Query.QueryTarget.TIME_ELAPSED);
        this.cullQuery = new Query(Query.QueryTarget.TIME_ELAPSED);
    }

    public long gatherTimeNs() {
        if (gatherQuery.isResultAvailable()) {
            gatherTimeNs = gatherQuery.getResult64();
        }
        return gatherTimeNs;
    }

    public long cullTimeNs() {
        if (cullQuery.isResultAvailable()) {
            cullTimeNs = cullQuery.getResult64();
        }
        return cullTimeNs;
    }

    public ShaderStorageBuffer clusterGridSSBO() {
        return clusterGridSSBO;
    }

    public void compute(Camera camera) {
        glPushDebugGroup(GL_DEBUG_SOURCE_APPLICATION, 0, "Clustered Shading Compute");

        int width = Game.INSTANCE.window().width();
        int height = Game.INSTANCE.window().height();

        try (Query.QueryEnder _ = gatherQuery.begin()) {
            gatherClustersProgram.use();
            clusterGridSSBO.bind(1);
            gatherClustersProgram.uniform1f(gatherClustersProgram.getUniformLocation("zNear"), camera.near());
            gatherClustersProgram.uniform1f(gatherClustersProgram.getUniformLocation("zFar"), camera.far());
            gatherClustersProgram.uniformMatrix4fv(gatherClustersProgram.getUniformLocation("inverseProjection"), false, camera.projection().invert(new Matrix4f()));
            gatherClustersProgram.uniform3ui(gatherClustersProgram.getUniformLocation("gridSize"), GRID_SIZE_X, GRID_SIZE_Y, GRID_SIZE_Z);
            gatherClustersProgram.uniform2ui(gatherClustersProgram.getUniformLocation("screenDimensions"), width, height);

            glDispatchCompute(GRID_SIZE_X, GRID_SIZE_Y, GRID_SIZE_Z);
            glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
        }
        try (Query.QueryEnder _ = cullQuery.begin()) {
            cullLightsProgram.use();
            clusterGridSSBO.bind(1);
            lightSSBO.bind(2);
            cullLightsProgram.uniformMatrix4fv(cullLightsProgram.getUniformLocation("viewMatrix"), false, camera.view());

            glDispatchCompute(27, 1, 1);
            glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
        }

        glPopDebugGroup();
    }
}
