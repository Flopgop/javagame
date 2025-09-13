package net.flamgop.entity.components;

import net.flamgop.asset.AssetManager;
import net.flamgop.entity.AbstractComponent;
import net.flamgop.entity.Component;
import net.flamgop.shadow.DirectionalLight;

public class SkyLightComponent extends Component {

    private final DirectionalLight light;

    public SkyLightComponent(DirectionalLight light) {
        this.light = light;
    }

    public DirectionalLight skylight() {
        return light;
    }
}
