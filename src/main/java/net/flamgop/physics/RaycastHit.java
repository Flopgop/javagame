package net.flamgop.physics;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 *
 * @param hit whether this raycast hit actually hit
 * @param data the data of the hit, or null if there was no hit.
 */
public record RaycastHit(boolean hit, @Nullable RaycastData data) {
    public record RaycastData(Object actor, Vector3f position, Vector3f normal) {

    }
}
