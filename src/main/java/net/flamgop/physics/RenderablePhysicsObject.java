package net.flamgop.physics;

import net.flamgop.gpu.VertexArray;
import net.flamgop.gpu.buffer.GPUBuffer;
import net.flamgop.gpu.buffer.UniformBuffer;
import net.flamgop.gpu.uniform.ModelUniformData;
import org.joml.Quaternionf;
import physx.common.PxQuat;
import physx.common.PxTransform;
import physx.common.PxVec3;
import physx.physics.PxRigidActor;

public class RenderablePhysicsObject {

    private final PxRigidActor actor;

    private final VertexArray mesh;
    private final ModelUniformData model = new ModelUniformData();
    private final UniformBuffer modelBuffer;

    public RenderablePhysicsObject(PxRigidActor actor, VertexArray mesh) {
        this.actor = actor;
        this.mesh = mesh;
        this.modelBuffer = new UniformBuffer(GPUBuffer.UpdateHint.DYNAMIC);
        this.modelBuffer.allocate(model);
    }

    public void update() {
        PxTransform transform = actor.getGlobalPose();
        PxVec3 pos = transform.getP();
        PxQuat rot = transform.getQ();
        model.model.identity()
                .translate(pos.getX(), pos.getY(), pos.getZ())
                .rotate(new Quaternionf(rot.getX(), rot.getY(), rot.getZ(), rot.getW()));
        modelBuffer.store(model);
    }

    public void render() {
        modelBuffer.bind(1);
        mesh.draw();
    }
}
