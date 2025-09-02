package net.flamgop.entity.components;

import net.flamgop.asset.AssetManager;
import net.flamgop.entity.AbstractComponent;
import net.flamgop.physics.Physics;
import net.flamgop.physics.PhysicsScene;
import physx.common.PxVec3;

public class PhysxSceneComponent extends AbstractComponent {

    private final PhysicsScene scene;

    public PhysxSceneComponent(Physics physics) {
        PxVec3 temp = new PxVec3();
        temp.setX(0); temp.setY(2*-9.81f); temp.setZ(0);
        this.scene = physics.createScene(physics.defaultSceneDesc(temp));
    }

    public PhysicsScene scene() {
        return scene;
    }

    @Override
    public void load(AssetManager assetManager) {

    }

    @Override
    public void unload(AssetManager assetManager) {

    }

    @Override
    public void update(float delta) {

    }

    @Override
    public void physicsUpdate(float fixedDelta) {

    }

    @Override
    public void render() {

    }
}
