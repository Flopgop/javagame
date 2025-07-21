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

    private final PxScene scene;

    private float gravity = 2*-9.81f;
    private final PxVec3 tmpVec = new PxVec3(0.0f, gravity, 0.0f);

    private double accumulator = 0;
    private final double fixedDeltaTime = 1.0 / 60.0;
    private final int maxSubstepsPerFrame = 20;

    public Physics(int threads) {
        int version = PxTopLevelFunctions.getPHYSICS_VERSION();
        int versionMajor = version >> 24;
        int versionMinor = (version >> 16) & 0xff;
        int versionMicro = (version >> 8) & 0xff;
        System.out.printf("PhysX loaded, version: %d.%d.%d\n", versionMajor, versionMinor, versionMicro);

        this.foundation = PxTopLevelFunctions.CreateFoundation(version, allocator, errorCallback);

        this.physics = PxTopLevelFunctions.CreatePhysics(version, foundation, tolerances);

        this.cpuDispatcher = PxTopLevelFunctions.DefaultCpuDispatcherCreate(threads);

        PxSceneDesc sceneDesc = new PxSceneDesc(tolerances);
        sceneDesc.setGravity(tmpVec);
        sceneDesc.setCpuDispatcher(cpuDispatcher);
        sceneDesc.setFilterShader(PxTopLevelFunctions.DefaultFilterShader());
        this.scene = physics.createScene(sceneDesc);
    }

    public void update(double delta) {
        accumulator += delta;

        int steps = 0;
        while (accumulator >= fixedDeltaTime && steps < maxSubstepsPerFrame) {
            scene.simulate((float) fixedDeltaTime);
            scene.fetchResults(true);

            accumulator -= fixedDeltaTime;
            steps++;
        }

        if (steps >= maxSubstepsPerFrame && accumulator >= fixedDeltaTime) {
            System.err.printf("Physics running ~%.2f steps behind!\n", accumulator / fixedDeltaTime);
            accumulator = 0.0;
        }
    }

    public PxScene scene() {
        return scene;
    }

    public double fixedDeltaTime() {
        return fixedDeltaTime;
    }

    public float gravity() {
        return gravity;
    }

    public void gravity(float gravity) {
        this.gravity = gravity;
        tmpVec.setX(0);
        tmpVec.setY(gravity);
        tmpVec.setZ(0);
        scene.setGravity(tmpVec);
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
