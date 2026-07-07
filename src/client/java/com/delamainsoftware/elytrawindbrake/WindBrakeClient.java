package com.delamainsoftware.elytrawindbrake;

import com.delamainsoftware.elytrawindbrake.net.WindBrakeNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

/**
 * Client side of the handshake. On every connection the mod starts LOCKED and asks the
 * server whether it has the mod; only the server's reply unlocks the features (see
 * {@link WindBrakeNetworking}). This is what stops the mod being used client-side only.
 */
public class WindBrakeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Server confirmed it has the mod -> unlock. Flip the flag on the client thread.
        ClientPlayNetworking.registerGlobalReceiver(WindBrakeNetworking.PRESENT,
                (client, handler, buf, responseSender) ->
                        client.execute(() -> WindBrakeNetworking.setServerHasMod(true)));

        // Fresh connection: assume no mod on the server, then ping to ask. A vanilla server
        // never answers, so the features stay off there.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            WindBrakeNetworking.setServerHasMod(false);
            sender.sendPacket(WindBrakeNetworking.CHECK, PacketByteBufs.empty());
        });

        // Disconnect: relock, so a previous "enabled" server can't leak into the next one.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                WindBrakeNetworking.setServerHasMod(false));
    }
}
