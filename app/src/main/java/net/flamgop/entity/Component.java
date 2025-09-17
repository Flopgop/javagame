package net.flamgop.entity;

import net.flamgop.asset.AssetManager;

public class Component extends AbstractComponent {
    @Override public void load(AssetManager assetManager) {}
    @Override public void unload(AssetManager assetManager) {}
    @Override public void update(float delta) {}
    @Override public void physicsUpdate(float fixedDelta) {}
    @Override public void render() {}
}
