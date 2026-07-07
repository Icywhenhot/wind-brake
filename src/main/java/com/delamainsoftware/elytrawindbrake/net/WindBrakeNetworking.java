package com.delamainsoftware.elytrawindbrake.net;

import net.minecraft.resources.ResourceLocation;

/**
 * Shared handshake constants + the one flag that gates every feature.
 *
 * The mod refuses to do anything unless the SERVER it's connected to also has the mod.
 * The handshake is a simple ping/pong on two custom plugin channels:
 *   - the client fires {@link #CHECK} at the server the moment it joins;
 *   - a server that has this mod answers with {@link #PRESENT} (a vanilla server can't,
 *     so it never does), and only that answer flips {@link #serverHasMod} on.
 *
 * Lives in the {@code main} source set so both the server initializer (main) and the
 * client mixin/initializer (client) can reference it — the client source set sees main.
 */
public final class WindBrakeNetworking {
    private WindBrakeNetworking() {}

    /** Serverbound: "do you have the mod?" — the client's join-time ping. */
    public static final ResourceLocation CHECK = new ResourceLocation("elytrawindbrake", "check");
    /** Clientbound: the server's "yes, I do" reply — the only thing that unlocks the mod. */
    public static final ResourceLocation PRESENT = new ResourceLocation("elytrawindbrake", "present");

    // Written on the client's network thread (packet receive / connection events), read on
    // the client tick + render threads, hence volatile. Defaults to false, so the mod starts
    // locked on every connection and only the server's reply can unlock it.
    private static volatile boolean serverHasMod = false;

    public static boolean serverHasMod() {
        return serverHasMod;
    }

    public static void setServerHasMod(boolean value) {
        serverHasMod = value;
    }
}
