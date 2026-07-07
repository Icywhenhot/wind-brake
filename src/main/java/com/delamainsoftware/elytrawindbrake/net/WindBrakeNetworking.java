package com.delamainsoftware.elytrawindbrake.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Shared handshake payloads + the one flag that gates every feature.
 *
 * The mod refuses to do anything unless the SERVER it's connected to also has the mod.
 * The handshake is a ping/pong of two empty custom payloads:
 *   - the client fires {@link CheckPayload} at the server the moment it joins;
 *   - a server that has this mod answers with {@link PresentPayload} (a vanilla server can't,
 *     so it never does), and only that answer flips {@link #serverHasMod} on.
 *
 * This branch targets 1.20.5–1.21.1, which use Minecraft's {@code CustomPacketPayload}
 * codec networking (the older byte-buf channel API was removed at 1.20.5). Both payloads
 * are empty — their mere arrival is the entire message — so each uses {@link StreamCodec#unit}.
 *
 * Lives in the {@code main} source set so both the server initializer (main) and the
 * client initializer/mixin (client) can reference it — the client source set sees main.
 */
public final class WindBrakeNetworking {
    private WindBrakeNetworking() {}

    /** Serverbound: "do you have the mod?" — the client's join-time ping. */
    public record CheckPayload() implements CustomPacketPayload {
        public static final CheckPayload INSTANCE = new CheckPayload();
        public static final Type<CheckPayload> TYPE = new Type<>(id("check"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CheckPayload> CODEC =
                StreamCodec.unit(INSTANCE);

        @Override
        public Type<CheckPayload> type() {
            return TYPE;
        }
    }

    /** Clientbound: the server's "yes, I do" reply — the only thing that unlocks the mod. */
    public record PresentPayload() implements CustomPacketPayload {
        public static final PresentPayload INSTANCE = new PresentPayload();
        public static final Type<PresentPayload> TYPE = new Type<>(id("present"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PresentPayload> CODEC =
                StreamCodec.unit(INSTANCE);

        @Override
        public Type<PresentPayload> type() {
            return TYPE;
        }
    }

    // ResourceLocation's constructors went private at 1.21 (factory methods only), while
    // 1.20.5/1.20.6 still had the public constructor. tryParse(String) exists across the whole
    // 1.20.5–1.21.1 range, so building IDs through it keeps one jar spanning the 1.21 seam.
    private static ResourceLocation id(String path) {
        return ResourceLocation.tryParse("elytrawindbrake:" + path);
    }

    // Written on the client's network/tick thread (packet receive / connection events), read on
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
