package io.github.hello09x.fakeplayer.core.manager;

import com.google.inject.Singleton;
import io.github.hello09x.fakeplayer.core.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Makes fake players participate in Essentials' AFK state.
 *
 * <p>Essentials does not always start its normal activity tracker for players
 * without a real client connection. This bridge uses Essentials' configured
 * auto-AFK delay and marks a still-online fake player AFK through the public
 * user object exposed by Essentials.</p>
 */
@Singleton
public class EssentialsAfkManager {

    private static final Logger log = Main.getInstance().getLogger();

    /**
     * Schedule Essentials AFK handling for a newly spawned fake player.
     * A disabled/missing Essentials installation is deliberately a no-op.
     */
    public void schedule(@NotNull Player player) {
        var essentials = Bukkit.getPluginManager().getPlugin("Essentials");
        if (essentials == null || !essentials.isEnabled()) {
            return;
        }

        if (!(essentials instanceof JavaPlugin essentialsPlugin)) {
            log.warning("Essentials is not a JavaPlugin; fake-player AFK integration was skipped");
            return;
        }

        long timeoutSeconds = essentialsPlugin.getConfig().getLong("auto-afk", -1L);
        if (timeoutSeconds <= 0L) {
            return;
        }

        long delayTicks;
        try {
            delayTicks = Math.multiplyExact(timeoutSeconds, 20L);
        } catch (ArithmeticException ignored) {
            log.warning("Essentials auto-afk timeout is too large; fake-player AFK integration was skipped");
            return;
        }

        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            if (!player.isOnline()) {
                return;
            }
            this.setAfk(essentials, player);
        }, delayTicks);
    }

    private void setAfk(@NotNull Plugin essentials, @NotNull Player player) {
        try {
            var getUser = findSingleArgumentMethod(essentials.getClass(), "getUser", Player.class);
            var user = getUser.invoke(essentials, player);
            if (user == null) {
                log.warning("Essentials did not provide a user for fake player " + player.getName());
                return;
            }

            var setAfk = user.getClass().getMethod("setAfk", boolean.class);
            setAfk.invoke(user, true);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException error) {
            log.log(Level.WARNING, "Failed to mark fake player " + player.getName() + " as AFK through Essentials", error);
        }
    }

    private static @NotNull Method findSingleArgumentMethod(
            @NotNull Class<?> type,
            @NotNull String name,
            @NotNull Class<?> argumentType
    ) throws NoSuchMethodException {
        return Arrays.stream(type.getMethods())
                .filter(method -> method.getName().equals(name))
                .filter(method -> method.getParameterCount() == 1)
                .filter(method -> method.getParameterTypes()[0].isAssignableFrom(argumentType))
                .findFirst()
                .orElseThrow(() -> new NoSuchMethodException(type.getName() + "#" + name + "(" + argumentType.getName() + ")"));
    }
}
