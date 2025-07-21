package net.flamgop;

import net.flamgop.gpu.Camera;
import net.flamgop.input.InputState;
import net.flamgop.physics.CollisionFlags;
import net.flamgop.physics.Physics;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import physx.common.PxIDENTITYEnum;
import physx.common.PxTransform;
import physx.common.PxVec3;
import physx.geometry.PxBoxGeometry;
import physx.geometry.PxGeometry;
import physx.physics.*;

@SuppressWarnings("FieldCanBeLocal")
public class Player {

    private final Camera camera;
    private final PxRigidDynamic actor;
    private final Physics physics;
    private final PxScene scene;
    private final PxVec3 temp = new PxVec3();
    private final InputState inputState;

    private final PxBoxGeometry groundCheck = new PxBoxGeometry(0.5f, 0.01f, 0.5f);
    private final PxTransform groundCheckTransform = new PxTransform();

    private final float defaultAcceleration = 1f;
    private final float airAcceleration = 0.5f;
    private final float jumpAcceleration = 4f;
    private final float sensitivity = 0.5f;

    private float yaw = 0.0f;
    private float pitch = 0.0f;
    private boolean onGround = false;

    public Player(Physics physics, Camera camera, InputState inputState) {
        this.camera = camera;
        this.physics = physics;
        this.scene = physics.scene();
        this.inputState = inputState;

        PxGeometry geometry = new PxBoxGeometry(0.25f, 1f, 0.25f);
        PxMaterial material = physics.physics().createMaterial(0f, 0f, 0f);
        material.setFrictionCombineMode(PxCombineModeEnum.eMAX);
        material.setRestitutionCombineMode(PxCombineModeEnum.eMIN);
        PxShapeFlags shapeFlags = new PxShapeFlags((byte) (PxShapeFlagEnum.eSCENE_QUERY_SHAPE.value | PxShapeFlagEnum.eSIMULATION_SHAPE.value));

        temp.setX(camera.position().x());
        temp.setY(camera.position().y());
        temp.setZ(camera.position().z());
        PxTransform transform = new PxTransform(PxIDENTITYEnum.PxIdentity);
        transform.setP(temp);

        PxFilterData filterData = new PxFilterData(CollisionFlags.PLAYER.flag(), CollisionFlags.WORLD.flag(), 0, 0);
        PxShape shape = physics.physics().createShape(geometry, material, true, shapeFlags);
        this.actor = physics.physics().createRigidDynamic(transform);
        shape.setSimulationFilterData(filterData);
        this.actor.attachShape(shape);
        this.actor.setMass(8f);
        this.actor.setRigidBodyFlag(PxRigidBodyFlagEnum.eKINEMATIC, false);
        this.actor.setActorFlag(PxActorFlagEnum.eDISABLE_GRAVITY, false);
        scene.addActor(this.actor);

        this.actor.setRigidDynamicLockFlag(PxRigidDynamicLockFlagEnum.eLOCK_ANGULAR_X, true);
        this.actor.setRigidDynamicLockFlag(PxRigidDynamicLockFlagEnum.eLOCK_ANGULAR_Y, true);
        this.actor.setRigidDynamicLockFlag(PxRigidDynamicLockFlagEnum.eLOCK_ANGULAR_Z, true);

        this.actor.setLinearDamping(1.0f);

        filterData.destroy();
    }

    private void applyMotionDamping(double delta) {
        PxVec3 pxVelocity = actor.getLinearVelocity();
        Vector3f velocity = new Vector3f(pxVelocity.getX(), pxVelocity.getY(), pxVelocity.getZ());

        float dampingPerSecond = 5;
        float decay = (float) Math.exp(-dampingPerSecond * delta);

        Vector3f dampingForce = new Vector3f(
                -velocity.x * decay,
                -velocity.y * 0f,
                -velocity.z * decay
        );
        temp.setX(dampingForce.x);
        temp.setY(dampingForce.y);
        temp.setZ(dampingForce.z);
        actor.addForce(temp, PxForceModeEnum.eFORCE);
    }

    private void checkOnGround() {
        PxSweepResult result = new PxSweepResult();

        PxTransform transform = actor.getGlobalPose();
        temp.setX(transform.getP().getX()); temp.setY(transform.getP().getY()-1.02f); temp.setZ(transform.getP().getZ());
        groundCheckTransform.setP(temp);
        temp.setX(0);
        temp.setY(-1);
        temp.setZ(0);
        temp.normalize();

        this.onGround = scene.sweep(groundCheck, groundCheckTransform, temp, 0.01f, result);

        result.destroy();
    }

    public void update(double delta) {
        checkOnGround();

        Vector2f mouseDelta = inputState.deltaMousePosition();
        yaw += mouseDelta.x * sensitivity;
        pitch -= mouseDelta.y * sensitivity;
        if (pitch < -80f) pitch = -80f;
        if (pitch > 80f) pitch = 80f;

        Vector3f forward = camera.forward().mul(1,0,1).normalize();
        Vector3f right = camera.right();

        Vector3f acceleration = new Vector3f();
        if (inputState.isKeyDown(GLFW.GLFW_KEY_W)) acceleration.add(forward);
        if (inputState.isKeyDown(GLFW.GLFW_KEY_S)) acceleration.sub(forward);
        if (inputState.isKeyDown(GLFW.GLFW_KEY_D)) acceleration.add(right);
        if (inputState.isKeyDown(GLFW.GLFW_KEY_A)) acceleration.sub(right);

        if (acceleration.lengthSquared() > 0.01*0.01) {
            acceleration.normalize().mul(onGround ? defaultAcceleration : airAcceleration);
            temp.setX(acceleration.x);
            temp.setY(acceleration.y);
            temp.setZ(acceleration.z);
            actor.addForce(temp, PxForceModeEnum.eACCELERATION);
        }

        if (inputState.wasKeyPressed(GLFW.GLFW_KEY_SPACE) && onGround) {
            temp.setX(0); temp.setY(jumpAcceleration * -physics.gravity()); temp.setZ(0);
            actor.addForce(temp, PxForceModeEnum.eIMPULSE);
        }

        applyMotionDamping(delta);

        PxTransform transform = actor.getGlobalPose();
        PxVec3 pos = transform.getP();
        Vector3f position = new Vector3f(pos.getX(), pos.getY(), pos.getZ());

        camera.position(position.add(0,0.5f,0));

        double radYaw = Math.toRadians(yaw);
        double radPitch = Math.toRadians(pitch);

        Vector3f target = new Vector3f(
                (float) (Math.cos(radPitch) * Math.sin(radYaw)),
                (float) Math.sin(radPitch),
                (float) (-Math.cos(radPitch) * Math.cos(radYaw))
        ).normalize().add(position);

        camera.target(target);
        camera.reconfigureView();
    }

    public void render() {} // do nothing for now?
}
