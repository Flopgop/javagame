package net.flamgop.shadow;

import net.flamgop.Game;
import net.flamgop.gpu.Camera;
import net.flamgop.gpu.GPUFramebuffer;
import net.flamgop.gpu.GPUTexture;
import net.flamgop.gpu.ShaderProgram;
import net.flamgop.gpu.model.Material;
import net.flamgop.gpu.model.TexturedMesh;
import net.flamgop.util.AABB;
import net.flamgop.util.FrustumPlane;
import net.flamgop.util.ResourceHelper;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.Arrays;

import static org.lwjgl.opengl.GL46.*;

public class ShadowManager {

    private final Material depthOnlyMaterial;
    private final ShaderProgram shadowShaderProgram;

    private final Camera camera;

    private final float lambda = 0.6f;

    private final int cascadeCount;
    private final int largestCascadeResolution;
    private final GPUFramebuffer shadowFramebuffer;
    private GPUTexture cascades;

    public ShadowManager(Camera camera, int resolution, int cascadeCount) {
        this.camera = camera;
        this.cascadeCount = cascadeCount;
        this.largestCascadeResolution = resolution;
        shadowShaderProgram = new ShaderProgram();
        shadowShaderProgram.attachShaderSource("Shadow Vertex Shader", ResourceHelper.loadFileContentsFromResource("shaders/shadow.vertex.glsl"), GL_VERTEX_SHADER);
        shadowShaderProgram.attachShaderSource("Shadow Geometry Shader", ResourceHelper.loadFileContentsFromResource("shaders/shadow.geometry.glsl"), GL_GEOMETRY_SHADER);
        shadowShaderProgram.link();
        shadowShaderProgram.label("Shadow Program");
        depthOnlyMaterial = new Material(shadowShaderProgram);

        shadowFramebuffer = new GPUFramebuffer(resolution, resolution, (fb, w, h) -> {
            cascades = new GPUTexture(GPUTexture.TextureTarget.TEXTURE_2D_ARRAY); // this is cursed lmao
            cascades.storage(1, GL_DEPTH_COMPONENT32F, w, h, cascadeCount);
            glTextureParameteri(cascades.handle(), GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTextureParameteri(cascades.handle(), GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTextureParameteri(cascades.handle(), GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
            glTextureParameteri(cascades.handle(), GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);

            cascades.label("Shadow Depth Texture Array");

            fb.texture(cascades, GL_DEPTH_ATTACHMENT, 0);
        }, (fb) -> {
            cascades.destroy();
        });
        shadowFramebuffer.label("Shadow Framebuffer");
    }

    public int cascades() {
        return this.cascadeCount;
    }

    public float lambda() {
        return this.lambda;
    }

    public GPUTexture texture() {
        return cascades;
    }

    public void prepareShadowPass() {
        TexturedMesh.overrideMaterial(depthOnlyMaterial);
    }

    public void bindFramebuffer() {
        glViewport(0,0, this.largestCascadeResolution, this.largestCascadeResolution);
        shadowFramebuffer.clear(GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
    }

    public void bindUniforms(ShaderProgram shaderProgram, Matrix4f lightView) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (shaderProgram == null) shaderProgram = shadowShaderProgram;
            FloatBuffer matrixBuffer = stack.callocFloat(16);
            float[] splits = CascadedShadowMaps.computeCascadeSplits(cascadeCount, camera.near(), camera.far(), lambda);
            Matrix4f[] cascades = CascadedShadowMaps.computeCascadeMatrices(camera, lightView, this.cascadeCount, this.largestCascadeResolution, splits);
            for (int i = 0; i < cascadeCount; i++) {
                glProgramUniform1f(shaderProgram.handle(), shaderProgram.getUniformLocation("cascade_distances[" + i + "]"), splits[i + 1]);
                glProgramUniformMatrix4fv(shaderProgram.handle(), shaderProgram.getUniformLocation("cascade_matrices[" + i + "]"), false, cascades[i].get(matrixBuffer));
                matrixBuffer.clear();
            }
        }
    }

    public void finishShadowPass() {
        TexturedMesh.disableMaterialOverride();
    }

    public static Matrix4f getShadowView(DirectionalLight light) {
        return new Matrix4f()
                .lookAt(new Vector3f(new Vector3f(light.direction).normalize()).mul(-100), new Vector3f(0,0,0), new Vector3f(0,1,0));
    }

    public static Matrix4f getShadowMatrix(Camera camera, DirectionalLight light, int resolution) {
        Vector3f lightDir = new Vector3f(light.direction).normalize();
        Matrix4f lightView = new Matrix4f()
                .lookAt(new Vector3f(lightDir).mul(-100), new Vector3f(0,0,0), new Vector3f(0,1,0));

        Vector3f orthoMin = new Vector3f(), orthoMax = new Vector3f();
        Matrix4f invCamViewProj = new Matrix4f(camera.projection()).mul(camera.view()).invert();
        ShadowUtil.computeOrthoExtents(lightView, invCamViewProj, orthoMin, orthoMax);
        Vector3f min = new Vector3f(), max = new Vector3f();
        invCamViewProj.frustumAabb(min, max);
        Vector2f nearAndFar = ShadowUtil.computeNearAndFar(lightView, orthoMin, orthoMax, new AABB(min, max));

        Matrix4f proj = new Matrix4f().ortho(orthoMin.x, orthoMax.x, orthoMin.y, orthoMax.y, nearAndFar.x, nearAndFar.y);

        return new Matrix4f(proj).mul(lightView);
    }

    public static FrustumPlane[] getShadowFrustumPlanes(Camera camera, DirectionalLight light, int resolution, int cascade) {
        Matrix4f viewProj = getShadowMatrix(camera, light, resolution);

        float m00 = viewProj.m00(), m01 = viewProj.m01(), m02 = viewProj.m02(), m03 = viewProj.m03();
        float m10 = viewProj.m10(), m11 = viewProj.m11(), m12 = viewProj.m12(), m13 = viewProj.m13();
        float m20 = viewProj.m20(), m21 = viewProj.m21(), m22 = viewProj.m22(), m23 = viewProj.m23();
        float m30 = viewProj.m30(), m31 = viewProj.m31(), m32 = viewProj.m32(), m33 = viewProj.m33();

        return new FrustumPlane[]{
                new FrustumPlane(new Vector3f(m03 + m00, m13 + m10, m23 + m20), m33 + m30).normalized(), // Left
                new FrustumPlane(new Vector3f(m03 - m00, m13 - m10, m23 - m20), m33 - m30).normalized(), // Right
                new FrustumPlane(new Vector3f(m03 + m01, m13 + m11, m23 + m21), m33 + m31).normalized(), // Bottom
                new FrustumPlane(new Vector3f(m03 - m01, m13 - m11, m23 - m21), m33 - m31).normalized(), // Top
                new FrustumPlane(new Vector3f(m03 + m02, m13 + m12, m23 + m22), m33 + m32).normalized(), // Near
                new FrustumPlane(new Vector3f(m03 - m02, m13 - m12, m23 - m22), m33 - m32).normalized()  // Far
        };
    }


}
