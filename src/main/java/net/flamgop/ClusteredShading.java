package net.flamgop;

import net.flamgop.gpu.Camera;
import net.flamgop.gpu.ShaderProgram;
import net.flamgop.gpu.buffer.BufferSerializable;
import net.flamgop.gpu.buffer.GPUBuffer;
import net.flamgop.gpu.buffer.ShaderStorageBuffer;
import net.flamgop.level.Level;
import net.flamgop.util.ResourceHelper;
import net.flamgop.util.Util;
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

    private final Level level;

    private final int gatherQuery;
    private final int cullQuery;

    private long gatherTimeNs;
    private long cullTimeNs;

    public ClusteredShading(Level level) {
        gatherClustersProgram = new ShaderProgram();
        gatherClustersProgram.attachShaderSource("Gather Clusters Compute Shader", ResourceHelper.loadFileContentsFromResource("gather_clusters.compute.glsl"), GL_COMPUTE_SHADER);
        gatherClustersProgram.link();
        gatherClustersProgram.label("Gather Clusters Program");

        cullLightsProgram = new ShaderProgram();
        cullLightsProgram.attachShaderSource("Cull Lights Compute Shader", ResourceHelper.loadFileContentsFromResource("cull_lights.compute.glsl"), GL_COMPUTE_SHADER);
        cullLightsProgram.link();
        cullLightsProgram.label("Cull Lights Program");

        clusterGridSSBO = new ShaderStorageBuffer(GPUBuffer.BufferUsage.STATIC_COPY);
        clusterGridSSBO.buffer().allocate(Cluster.BYTES * NUM_CLUSTERS);

        this.level = level;

        this.gatherQuery = glCreateQueries(GL_TIME_ELAPSED);
        this.cullQuery = glCreateQueries(GL_TIME_ELAPSED);
    }

    public long gatherTimeNs() {
        if (Util.isQueryReady(gatherQuery)) {
            gatherTimeNs = Util.getQueryTime(gatherQuery);
        }
        return gatherTimeNs;
    }

    public long cullTimeNs() {
        if (Util.isQueryReady(cullQuery)) {
            cullTimeNs = Util.getQueryTime(cullQuery);
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

        glBeginQuery(GL_TIME_ELAPSED, this.gatherQuery);
        gatherClustersProgram.use();
        clusterGridSSBO.bind(1);
        glProgramUniform1f(gatherClustersProgram.handle(), gatherClustersProgram.getUniformLocation("zNear"), camera.near());
        glProgramUniform1f(gatherClustersProgram.handle(), gatherClustersProgram.getUniformLocation("zFar"), camera.far());
        glProgramUniformMatrix4fv(gatherClustersProgram.handle(), gatherClustersProgram.getUniformLocation("inverseProjection"), false, camera.projection().invert(new Matrix4f()).get(new float[16]));
        glProgramUniform3ui(gatherClustersProgram.handle(), gatherClustersProgram.getUniformLocation("gridSize"), GRID_SIZE_X, GRID_SIZE_Y, GRID_SIZE_Z);
        glProgramUniform2ui(gatherClustersProgram.handle(), gatherClustersProgram.getUniformLocation("screenDimensions"), width, height);

        glDispatchCompute(GRID_SIZE_X, GRID_SIZE_Y, GRID_SIZE_Z);
        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
        glEndQuery(GL_TIME_ELAPSED);
        glBeginQuery(GL_TIME_ELAPSED, this.cullQuery);

        cullLightsProgram.use();
        clusterGridSSBO.bind(1);
        level.lightSSBO().bind(2);
        glProgramUniformMatrix4fv(cullLightsProgram.handle(), cullLightsProgram.getUniformLocation("viewMatrix"), false, camera.view().get(new float[16]));

        glDispatchCompute(27, 1, 1);
        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
        glEndQuery(GL_TIME_ELAPSED);

        glPopDebugGroup();
    }
}
