package net.flamgop;

import net.flamgop.entity.Scene;
import net.flamgop.gpu.Camera;
import net.flamgop.input.InputState;
import net.flamgop.math.MathHelper;
import net.flamgop.physics.CollisionFlags;
import net.flamgop.physics.Physics;
import net.flamgop.physics.PhysicsScene;
import net.flamgop.physics.RaycastHit;
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
            if (hit.getActor().getType() == PxActorTypeEnum.eRIGID_DYNAMIC && !player.fly) {
                PxRigidDynamic dynamic = PxRigidDynamic.wrapPointer(hit.getActor().getAddress());
                if (dynamic.isSleeping()) {
                    dynamic.wakeUp();
                }

                PxVec3 pushDirection = hit.getWorldNormal();
                float force = -player.pushForce(hit.getActor());
                PxVec3 pushForce = new PxVec3(force, force, force);
                dynamic.addForce(pushDirection.multiply(pushForce), PxForceModeEnum.eFORCE);
                pushForce.destroy();

                // how do I make dynamic look like its getting convincingly pushed?
            }
        }
    }

    private static final float FOV = 60.0f;
    private static final float SPRINT_FOV = 75.0f;

    private static final float NORMAL_HEIGHT = 1.0f;
    private static final float CROUCH_HEIGHT = 0.55f;
    private static final float SLIDING_HEIGHT = 0.35f;

    private final Scene worldScene;
    private final Camera camera;
    private final PxVec3 temp = new PxVec3();
    private final InputState inputState;
    private final PhysicsScene scene;

    private final PxControllerFilters filters;
    private final PxController controller;

    private final Vector3f positionView = new Vector3f();
    private final Vector3f velocity = new Vector3f();

    private final float sensitivity = 0.5f;
    private final float crouchingSpeed = 2.5f;
    private final float speed = 5.0f;
    private final float sprintSpeed = 10.0f;
    private final float slidingSpeed = 10.0f;
    private final float jumpForce = 8f;
    private final float gravity;

    private float yaw = 0.0f;
    private float pitch = 0.0f;
    private boolean onGround = false;
    private boolean jumpQueued = false;

    private boolean fly = false;
    private boolean crouching = false;
    private boolean sprinting = false;

    private final Vector3f slideDirection = new Vector3f();
    private boolean shouldSlide = false;
    private float currentSlidingSpeed = 0f;

    private float currentFOV = (float) Math.toRadians(FOV);
    private float targetFOV = currentFOV;

    private float currentPlayerHeight = 1.0f;
    private float targetPlayerHeight = currentPlayerHeight;

    private RaycastHit hit;


    public Player(Scene worldScene, Physics physics, PhysicsScene scene, Camera camera, InputState inputState) {
        this.worldScene = worldScene;
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

        filters.setMFilterData(new PxFilterData(CollisionFlags.PLAYER.flag(), CollisionFlags.WORLD.flag(), 0, 0));

        this.gravity = 2 * scene.gravity();
    }

    public Camera camera() {
        return camera;
    }

    public Vector3f position() {
        return new Vector3f(positionView);
    }

    public Vector3f velocity() {
        return new Vector3f(velocity);
    }

    public void noclip() {
        fly = !fly;
    }

    public float pushForce(PxActor actor) {
        return crouching ? 1000f : 5000f;
    }

    public PhysicsScene scene() {
        return scene;
    }

    private void move(double delta) {
        PxControllerState state = new PxControllerState();
        controller.getState(state);
        boolean newGrounded = (state.getCollisionFlags() & PxControllerCollisionFlagEnum.eCOLLISION_DOWN.value) != 0;
        if (!this.onGround && newGrounded) {
            // we just landed!
        }
        this.onGround = newGrounded;

        Vector3f movementVector = new Vector3f();
        Vector3f forward = camera.forward();
        if (!fly) forward = forward.mul(1,0,1).normalize();
        Vector3f right = camera.right();
        if (!fly) right = right.mul(1,0,1).normalize();
        if (inputState.isKeyDown(GLFW.GLFW_KEY_W)) movementVector.add(forward);
        if (inputState.isKeyDown(GLFW.GLFW_KEY_S)) movementVector.sub(forward);
        if (inputState.isKeyDown(GLFW.GLFW_KEY_D)) movementVector.add(right);
        if (inputState.isKeyDown(GLFW.GLFW_KEY_A)) movementVector.sub(right);

        boolean isMoving = movementVector.lengthSquared() > 0.0001f;
        if (isMoving)
            movementVector.normalize();

        if (inputState.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) && !crouching) {
            if (!sprinting && isMoving) {
                sprinting = true;
                targetFOV = (float) Math.toRadians(SPRINT_FOV);
            } else if (sprinting && !isMoving) {
                sprinting = false;
                targetFOV = (float) Math.toRadians(FOV);
            }
        } else {
            if (sprinting) {
                if (crouching && onGround) {
                    shouldSlide = true;
                    if (isMoving) {
                        slideDirection.set(movementVector);
                    } else {
                        Vector3f fwd = camera.forward();
                        fwd.y = 0;
                        fwd.normalize();
                        slideDirection.set(fwd);
                    }
                    currentSlidingSpeed = slidingSpeed;
                    targetPlayerHeight = SLIDING_HEIGHT;
                }
                targetFOV = (float) Math.toRadians(FOV);
                sprinting = false;
            }
        }

        Vector3f acceleration;
        if (shouldSlide) {
            targetPlayerHeight = SLIDING_HEIGHT;
            acceleration = new Vector3f(slideDirection).mul(currentSlidingSpeed);
            currentSlidingSpeed = MathHelper.conditionalLerp(currentSlidingSpeed, 0f, (float)delta, onGround ? 1f : 0.2f, 0.5f);
            if (!crouching) {
                shouldSlide = false;
                targetPlayerHeight = NORMAL_HEIGHT;
            }
            if (currentSlidingSpeed == 0) {
                shouldSlide = false;
                targetPlayerHeight = CROUCH_HEIGHT;
            }
        } else {
            float movementSpeed;
            if (crouching) movementSpeed = crouchingSpeed;
            else if (sprinting) movementSpeed = sprintSpeed;
            else movementSpeed = speed;

            acceleration = movementVector.mul(movementSpeed);
        }

        velocity.add(acceleration);

        if (!onGround && !fly)
            velocity.add(0, (float) (gravity * delta),0);
        else if (!fly) {
            velocity.setComponent(1, -0.01f);
            if (jumpQueued && inputState.isKeyDown(GLFW.GLFW_KEY_SPACE)) {
                velocity.setComponent(1, jumpForce);
                onGround = false;
                jumpQueued = false;
            } else if (jumpQueued) {
                jumpQueued = false;
            }

            if (inputState.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL)) {
                if (!crouching) {
                    targetPlayerHeight = CROUCH_HEIGHT;
                    crouching = true;
                }
            } else {
                if (crouching) {
                    targetPlayerHeight = NORMAL_HEIGHT;
                    crouching = false;
                }
            }
        } else {
            if (crouching) {
                targetPlayerHeight = NORMAL_HEIGHT;
                crouching = false;
            }
            if (inputState.isKeyDown(GLFW.GLFW_KEY_SPACE)) {
                velocity.setComponent(1, speed);
            } else if (inputState.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL)) {
                velocity.setComponent(1, -speed);
            } else {
                velocity.setComponent(1, movementVector.y);
            }
        }

        Vector3f newVel = new Vector3f(velocity).mul((float) delta);
        PxVec3 disp = PhysxJoml.toPxVec3(newVel);
        controller.move(disp, 0.00001f, (float) delta, filters);
        disp.destroy();

        velocity.x *= (float) (0.1f * delta);
        velocity.z *= (float) (0.1f * delta);

        state.destroy();

        this.hit = this.scene.raycast(this.camera.position(), this.camera.forward(), 100f);
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

        if (inputState.wasKeyPressed(GLFW.GLFW_KEY_F)) {
            this.noclip();
        }

        float prevCurrentFOV = currentFOV;
        currentFOV = MathHelper.conditionalLerp(currentFOV, targetFOV, (float) delta, 5.0f);
        if (prevCurrentFOV != currentFOV) camera.fov(currentFOV);

        float prevCurrentPlayerHeight = currentPlayerHeight;
        currentPlayerHeight = MathHelper.conditionalLerp(currentPlayerHeight, targetPlayerHeight, (float) delta, 10.0f);
        if (prevCurrentPlayerHeight != currentPlayerHeight) controller.resize(currentPlayerHeight);

        camera.position(new Vector3f((float) controller.getPosition().getX(), (float) (controller.getPosition().getY() + 0.25f), (float) controller.getPosition().getZ()));
    }


    public void fixedUpdate(double delta) {
        move(delta);
    }

    public void render() {

    } // do nothing for now?

    public void renderDebug(TextRenderer textRenderer, float x, float y, float scale, double delta) {
        if (hit != null && hit.hit() && hit.data() != null) { // if the second is true the third is always true, but my editor will warn me if I don't add it anyway.
            textRenderer.drawText(
                    Game.INSTANCE.font(),
                    String.format("Position: %.2f %.2f %.2f Raycast Hit: %.2f %.2f %.2f\nVelocity: %.2f %.2f %.2f\nOnGround: %s", positionView.x(), positionView.y(), positionView.z(), hit.data().position().x(), hit.data().position().y(), hit.data().position().z(), velocity.x(), velocity.y(), velocity.z(), this.onGround ? "true" : "false"),
                    x, y, scale, new Vector3f(1.0f, 0.0f, 0.0f)
            );
        } else {
            textRenderer.drawText(
                    Game.INSTANCE.font(),
                    String.format("Position: %.2f %.2f %.2f\nVelocity: %.2f %.2f %.2f\nOnGround: %s", positionView.x(), positionView.y(), positionView.z(), velocity.x(), velocity.y(), velocity.z(), this.onGround ? "true" : "false"),
                    x, y, scale, new Vector3f(1.0f, 0.0f, 0.0f)
            );
        }
    }
}
