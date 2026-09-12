package io.github.hello09x.fakeplayer.v26_1_2.spi;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 26.1.2 uses the Bukkit/Paper chat entry point so fake-player messages pass
 * through the same chat event pipeline as ordinary player messages.
 */
public class NMSServerPlayerImpl extends io.github.hello09x.fakeplayer.v26_1.spi.NMSServerPlayerImpl {

    public NMSServerPlayerImpl(@NotNull Player player) {
        super(player);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void chat(@NotNull String message) {
        getPlayer().chat(message);
    }
}
