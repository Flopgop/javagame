package net.flamgop.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Entity {
    private final UUID id;
    private final Transform transform;

    private final List<AbstractComponent> components = new ArrayList<>();

    private boolean active;

    public Entity() {
        this(UUID.randomUUID());
    }

    public Entity(UUID id) {
        this.id = id;
        this.transform = new Transform(this);
        this.active = true;
    }

    public List<AbstractComponent> components() {
        return Collections.unmodifiableList(components);
    }

    public boolean active() {
        return active;
    }

    public void active(boolean active) {
        this.active = active;
    }

    public UUID id() {
        return id;
    }

    public Transform transform() {
        return transform;
    }

    public void addComponent(AbstractComponent c) {
        c.transform(this.transform);
        components.add(c);
    }

    public <T extends AbstractComponent> T getComponent(Class<T> type) {
        for (AbstractComponent c : components) {
            if (type.isInstance(c)) return type.cast(c);
        }
        return null;
    }

    public <T extends AbstractComponent> boolean hasComponent(Class<T> type) {
        for (AbstractComponent c : components) {
            if (type.isInstance(c)) return true;
        }
        return false;
    }
}
