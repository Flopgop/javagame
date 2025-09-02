package net.flamgop.shadow;

import org.joml.Vector3f;

public class DirectionalLight {
    public Vector3f direction;
    public Vector3f color;

    public DirectionalLight(Vector3f direction, Vector3f color) {
        this.direction = direction;
        this.color = color;
    }

    public DirectionalLight() {}
}
