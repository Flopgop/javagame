package net.flamgop.entity;

import net.flamgop.asset.AssetManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Scene {
    private final List<Entity> allEntities = new ArrayList<>();
    private final List<Entity> rootEntities = new ArrayList<>();

    public void addRootEntity(Entity entity) {
        rootEntities.add(entity);
        allEntities.add(entity);
    }

    public @Nullable Entity getByUUID(UUID uuid) {
        return allEntities.stream().filter(e -> e.id().equals(uuid)).findFirst().orElse(null);
    }

    public List<Entity> allEntities() {
        return Collections.unmodifiableList(allEntities);
    }

    public List<Entity> rootEntities() {
        return Collections.unmodifiableList(rootEntities);
    }

    public void load(AssetManager assetManager) {
        allEntities.forEach(e -> e.components().forEach(c -> c.load(assetManager)));
    }

    public void unload(AssetManager assetManager) {
        allEntities.forEach(e -> e.components().forEach(c -> c.unload(assetManager)));
    }

    public void update(float delta) {
        allEntities.forEach(e -> e.components().forEach(c -> c.update(delta)));
    }

    public void fixedUpdate(float fixedDelta) {
        allEntities.forEach(e -> e.components().forEach(c -> c.physicsUpdate(fixedDelta)));
    }

    public void render(float delta) {
        allEntities.forEach(e -> e.components().forEach(AbstractComponent::render));
    }
}
