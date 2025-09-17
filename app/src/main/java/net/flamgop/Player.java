package net.flamgop;

import com.github.stephengold.joltjni.*;
import com.github.stephengold.joltjni.enumerate.EGroundState;
import com.github.stephengold.joltjni.readonly.RVec3Arg;
import com.github.stephengold.joltjni.readonly.Vec3Arg;
import net.flamgop.gpu.Camera;
import net.flamgop.input.InputState;
import net.flamgop.math.MathHelper;
import net.flamgop.physics.Layers;
import net.flamgop.physics.Physics;
import net.flamgop.physics.RaycastHit;
import net.flamgop.text.TextRenderer;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings("FieldCanBeLocal")
public class Player {

    private static final float FOV = 60.0f;
    private static final float SPRINT_FOV = 75.0f;

    private static final float NORMAL_HEIGHT = 1.0f;
    private static final float CROUCH_HEIGHT = 0.55f;
    private static final float SLIDING_HEIGHT = 0.35f;

    private final Camera camera;
    private final InputState inputState;

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

    private final Physics physics;
    private final Shape shape;
    private final CharacterVirtualRef characterVirtual;
    private final ExtendedUpdateSettings extendedUpdateSettings;

    public Player(Physics physics, Camera camera, InputState inputState) {
        this.camera = camera;
        this.inputState = inputState;
        this.physics = physics;

        this.shape = new CapsuleShape(1f, 0.5f);
        CharacterVirtualSettings settings = new CharacterVirtualSettings();
        settings.setShape(shape);
        settings.setInnerBodyLayer(Layers.PLAYER);

        RVec3Arg startLocation = new RVec3(0.,2.,0.);
        characterVirtual = new CharacterVirtual(settings, startLocation, new Quat(), 0, physics.system()).toRef();

        extendedUpdateSettings = new ExtendedUpdateSettings();
        extendedUpdateSettings.setStickToFloorStepDown(Vec3.sZero());
        extendedUpdateSettings.setWalkStairsStepUp(Vec3.sZero());

        //noinspection resource
        this.gravity = physics.system().getGravity().getY() * 2f;
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

    private void move(double delta) {
        boolean newGrounded = characterVirtual.getGroundState() == EGroundState.OnGround;
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

        characterVirtual.setLinearVelocity(new Vec3(velocity.x, velocity.y, velocity.z));

        velocity.x *= (float) (0.1f * delta);
        velocity.z *= (float) (0.1f * delta);

//        this.hit = this.scene.raycast(this.camera.position(), this.camera.forward(), 100f);
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
        if (prevCurrentPlayerHeight != currentPlayerHeight) {}

        camera.position(new Vector3f((float) characterVirtual.getPosition().getX(), ((float) characterVirtual.getPosition().getY() - 1f + currentPlayerHeight) + 0.25f, (float) characterVirtual.getPosition().getZ()));
    }


    @SuppressWarnings("resource")
    public void fixedUpdate(double delta) {
        move(delta);
        Vec3Arg gravity = physics.system().getGravity();
        BroadPhaseLayerFilter bplFilter
                = physics.system().getDefaultBroadPhaseLayerFilter(Layers.MOVING);
        ObjectLayerFilter olFilter
                = physics.system().getDefaultLayerFilter(Layers.MOVING);
        TempAllocator tempAllocator = physics.tempAllocator();
        characterVirtual.extendedUpdate((float) delta, gravity, extendedUpdateSettings, bplFilter,
                olFilter, physics.allBodies(), physics.allShapes(), tempAllocator);
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
