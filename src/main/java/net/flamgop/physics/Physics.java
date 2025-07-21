package net.flamgop.physics;

import physx.PxTopLevelFunctions;
import physx.common.*;
import physx.physics.PxPhysics;
import physx.physics.PxScene;
import physx.physics.PxSceneDesc;

public class Physics {

    private final PxDefaultAllocator allocator = new PxDefaultAllocator();
    private final PxDefaultErrorCallback errorCallback = new PxDefaultErrorCallback();
    private final PxFoundation foundation;

    private final PxTolerancesScale tolerances = new PxTolerancesScale();
    private final PxPhysics physics;
    private final PxDefaultCpuDispatcher cpuDispatcher;

    public Physics(int threads) {
        int version = PxTopLevelFunctions.getPHYSICS_VERSION();
        int versionMajor = version >> 24;
        int versionMinor = (version >> 16) & 0xff;
        int versionMicro = (version >> 8) & 0xff;
        System.out.printf("PhysX loaded, version: %d.%d.%d\n", versionMajor, versionMinor, versionMicro);

        this.foundation = PxTopLevelFunctions.CreateFoundation(version, allocator, errorCallback);

        this.physics = PxTopLevelFunctions.CreatePhysics(version, foundation, tolerances);

        this.cpuDispatcher = PxTopLevelFunctions.DefaultCpuDispatcherCreate(threads);
    }

    public PxDefaultCpuDispatcher cpuDispatcher() {
        return cpuDispatcher;
    }

    public PxPhysics physics() {
        return physics;
    }

    public PxFoundation foundation() {
        return foundation;
    }

    public PxTolerancesScale tolerances() {
        return tolerances;
    }

    public PxScene createScene(PxSceneDesc sceneDesc) {
        PxScene scene = this.physics.createScene(sceneDesc);
        sceneDesc.destroy();
        return scene;
    }

    public void destroy() {
        cpuDispatcher.destroy();
        physics.release();
        tolerances.destroy();
        foundation.release();
        errorCallback.destroy();
        allocator.destroy();
    }
}
