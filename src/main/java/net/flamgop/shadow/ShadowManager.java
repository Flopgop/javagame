package net.flamgop.shadow;

import net.flamgop.gpu.Camera;
import net.flamgop.gpu.ShaderProgram;
import net.flamgop.gpu.model.Material;
import net.flamgop.gpu.model.TexturedMesh;
import net.flamgop.util.AABB;
import net.flamgop.util.FrustumPlane;
import net.flamgop.util.ResourceHelper;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.opengl.GL46.*;

public class ShadowManager {

    private final Material depthOnlyMaterial;
    private final ShaderProgram depthOnlyShaderProgram;

    public ShadowManager() {
        depthOnlyShaderProgram = new ShaderProgram();
        depthOnlyShaderProgram.attachShaderSource("Depth Only Vertex Shader", ResourceHelper.loadFileContentsFromResource("shadow.vertex.glsl"), GL_VERTEX_SHADER);
        depthOnlyShaderProgram.link();
        depthOnlyShaderProgram.label("Depth Only Program");
        depthOnlyMaterial = new Material(depthOnlyShaderProgram);
    }

    public void prepareShadowPass() {
        TexturedMesh.overrideMaterial(depthOnlyMaterial);
    }

    public void bind(Matrix4f matrix, ShaderProgram shaderProgram) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (shaderProgram == null) shaderProgram = depthOnlyShaderProgram;
            glProgramUniformMatrix4fv(shaderProgram.handle(), shaderProgram.getUniformLocation("shadow_view_proj"), false, matrix.get(stack.callocFloat(16)));
        }
    }

    public void finishShadowPass() {
        TexturedMesh.disableMaterialOverride();
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

    public static FrustumPlane[] getShadowFrustumPlanes(Camera camera, DirectionalLight light, int resolution) {
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
