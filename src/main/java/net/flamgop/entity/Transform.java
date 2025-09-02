package net.flamgop.entity;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Transform {
    private final Vector3f position = new Vector3f();
    private final Quaternionf rotation = new Quaternionf();

    private @Nullable Transform parent;
    private final List<Transform> children = new ArrayList<>();

    private boolean dirty = true;
    private final Matrix4f cachedLocal = new Matrix4f();
    private final Matrix4f cachedWorld = new Matrix4f();

    private final Entity entity;

    public Transform(Entity entity) {
        this.entity = entity;
    }

    public Entity entity() {
        return entity;
    }

    public void parent(@Nullable Transform parent) {
        if (parent != null) parent.children.remove(this);
        this.parent = parent;
        if (parent != null && !parent.children.contains(this)) parent.children.add(this);
    }

    public List<Transform> children() {
        return Collections.unmodifiableList(children);
    }

    public void translate(Vector3f delta) {
        position.add(delta);
        markDirty();
    }

    public void rotate(Quaternionf delta) {
        rotation.mul(delta);
        markDirty();
    }

    public void lookAt(Vector3f target, Vector3f up) {
        rotation.identity().lookAlong(target.sub(position, new Vector3f()).normalize(), up);
        markDirty();
    }

    public Vector3f position() {
        return new Vector3f(position);
    }
    public Quaternionf rotation() {
        return new Quaternionf(rotation);
    }

    public void position(Vector3f position) {
        this.position.set(position);
    }
    public void rotation(Quaternionf rotation) {
        this.rotation.set(rotation);
    }

    private void markDirty() {
        this.dirty = true;
        for (Transform child : children) {
            child.markDirty();
        }
    }

    public Matrix4f getLocalMatrix() {
        return new Matrix4f().translation(position).mul(new Matrix4f().rotation(rotation));
    }

    public Matrix4f getInverseWorldMatrix() {
        return getWorldMatrix().invert(new Matrix4f());
    }

    public boolean dirty() {
        return dirty;
    }

    public Matrix4f getWorldMatrix() {
        if (dirty) {
            cachedLocal.identity()
                    .translate(position)
                    .rotate(rotation);

            if (parent != null)
                cachedWorld.set(parent.getWorldMatrix()).mul(cachedLocal);
            else
                cachedWorld.set(cachedLocal);

            dirty = false;
        }
        return new Matrix4f(cachedWorld);
    }
}
