package net.flamgop.math;

public class MathHelper {
    public static float lerp(float current, float target, float deltaTime, float speed) {
        return current + (target - current) * (1 - (float) Math.exp(-speed * deltaTime));
    }

    public static float conditionalLerp(float current, float target, float deltaTime, float speed) {
        return conditionalLerp(current, target, deltaTime, speed, 0.001f);
    }

    public static float conditionalLerp(float current, float target, float deltaTime, float speed, float epsilon) {
        if (Math.abs(current - target) > epsilon) {
            current = lerp(current, target, deltaTime, speed);
            if (Math.abs(current - target) <= epsilon) {
                current = target;
            }
        } else {
            current = target;
        }
        return current;
    }
}
