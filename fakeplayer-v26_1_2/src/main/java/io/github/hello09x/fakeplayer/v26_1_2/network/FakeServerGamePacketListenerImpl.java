package io.github.hello09x.fakeplayer.v26_1_2.network;

import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import org.jetbrains.annotations.NotNull;

/**
 * Supplies a believable, stable-for-a-short-period latency for fake players.
 * Fake connections never exchange keep-alive packets, so vanilla otherwise
 * reports them as having exactly zero milliseconds of latency forever.
 */
public class FakeServerGamePacketListenerImpl
        extends io.github.hello09x.fakeplayer.v26_1.network.FakeServerGamePacketListenerImpl {

    private static final int MINIMUM_PING = 28;
    private static final int BASE_PING_RANGE = 35;
    private static final int VARIATION_RANGE = 9;
    private static final long CHANGE_INTERVAL_MILLIS = 30_000L;

    private final int pingSeed;

    public FakeServerGamePacketListenerImpl(
            @NotNull MinecraftServer server,
            @NotNull Connection connection,
            @NotNull ServerPlayer player,
            @NotNull CommonListenerCookie cookie
    ) {
        super(server, connection, player, cookie);
        this.pingSeed = player.getUUID().hashCode();
    }

    @Override
    public int latency() {
        int base = MINIMUM_PING + Math.floorMod(this.pingSeed, BASE_PING_RANGE);
        long period = System.currentTimeMillis() / CHANGE_INTERVAL_MILLIS;
        long mixed = period * 1_103_515_245L + this.pingSeed * 12_345L;
        int variation = Math.floorMod((int) (mixed ^ (mixed >>> 32)), VARIATION_RANGE) - 4;
        return base + variation;
    }
}
