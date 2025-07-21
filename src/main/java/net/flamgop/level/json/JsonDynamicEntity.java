package net.flamgop.level.json;

public class JsonDynamicEntity {
    public String identifier;
    public String modelIdentifier;
    public float[] position; // Vector3f
    public float[] rotation; // Quaternionf
    public String collisionModelIdentifier;
    public float mass;
    public int collidesWithFlag;
    public int collisionGroup;

    public JsonDynamicEntity() {}
}
