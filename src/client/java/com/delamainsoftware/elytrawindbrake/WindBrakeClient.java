package com.delamainsoftware.elytrawindbrake;

import com.delamainsoftware.elytrawindbrake.net.WindBrakeNetworking;
import com.delamainsoftware.elytrawindbrake.net.WindBrakeNetworking.CheckPayload;
import com.delamainsoftware.elytrawindbrake.net.WindBrakeNetworking.PresentPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Client side of the handshake. On every connection the mod starts LOCKED and asks the
 * server whether it has the mod; only the server's reply unlocks the features (see
 * {@link WindBrakeNetworking}). This is what stops the mod being used client-side only.
 */
public class WindBrakeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Server confirmed it has the mod -> unlock. Payload receivers run on the client thread.
        ClientPlayNetworking.registerGlobalReceiver(PresentPayload.TYPE, (payload, context) ->
                WindBrakeNetworking.setServerHasMod(true));

        // Fresh connection: assume no mod on the server, then ask — but only if the server
        // actually declared the CHECK channel (i.e. it has the mod). canSend guards against
        // the new API throwing when sending an unknown payload to a vanilla server; such a
        // server never registered the channel, so we simply never ask and stay locked.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            WindBrakeNetworking.setServerHasMod(false);
            if (ClientPlayNetworking.canSend(CheckPayload.TYPE)) {
                ClientPlayNetworking.send(CheckPayload.INSTANCE);
            }
        });

        // Disconnect: relock, so a previous "enabled" server can't leak into the next one.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                WindBrakeNetworking.setServerHasMod(false));
    }
}
