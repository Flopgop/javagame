package net.flamgop.level;

import net.flamgop.asset.AssetLoader;
import net.flamgop.gpu.buffer.GPUBuffer;
import net.flamgop.gpu.buffer.UniformBuffer;
import net.flamgop.gpu.model.Model;
import net.flamgop.gpu.uniform.ModelUniformData;
import net.flamgop.level.json.JsonDynamicEntity;
import net.flamgop.physics.Physics;
import net.flamgop.physics.PhysicsScene;
import net.flamgop.util.PhysxJoml;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import physx.common.PxIDENTITYEnum;
import physx.common.PxQuat;
import physx.common.PxTransform;
import physx.common.PxVec3;
import physx.geometry.PxConvexMesh;
import physx.geometry.PxConvexMeshGeometry;
import physx.physics.*;

import java.io.FileNotFoundException;

public class DynamicEntity {

    public static DynamicEntity fromJson(AssetLoader assetLoader, Physics physics, JsonDynamicEntity entity) {
        return new DynamicEntity(
                assetLoader,
                physics,
                entity.identifier,
                entity.modelIdentifier,
                new Vector3f(entity.position[0], entity.position[1], entity.position[2]),
                new Quaternionf(entity.rotation[0], entity.rotation[1], entity.rotation[2], entity.rotation[3]).normalize(),
                entity.collisionModelIdentifier,
                "",
                entity.mass,
                entity.collidesWithFlag,
                entity.collisionGroup
        );
    }

    private final PxShape shape;
    private final PxRigidDynamic actor;
    private final PxConvexMesh collisionMesh;
    private final PxConvexMeshGeometry collisionMeshGeometry;

    private final Model model;
    private final ModelUniformData modelUniformData = new ModelUniformData();
    private final UniformBuffer modelUniformBuffer;

    public DynamicEntity(
            AssetLoader assetLoader,
            Physics physics,
            String identifier,
            String modelIdentifier,
            Vector3f position,
            Quaternionf rotation,
            String collisionModelIdentifier,
            String materialIdentifier,
            float mass,
            int collidesWithFlag,
            int collisionGroup
    ) {
        try {
            this.collisionMesh = physics.loadConvexMesh(assetLoader.load(collisionModelIdentifier));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        collisionMeshGeometry = new PxConvexMeshGeometry(collisionMesh);
        PxShapeFlags shapeFlags = new PxShapeFlags((byte)  (PxShapeFlagEnum.eSCENE_QUERY_SHAPE.value | PxShapeFlagEnum.eSIMULATION_SHAPE.value));
        PxTransform transform = new PxTransform(PxIDENTITYEnum.PxIdentity);
        PxFilterData filterData = new PxFilterData(collisionGroup, collidesWithFlag, 0, 0);
        PxMaterial material = physics.physics().createMaterial(1.0f, 0.7f, 0f);
        this.shape = physics.physics().createShape(collisionMeshGeometry, material, true, shapeFlags);
        shape.setSimulationFilterData(filterData);
        shape.setQueryFilterData(filterData);

        transform.setP(PhysxJoml.toPxVec3(position));
        transform.setQ(PhysxJoml.toPxQuat(rotation));

        this.actor = physics.physics().createRigidDynamic(transform);
        actor.setMass(mass);
        actor.attachShape(shape);

        shapeFlags.destroy();
        transform.destroy();
        filterData.destroy();

        this.model = new Model();
        try {
            this.model.load(assetLoader, modelIdentifier);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        this.modelUniformBuffer = new UniformBuffer(GPUBuffer.UpdateHint.STREAM);
        this.modelUniformBuffer.allocate(this.modelUniformData);
        this.modelUniformBuffer.buffer().label(identifier + " Model UBO");
    }

    public void addToScene(PhysicsScene scene) {
        scene.addActor(this.actor);
    }

    public void render(double delta) {
        PxTransform transform = actor.getGlobalPose();
        PxVec3 pos = transform.getP();
        PxQuat rot = transform.getQ();
        modelUniformData.model.identity()
                .translate(pos.getX(), pos.getY(), pos.getZ())
                .rotate(new Quaternionf(rot.getX(), rot.getY(), rot.getZ(), rot.getW()));
        modelUniformBuffer.store(modelUniformData);

        modelUniformBuffer.bind(1);
        this.model.draw();
    }

    public void update(double delta) {}
}
