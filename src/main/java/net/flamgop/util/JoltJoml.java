package net.flamgop.util;

import com.github.stephengold.joltjni.Quat;
import com.github.stephengold.joltjni.RVec3;
import com.github.stephengold.joltjni.readonly.QuatArg;
import com.github.stephengold.joltjni.readonly.RVec3Arg;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class JoltJoml {
    public static RVec3Arg toRVec3Arg(Vector3f vector3f) {
        return new RVec3(vector3f.x, vector3f.y, vector3f.z);
    }
    public static QuatArg toQuatArg(Quaternionf quaternionf) {
        return new Quat(quaternionf.x, quaternionf.y, quaternionf.z, quaternionf.w);
    }
    public static Vector3f toVector3f(RVec3Arg vec3) {
        return new Vector3f(vec3.x(), vec3.y(), vec3.z());
    }
    public static Quaternionf toQuaternionf(QuatArg quat) {
        return new Quaternionf(quat.getX(), quat.getY(), quat.getZ(), quat.getW());
    }
}
