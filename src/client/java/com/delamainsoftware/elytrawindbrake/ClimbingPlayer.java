package com.delamainsoftware.elytrawindbrake;

/**
 * Implemented (via mixin) by the local player, and read by the avatar-renderer mixin.
 * It lets the renderer tilt the MODEL to match the climb arc while leaving the camera
 * alone — the model's gliding pitch and the camera are decoupled this way.
 */
public interface ClimbingPlayer {

    /** True while the creative climb arc is actively driving the player. */
    boolean elytrawindbrake$isClimbing();

    /**
     * The pitch (in the same convention as getViewXRot: 0 = level, -90 = straight up)
     * the model should be posed at, interpolated for the given render partial tick.
     */
    float elytrawindbrake$getModelPitch(float partialTick);
}
