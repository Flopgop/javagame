package net.flamgop.physics;

import net.flamgop.Player;
import physx.PxTopLevelFunctions;
import physx.character.PxControllerManager;
import physx.common.PxVec3;
import physx.physics.PxActor;
import physx.physics.PxScene;

public class PhysicsScene {
    private final PxScene scene;
    private final PxControllerManager controllerManager;

    private final double fixedDeltaTime;
    private final int maxSubstepsPerFrame;
    private double accumulator = 0;

    private float gravity = -9.81f;
    private final PxVec3 tmpVec = new PxVec3(0.0f, gravity, 0.0f);

    public PhysicsScene(PxScene scene, double fixedDeltaTime, int maxSubstepsPerFrame) {
        this.scene = scene;
        this.fixedDeltaTime = fixedDeltaTime;
        this.maxSubstepsPerFrame = maxSubstepsPerFrame;

        this.controllerManager = PxTopLevelFunctions.CreateControllerManager(scene);
    }

    public PxControllerManager controllerManager() {
        return controllerManager;
    }

    public PxScene handle() {
        return scene;
    }

    public void addActor(PxActor actor) {
        scene.addActor(actor);
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

    public void fixedUpdate(Player player, double delta) {
        accumulator += delta;

        int steps = 0;
        while (accumulator >= fixedDeltaTime && steps < maxSubstepsPerFrame) {
            scene.simulate((float) fixedDeltaTime);
            scene.fetchResults(true);
            player.fixedUpdate(player.scene().fixedDeltaTime());

            accumulator -= fixedDeltaTime;
            steps++;
        }

        if (steps >= maxSubstepsPerFrame && accumulator >= fixedDeltaTime) {
            System.err.printf("Physics running ~%.2f steps behind!\n", accumulator / fixedDeltaTime);
            accumulator = 0.0;
        }
    }
}
