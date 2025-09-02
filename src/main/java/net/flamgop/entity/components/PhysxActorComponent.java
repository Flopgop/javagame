package net.flamgop.entity.components;

import net.flamgop.asset.AssetManager;
import net.flamgop.entity.AbstractComponent;
import net.flamgop.entity.Entity;
import net.flamgop.util.PhysxJoml;
import physx.JavaNativeRef;
import physx.common.PxTransform;
import physx.physics.PxRigidActor;

public class PhysxActorComponent extends AbstractComponent {

    private final PxRigidActor actor;
    private boolean kinematic;

    public PhysxActorComponent(Entity self, PxRigidActor actor, boolean kinematic) {
        this.actor = actor;
        this.kinematic = kinematic;

        actor.setUserData(new JavaNativeRef<>(self));
    }

    @Override
    public void load(AssetManager assetManager) {

    }

    @Override
    public void unload(AssetManager assetManager) {

    }

    @Override
    public void update(float delta) {
        if (kinematic) {
            PxTransform physxTransform = new PxTransform(
                    PhysxJoml.toPxVec3(transform().position()),
                    PhysxJoml.toPxQuat(transform().rotation())
            );
            actor.setGlobalPose(physxTransform);
        } else {
            PxTransform physxTransform = actor.getGlobalPose();
            transform().position(PhysxJoml.toVector3f(physxTransform.getP()));
            transform().rotation(PhysxJoml.toQuaternionf(physxTransform.getQ()));
        }
    }

    @Override
    public void physicsUpdate(float fixedDelta) {

    }

    @Override
    public void render() {

    }

    public PxRigidActor actor() {
        return actor;
    }
}
