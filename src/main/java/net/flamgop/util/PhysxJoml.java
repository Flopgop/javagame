package net.flamgop.util;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import physx.common.PxQuat;
import physx.common.PxVec3;

public class PhysxJoml {
    public static Vector3f toVector3f(PxVec3 vec) {
        return new Vector3f(vec.getX(), vec.getY(), vec.getZ());
    }

    public static PxVec3 toPxVec3(Vector3f vec) {
        return new PxVec3(vec.x(), vec.y(), vec.z());
    }

    public static Quaternionf toQuaternionf(PxQuat quat) {
        return new Quaternionf(quat.getX(), quat.getY(), quat.getZ(), quat.getW());
    }

    public static PxQuat toPxQuat(Quaternionf quat) {
        return new PxQuat(quat.x, quat.y, quat.z, quat.w);
    }
}
