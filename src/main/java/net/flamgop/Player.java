package net.flamgop;

import net.flamgop.gpu.Camera;
import net.flamgop.input.InputState;
import net.flamgop.physics.Physics;
import net.flamgop.physics.PhysicsScene;
import net.flamgop.text.TextRenderer;
import net.flamgop.util.PhysxJoml;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import physx.character.*;
import physx.common.PxVec3;
import physx.physics.*;

@SuppressWarnings("FieldCanBeLocal")
public class Player {

    private static class WakerUpperBehaviorCallback extends PxControllerBehaviorCallbackImpl {
        @Override
        public int getShapeBehaviorFlags(PxShape shape, PxActor actor) {
            if (actor.getType() == PxActorTypeEnum.eRIGID_DYNAMIC) {
                PxRigidDynamic dynamic = PxRigidDynamic.wrapPointer(actor.getAddress());
                if (dynamic.isSleeping()) {
                    dynamic.wakeUp();
                }
            }
            return 0;
        }
    }

    private static class PusherHitReport extends PxUserControllerHitReportImpl {

        private final Player player;

        public PusherHitReport(Player player) {
            this.player = player;
        }

        @Override
        public void onShapeHit(PxControllerShapeHit hit) {
            if (hit.getActor().getType() == PxActorTypeEnum.eRIGID_DYNAMIC) {
                PxRigidDynamic dynamic = PxRigidDynamic.wrapPointer(hit.getActor().getAddress());
                if (dynamic.isSleeping()) { dynamic.wakeUp(); }

                PxVec3 pushDirection = hit.getWorldNormal();
                float force = -player.pushForce(hit.getActor());
                PxVec3 pushForce = new PxVec3(force, force, force);
                dynamic.addForce(pushDirection.multiply(pushForce), PxForceModeEnum.eFORCE);
                pushForce.destroy();

                // how do I make dynamic look like its getting convincingly pushed?
            }
        }
    }

    private final Camera camera;
    private final PxVec3 temp = new PxVec3();
    private final InputState inputState;
    private final PhysicsScene scene;

    private final PxControllerFilters filters;
    private final PxController controller;

    private final Vector3f positionView = new Vector3f();
    private final Vector3f velocity = new Vector3f();

    private final float sensitivity = 0.5f;
    private final float speed = 5.0f;

    private float yaw = 0.0f;
    private float pitch = 0.0f;
    private boolean onGround = false;
    private boolean jumpQueued = false;

    public Player(Physics physics, PhysicsScene scene, Camera camera, InputState inputState) {
        this.camera = camera;
        this.scene = scene;
        this.inputState = inputState;

        this.filters = new PxControllerFilters();
        PxMaterial material = physics.physics().createMaterial(0.0f, 0.0f, 0.0f);
        PxBoxControllerDesc desc = new PxBoxControllerDesc();
        desc.setHalfHeight(1f);
        desc.setHalfForwardExtent(0.5f);
        desc.setHalfSideExtent(0.5f);
        PxExtendedVec3 posExt = new PxExtendedVec3(camera.position().x, camera.position().y, camera.position().z);
        desc.setPosition(posExt);
        temp.setX(0); temp.setY(1); temp.setZ(0);
        desc.setUpDirection(temp);
        desc.setMaterial(material);
        desc.setBehaviorCallback(new WakerUpperBehaviorCallback());
        desc.setReportCallback(new PusherHitReport(this));
        this.controller = scene.controllerManager().createController(desc);
        posExt.destroy();
        controller.move(new PxVec3(0, -0.1f, 0), 0.1f, 0.016f, filters);
    }

    public float pushForce(PxActor actor) {
        return 5000.0f;
    }

    public PhysicsScene scene() {
        return scene;
    }

    private void move(double delta) {
        PxControllerState state = new PxControllerState();
        controller.getState(state);
        this.onGround = (state.getCollisionFlags() & PxControllerCollisionFlagEnum.eCOLLISION_DOWN.value) != 0;

        Vector3f movementVector = new Vector3f();
        Vector3f forward = camera.forward().mul(1,0,1).normalize();
        Vector3f right = camera.right().mul(1,0,1).normalize();
        if (inputState.isKeyDown(GLFW.GLFW_KEY_W)) movementVector.add(forward);
        if (inputState.isKeyDown(GLFW.GLFW_KEY_S)) movementVector.sub(forward);
        if (inputState.isKeyDown(GLFW.GLFW_KEY_D)) movementVector.add(right);
        if (inputState.isKeyDown(GLFW.GLFW_KEY_A)) movementVector.sub(right);

        movementVector.mul(speed);

        velocity.add(movementVector);

        if (!onGround)
            velocity.add(0, (float) (scene.gravity() * delta),0);
        else {
            velocity.setComponent(1, -0.01f);
            if (jumpQueued && inputState.isKeyDown(GLFW.GLFW_KEY_SPACE)) {
                velocity.setComponent(1, 5f);
                onGround = false;
                jumpQueued = false;
            } else if (jumpQueued) {
                jumpQueued = false;
            }
        }

        Vector3f newVel = new Vector3f(velocity).mul((float) delta);
        PxVec3 disp = PhysxJoml.toPxVec3(newVel);
        controller.move(disp, 0.00001f, (float) delta, filters);
        disp.destroy();

        velocity.x *= (float) (0.1f * delta);
        velocity.z *= (float) (0.1f * delta);

        state.destroy();

        camera.position(new Vector3f((float) controller.getPosition().getX(), (float) (controller.getPosition().getY() + 0.25f), (float) controller.getPosition().getZ()));
    }

    public void update(double delta) {
        Vector2f mouseDelta = inputState.deltaMousePosition();
        yaw += mouseDelta.x * sensitivity;
        pitch -= mouseDelta.y * sensitivity;
        pitch = Math.max(-80f, Math.min(80f, pitch));

        double radYaw = Math.toRadians(yaw);
        double radPitch = Math.toRadians(pitch);
        Vector3f target = new Vector3f(
                (float) (Math.cos(radPitch) * Math.sin(radYaw)),
                (float) Math.sin(radPitch),
                (float) (-Math.cos(radPitch) * Math.cos(radYaw))
        ).normalize().add(camera.position());

        camera.target(target);
        camera.reconfigureView();
        positionView.set(camera.position());

        if (inputState.wasKeyPressed(GLFW.GLFW_KEY_SPACE) && !jumpQueued) {
            jumpQueued = true;
        }
    }


    public void fixedUpdate(double delta) {
        move(delta);
    }

    public void render() {

    } // do nothing for now?

    public void renderDebug(TextRenderer textRenderer, double delta) {
        textRenderer.drawText(
                Game.INSTANCE.font(),
                String.format("Position: %.2f %.2f %.2f\nVelocity: %.2f %.2f %.2f\nOnGround: %s", positionView.x(), positionView.y(), positionView.z(), velocity.x(), velocity.y(), velocity.z(), this.onGround ? "true" : "false"),
                5f, Game.INSTANCE.height() - 4 * (Game.INSTANCE.font().lineHeight() * 0.5f), 0.5f, new Vector3f(1.0f, 0.0f, 0.0f)
        );
    }
}
