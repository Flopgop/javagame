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

public class Player {

    private final Camera camera;
    private final PxRigidDynamic actor;
    private final PxScene scene;
    private final PxVec3 temp = new PxVec3();
    private final InputState inputState;

    private final PxBoxGeometry groundCheck = new PxBoxGeometry(0.5f, 0.01f, 0.5f);
    private final PxTransform groundCheckTransform = new PxTransform();

    private final float defaultAcceleration = 0.5f;
    private final float airAcceleration = 0.25f;
    private final float sensitivity = 0.5f;

    private float yaw = 0.0f;
    private float pitch = 0.0f;
    private boolean onGround = false;

    public Player(Physics physics, PxScene scene, Camera camera, InputState inputState) {
        this.camera = camera;
        this.scene = scene;
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

    private void applyMotionDamping() {
        PxVec3 pxVelocity = actor.getLinearVelocity();
        Vector3f velocity = new Vector3f(pxVelocity.getX(), pxVelocity.getY(), pxVelocity.getZ());

        Vector3f dampingForce = new Vector3f(
                -velocity.x * 50f,
                -velocity.y * 0f,
                -velocity.z * 50f
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

        Vector3f impulse = new Vector3f();
        if (inputState.isKeyDown(GLFW.GLFW_KEY_W)) impulse.add(forward);
        if (inputState.isKeyDown(GLFW.GLFW_KEY_S)) impulse.sub(forward);
        if (inputState.isKeyDown(GLFW.GLFW_KEY_D)) impulse.add(right);
        if (inputState.isKeyDown(GLFW.GLFW_KEY_A)) impulse.sub(right);

        if (impulse.lengthSquared() > 0.01*0.01) {
            impulse.normalize().mul(onGround ? defaultAcceleration : airAcceleration);
            temp.setX(impulse.x);
            temp.setY(impulse.y);
            temp.setZ(impulse.z);
            actor.addForce(temp, PxForceModeEnum.eIMPULSE);
        }

        if (inputState.wasPressed(GLFW.GLFW_KEY_SPACE) && onGround) {
            temp.setX(0); temp.setY(5f * 9.81f); temp.setZ(0);
            actor.addForce(temp, PxForceModeEnum.eIMPULSE);
        }

        applyMotionDamping();

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
