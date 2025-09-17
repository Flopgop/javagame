package net.flamgop.physics;

import com.github.stephengold.joltjni.ConvexHullShape;
import com.github.stephengold.joltjni.ConvexHullShapeSettings;
import com.github.stephengold.joltjni.ShapeRefC;
import com.github.stephengold.joltjni.ShapeResult;
import com.github.stephengold.joltjni.readonly.Vec3Arg;

import java.util.List;

public class PhysicsShape {

    private final ConvexHullShape shape;

    public PhysicsShape(List<Vec3Arg> points) {
        ConvexHullShapeSettings settings = new ConvexHullShapeSettings(points);
        ShapeResult result = settings.create();
        if (!result.isValid()) throw new IllegalArgumentException("Bad points, couldn't make a convex hull.");
        this.shape = (ConvexHullShape) result.get().getPtr();

        settings.close();
    }

    public ShapeRefC shape() {
        return shape.toRefC();
    }

    public void destroy() {
        shape.close();
    }
}
