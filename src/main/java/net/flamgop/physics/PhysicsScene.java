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

    private float gravity = -9.81f;
    private final PxVec3 tmpVec = new PxVec3(0.0f, gravity, 0.0f);

    public PhysicsScene(PxScene scene) {
        this.scene = scene;

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

    public void fixedUpdate(double delta) {
        scene.simulate((float) delta);
        scene.fetchResults(true);
    }
}
