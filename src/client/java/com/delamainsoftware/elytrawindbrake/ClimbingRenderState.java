package com.delamainsoftware.elytrawindbrake;

/**
 * Carries the creative-climb pose from the player entity into the avatar RENDER STATE.
 *
 * The entity renderer no longer receives the entity inside {@code setupRotations} — it
 * only sees a per-frame render-state snapshot. So the climb flag and the already-
 * interpolated model pitch are copied off {@link ClimbingPlayer} during render-state
 * extraction (where the partial tick is available) and stashed here, then read back when
 * the body is posed. This keeps the camera fully decoupled from the model tilt.
 */
public interface ClimbingRenderState {

    /** True while the creative climb arc is actively driving the player this frame. */
    boolean elytrawindbrake$isClimbing();

    /**
     * The pitch (same convention as getViewXRot: 0 = level, -90 = straight up) the model
     * should be posed at this frame, already interpolated for the render partial tick.
     */
    float elytrawindbrake$getModelPitch();

    /** Copies this frame's climb state in during render-state extraction. */
    void elytrawindbrake$setClimb(boolean climbing, float modelPitch);
}
