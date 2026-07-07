package com.delamainsoftware.elytrawindbrake;

import com.delamainsoftware.elytrawindbrake.net.WindBrakeNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * The whole server side: it does nothing but answer the client's "do you have the mod?"
 * handshake. Registered under the {@code main} entrypoint so it runs on a dedicated server
 * AND on the integrated server behind singleplayer (so singleplayer still enables the mod).
 *
 * The reply itself IS the proof — a vanilla server has no receiver for {@link WindBrakeNetworking#CHECK}
 * and simply drops the unknown plugin-channel packet, so it never sends {@link WindBrakeNetworking#PRESENT}
 * and the client keeps the features locked.
 */
public class WindBrakeServer implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(WindBrakeNetworking.CHECK,
                (server, player, handler, buf, responseSender) ->
                        // Hop to the server thread before touching the connection.
                        server.execute(() -> ServerPlayNetworking.send(
                                player, WindBrakeNetworking.PRESENT, PacketByteBufs.empty())));
    }
}
