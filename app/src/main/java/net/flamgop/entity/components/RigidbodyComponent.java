package net.flamgop.entity.components;


import com.github.stephengold.joltjni.Body;
import com.github.stephengold.joltjni.BodyCreationSettings;
import com.github.stephengold.joltjni.BodyInterface;
import com.github.stephengold.joltjni.enumerate.EActivation;
import com.github.stephengold.joltjni.enumerate.EMotionType;
import com.github.stephengold.joltjni.enumerate.EOverrideMassProperties;
import net.flamgop.asset.Asset;
import net.flamgop.asset.AssetIdentifier;
import net.flamgop.asset.AssetManager;
import net.flamgop.entity.Component;
import net.flamgop.physics.Layers;
import net.flamgop.physics.Material;
import net.flamgop.physics.Physics;
import net.flamgop.physics.PhysicsShape;
import net.flamgop.util.JoltJoml;

public class RigidbodyComponent extends Component {
    private final Physics physics;
    private final AssetIdentifier assetIdentifier;
    private final boolean dynamic;
    private final float mass;
    private final Material material;

    private Asset<PhysicsShape> shape;
    private Body body;

    public RigidbodyComponent(Physics physics, AssetIdentifier assetIdentifier, boolean dynamic, float mass, Material material) {
        this.physics = physics;
        this.assetIdentifier = assetIdentifier;
        this.dynamic = dynamic;
        this.mass = mass;
        this.material = material;
    }

    public RigidbodyComponent(Physics physics, AssetIdentifier assetIdentifier, Material material) {
        this(physics, assetIdentifier, false, 0f, material);
    }

    @SuppressWarnings("resource")
    @Override
    public void load(AssetManager assetManager) {
        this.shape = assetManager.loadSync(this.assetIdentifier, PhysicsShape.class);
        BodyCreationSettings settings = new BodyCreationSettings(shape.get().shape(), JoltJoml.toRVec3Arg(this.transform().position()), JoltJoml.toQuatArg(this.transform().rotation()), dynamic ? EMotionType.Dynamic : EMotionType.Static, dynamic ? Layers.MOVING : Layers.NON_MOVING);
        if (dynamic) {
            settings.getMassPropertiesOverride().setMass(mass);
            settings.setOverrideMassProperties(EOverrideMassProperties.CalculateInertia);
        }
        settings.setFriction(material.friction());
        settings.setRestitution(material.restitution());
        BodyInterface bi = physics.bodyInterface();
        this.body = bi.createBody(settings);
        bi.addBody(this.body, dynamic ? EActivation.Activate : EActivation.DontActivate);
    }

    @Override
    public void unload(AssetManager assetManager) {
        this.body.close();
        this.shape.release();
    }

    @Override
    public void physicsUpdate(float fixedDelta) {
        this.transform().position(JoltJoml.toVector3f(this.body.getPosition()));
        this.transform().rotation(JoltJoml.toQuaternionf(this.body.getRotation()));
    }
}
