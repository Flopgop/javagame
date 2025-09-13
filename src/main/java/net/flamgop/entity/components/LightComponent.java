package net.flamgop.entity.components;

import net.flamgop.entity.Component;
import net.flamgop.gpu.data.Light;

public class LightComponent extends Component {

    private final Light light;
    public LightComponent(Light light) {
        this.light = light;
    }
    public Light light() {
        return light;
    }
}
