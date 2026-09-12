package io.github.hello09x.fakeplayer.v26_1_2.spi;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Routes fake-player chat through Bukkit on 26.1.2 so installed chat plugins
 * receive the same chat event they receive from an ordinary player.
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
