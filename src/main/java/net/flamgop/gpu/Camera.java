package net.flamgop.gpu;

import net.flamgop.gpu.buffer.GPUBuffer;
import net.flamgop.gpu.buffer.UniformBuffer;
import net.flamgop.gpu.uniform.CameraUniformData;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {
    private final CameraUniformData camera = new CameraUniformData();
    private final UniformBuffer cameraUniformBuffer;

    private final Vector3f position;
    private final Vector3f target;
    private final Vector3f up;

    private float fov;
    private float aspectRatio;
    private float near;
    private float far;

    public Camera(Vector3f position, Vector3f target, Vector3f up, float fov, float aspectRatio, float near, float far) {
        this.fov = fov;
        this.aspectRatio = aspectRatio;
        this.near = near;
        this.far = far;

        this.position = new Vector3f(position);
        this.target = target;
        this.up = up;

        camera.position = this.position;
        camera.view = new Matrix4f().lookAt(position, target, up);
        camera.projection = new Matrix4f().perspective(fov, aspectRatio, near, far);

        this.cameraUniformBuffer = new UniformBuffer(GPUBuffer.UpdateHint.DYNAMIC);
        this.cameraUniformBuffer.buffer().label("Camera UBO");
        this.cameraUniformBuffer.allocate(camera);
    }

    public void resize(int width, int height) {
        if ((float) width / height == aspectRatio) return;
        this.aspectRatio = (float) width / height;
        reconfigureProjection();
    }

    public void reconfigureProjection() {
        camera.projection.identity().perspective(fov, aspectRatio, near, far);
        this.upload();
    }

    public void reconfigureView() {
        camera.view.identity().lookAt(position, target, up);
        this.upload();
    }

    public void upload() {
        this.cameraUniformBuffer.store(camera);
    }

    public void bind(int index) {
        cameraUniformBuffer.bind(index);
    }

    public void fov(float fov) {
        this.fov = fov;
    }

    public void near(float near) {
        this.near = near;
    }

    public void far(float far) {
        this.far = far;
    }

    public float fov() {
        return this.fov;
    }

    public float near() {
        return this.near;
    }

    public float far() {
        return this.far;
    }

    public void position(Vector3f position) {
        this.position.set(position);
    }

    public void target(Vector3f target) {
        this.target.set(target);
    }

    public void up(Vector3f up) {
        this.up.set(up);
    }

    public Vector3f position() {
        return new Vector3f(this.position);
    }

    public Vector3f target() {
        return new Vector3f(this.target);
    }

    public Vector3f up() {
        return new Vector3f(this.up).normalize();
    }

    public Vector3f forward() {
        return target().sub(this.position()).normalize();
    }

    public Vector3f right() {
        return forward().cross(this.up()).normalize();
    }
}
