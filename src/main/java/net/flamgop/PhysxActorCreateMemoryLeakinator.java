package net.flamgop;

import net.flamgop.asset.AssetIdentifier;
import net.flamgop.asset.AssetManager;
import net.flamgop.physics.Physics;
import net.flamgop.util.PhysxJoml;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import physx.JavaNativeRef;
import physx.common.PxIDENTITYEnum;
import physx.common.PxTransform;
import physx.geometry.PxTriangleMesh;
import physx.geometry.PxTriangleMeshGeometry;
import physx.physics.*;

import java.nio.ByteBuffer;

public class PhysxActorCreateMemoryLeakinator {
    public static PxRigidActor giveMeASuperActor(Physics physics, AssetManager assetManager, AssetIdentifier mesh, int collidesWith, int group) {
        PxTriangleMesh collisionMesh = physics.loadMesh(assetManager.loadSync(mesh, ByteBuffer.class).get());
        PxTriangleMeshGeometry collisionMeshGeometry = new PxTriangleMeshGeometry(collisionMesh);
        PxShapeFlags shapeFlags = new PxShapeFlags((byte)  (PxShapeFlagEnum.eSCENE_QUERY_SHAPE.value | PxShapeFlagEnum.eSIMULATION_SHAPE.value | PxShapeFlagEnum.eVISUALIZATION.value));
        PxTransform transform = new PxTransform(PxIDENTITYEnum.PxIdentity);
        PxFilterData filterData = new PxFilterData(group, collidesWith, 0, 0);
        PxMaterial material = physics.physics().createMaterial(1.0f, 0.7f, 0f);
        PxShape shape = physics.physics().createShape(collisionMeshGeometry, material, true, shapeFlags);
        shape.setSimulationFilterData(filterData);
        shape.setQueryFilterData(filterData);

        transform.setP(PhysxJoml.toPxVec3(new Vector3f()));
        transform.setQ(PhysxJoml.toPxQuat(new Quaternionf().identity()));

        PxRigidActor actor = physics.physics().createRigidStatic(transform);
        actor.attachShape(shape);
        return actor;
    }
}
