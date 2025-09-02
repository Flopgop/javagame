package net.flamgop.entity.components;

import net.flamgop.asset.AssetManager;
import net.flamgop.entity.AbstractComponent;
import net.flamgop.shadow.DirectionalLight;

public class SkyLightComponent extends AbstractComponent {

    private final DirectionalLight light;

    public SkyLightComponent(DirectionalLight light) {
        this.light = light;
    }

    public DirectionalLight skylight() {
        return light;
    }

    @Override public void load(AssetManager assetManager) {}
    @Override public void unload(AssetManager assetManager) {}
    @Override public void update(float delta) {}
    @Override public void physicsUpdate(float fixedDelta) {}
    @Override public void render() {}
}
