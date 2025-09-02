package net.flamgop.entity;

import net.flamgop.asset.AssetManager;

public abstract class AbstractComponent {
    private Transform transform;

    public Transform transform() {
        return transform;
    }

    public void transform(Transform transform) {
        this.transform = transform;
    }

    public abstract void load(AssetManager assetManager);
    public abstract void unload(AssetManager assetManager);
    public abstract void update(float delta);
    public abstract void physicsUpdate(float fixedDelta); // sort of a fixed update
    public abstract void render();
}
