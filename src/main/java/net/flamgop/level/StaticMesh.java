package net.flamgop.level;

import net.flamgop.asset.AssetIdentifier;
import net.flamgop.asset.AssetManager;
import net.flamgop.gpu.buffer.GPUBuffer;
import net.flamgop.gpu.buffer.UniformBuffer;
import net.flamgop.gpu.model.Model;
import net.flamgop.gpu.data.ModelUniformData;
import net.flamgop.level.json.JsonStaticMesh;
import net.flamgop.physics.Physics;
import net.flamgop.physics.PhysicsScene;
import net.flamgop.util.PhysxJoml;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import physx.common.PxIDENTITYEnum;
import physx.common.PxQuat;
import physx.common.PxTransform;
import physx.common.PxVec3;
import physx.geometry.PxTriangleMesh;
import physx.geometry.PxTriangleMeshGeometry;
import physx.physics.*;

import java.nio.ByteBuffer;

@SuppressWarnings("FieldCanBeLocal")
public class StaticMesh {
    public static StaticMesh fromJson(AssetManager assetManager, Physics physics, JsonStaticMesh entity) {
        return new StaticMesh(
                assetManager,
                physics,
                entity.identifier,
                entity.modelIdentifier,
                new Vector3f(entity.position[0], entity.position[1], entity.position[2]),
                new Quaternionf(entity.rotation[0], entity.rotation[1], entity.rotation[2], entity.rotation[3]).normalize(),
                entity.collisionModelIdentifier,
                "",
                entity.collidesWithFlag,
                entity.collisionGroup
        );
    }

    private final PxShape shape;
    private final PxRigidStatic actor;
    private final PxTriangleMesh collisionMesh;
    private final PxTriangleMeshGeometry collisionMeshGeometry;

    private final boolean hasModel;
    private final Model model;
    private final ModelUniformData modelUniformData;
    private final UniformBuffer modelUniformBuffer;

    public StaticMesh(
            AssetManager assetManager,
            Physics physics,
            String identifier,
            String modelIdentifier,
            Vector3f position,
            Quaternionf rotation,
            String collisionModelIdentifier,
            String materialIdentifier,
            int collidesWithFlag,
            int collisionGroup
    ) {
        this.collisionMesh = physics.loadMesh(assetManager.loadSync(new AssetIdentifier(collisionModelIdentifier), ByteBuffer.class).get());
        collisionMeshGeometry = new PxTriangleMeshGeometry(collisionMesh);
        PxShapeFlags shapeFlags = new PxShapeFlags((byte)  (PxShapeFlagEnum.eSCENE_QUERY_SHAPE.value | PxShapeFlagEnum.eSIMULATION_SHAPE.value | PxShapeFlagEnum.eVISUALIZATION.value));
        PxTransform transform = new PxTransform(PxIDENTITYEnum.PxIdentity);
        PxFilterData filterData = new PxFilterData(collisionGroup, collidesWithFlag, 0, 0);
        PxMaterial material = physics.physics().createMaterial(1.0f, 0.7f, 0f);
        this.shape = physics.physics().createShape(collisionMeshGeometry, material, true, shapeFlags);
        shape.setSimulationFilterData(filterData);
        shape.setQueryFilterData(filterData);

        transform.setP(PhysxJoml.toPxVec3(position));
        transform.setQ(PhysxJoml.toPxQuat(rotation));

        this.actor = physics.physics().createRigidStatic(transform);
        actor.attachShape(shape);

        shapeFlags.destroy();
        transform.destroy();
        filterData.destroy();

        if (modelIdentifier != null) {
            this.hasModel = true;
            this.model = assetManager.loadSync(new AssetIdentifier(modelIdentifier), Model.class).get();
            this.modelUniformData = new ModelUniformData();
            this.modelUniformBuffer = new UniformBuffer(GPUBuffer.UpdateHint.STREAM);
            this.modelUniformBuffer.allocate(this.modelUniformData);
            this.modelUniformBuffer.buffer().label(identifier + " Model UBO");
        } else {
            this.hasModel = false;
            this.model = null;
            this.modelUniformData = null;
            this.modelUniformBuffer = null;
        }
    }

    public Model model() {
        return model;
    }

    public void addToScene(PhysicsScene scene) {
        scene.addActor(actor);
    }

    public void render(double delta) {
        if (!hasModel) return;
        PxTransform transform = actor.getGlobalPose();
        PxVec3 pos = transform.getP();
        PxQuat rot = transform.getQ();
        modelUniformData.model.identity()
                .translate(pos.getX(), pos.getY(), pos.getZ())
                .rotate(new Quaternionf(rot.getX(), rot.getY(), rot.getZ(), rot.getW()));
        modelUniformData.computeNormal();
        modelUniformBuffer.store(modelUniformData);

        modelUniformBuffer.bind(1);
        this.model.draw(modelUniformData.model);
    }
}
