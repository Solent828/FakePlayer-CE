package io.github.hello09x.fakeplayer.core.command.impl;

import com.google.inject.Singleton;
import dev.jorel.commandapi.executors.CommandExecutor;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.hello09x.fakeplayer.core.Main;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Range;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Singleton
public class MoveCommand extends AbstractCommand {

    private final Map<UUID, BukkitTask> stopTasks = new HashMap<>();

    /**
     * 假人移动
     */
    public CommandExecutor move(@Range(from = 0, to = 1) float forward, @Range(from = 0, to = 1) float strafing) {
        return (sender, args) -> {
            var fake = getFakeplayer(sender, args);
            applyMovement(fake, forward, strafing);
            cancelStopTask(fake.getUniqueId());

            var handle = bridge.fromPlayer(fake);

            var fakeId = fake.getUniqueId();
            var stopping = new BukkitRunnable() {
                @Override
                public void run() {
                    handle.setXxa(0);
                    handle.setZza(0);
                    var self = stopTasks.get(fakeId);
                    if (self != null && self.getTaskId() == this.getTaskId()) {
                        stopTasks.remove(fakeId);
                    }
                }
            };

            this.stopTasks.put(fakeId, stopping.runTaskLater(Main.getInstance(), fake.isSprinting() ? 40 : 20));
        };
    }

    /** Starts moving until /fp move stop is used. */
    public CommandExecutor start(@Range(from = -1, to = 1) float forward, @Range(from = -1, to = 1) float strafing) {
        return (sender, args) -> {
            var fake = getFakeplayer(sender, args);
            cancelStopTask(fake.getUniqueId());
            applyMovement(fake, forward, strafing);
        };
    }

    public void stop(@NotNull CommandSender sender, @NotNull CommandArguments args) throws dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException {
        var fake = getFakeplayer(sender, args);
        cancelStopTask(fake.getUniqueId());
        var handle = bridge.fromPlayer(fake);
        handle.setXxa(0);
        handle.setZza(0);
    }

    private void applyMovement(@NotNull org.bukkit.entity.Player fake, float forward, float strafing) {
        var handle = bridge.fromPlayer(fake);
        float velocity = fake.isSneaking() ? 0.3F : 1.0F;
        handle.setZza(velocity * forward);
        handle.setXxa(velocity * strafing);
    }

    private void cancelStopTask(@NotNull UUID fakeId) {
        var task = stopTasks.remove(fakeId);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }


}
