package io.github.hello09x.fakeplayer.core.manager.saved;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.fakeplayer.core.Main;
import io.github.hello09x.fakeplayer.core.entity.FakeplayerTicker;
import io.github.hello09x.fakeplayer.core.manager.FakeplayerManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class SavedBotService implements Listener {

    private final FakeplayerManager manager;
    private final File file;
    private final YamlConfiguration data;
    private final Set<UUID> skipNextQuitSnapshot = new HashSet<>();

    @Inject
    public SavedBotService(FakeplayerManager manager) {
        this.manager = manager;
        this.file = new File(Main.getInstance().getDataFolder(), "saved-bots.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized void save(@NotNull Player bot) {
        if (manager.isNotFake(bot)) {
            throw new IllegalArgumentException(bot.getName() + " is not a fake player");
        }
        var path = path(bot.getName());
        data.set(path + ".name", bot.getName());
        data.set(path + ".location", bot.getLocation());
        writeInventory(path, bot);
        flush();
    }

    public synchronized @NotNull List<SavedBot> list() {
        var section = data.getConfigurationSection("bots");
        if (section == null) {
            return List.of();
        }
        var result = new ArrayList<SavedBot>();
        for (var key : section.getKeys(false)) {
            var name = data.getString("bots." + key + ".name");
            var location = data.getLocation("bots." + key + ".location");
            if (name != null && location != null) {
                result.add(new SavedBot(name, location));
            }
        }
        result.sort(Comparator.comparing(SavedBot::name, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    public @Nullable SavedBot find(@NotNull String name) {
        return list().stream().filter(bot -> bot.name().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public @NotNull CompletableFuture<Player> spawn(@NotNull Player operator, @NotNull SavedBot saved) {
        var online = manager.get(saved.name());
        if (online != null) {
            return CompletableFuture.completedFuture(online);
        }
        return manager.spawnAsync(operator, saved.name(), saved.location().clone(), FakeplayerTicker.NON_REMOVE_AT)
                .thenApply(player -> {
                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> restore(player));
                    reapplySkinsRestorerSkin(player);
                    return player;
                });
    }

    /**
     * FakePlayer applies its default skin during login, which can overwrite a
     * skin previously assigned to this UUID by SkinsRestorer. Reapply the
     * stored SkinsRestorer skin after the fake player has completely joined.
     */
    private void reapplySkinsRestorerSkin(@NotNull Player bot) {
        if (!Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer")) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            if (bot.isOnline()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sr applyskin " + bot.getName());
            }
        }, 10L);
    }

    public synchronized boolean despawn(@NotNull SavedBot saved) {
        var player = manager.get(saved.name());
        if (player == null) {
            return false;
        }
        save(player);
        skipNextQuitSnapshot.add(player.getUniqueId());
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);
        return manager.remove(player.getName(), "saved bot despawned");
    }

    public synchronized void restore(@NotNull Player bot) {
        var saved = find(bot.getName());
        if (saved == null) {
            return;
        }
        var base = path(saved.name()) + ".inventory";
        var storage = new ItemStack[36];
        for (int slot = 0; slot < storage.length; slot++) {
            storage[slot] = data.getItemStack(base + ".storage." + slot);
        }
        bot.getInventory().setStorageContents(storage);
        bot.getInventory().setHelmet(data.getItemStack(base + ".helmet"));
        bot.getInventory().setChestplate(data.getItemStack(base + ".chestplate"));
        bot.getInventory().setLeggings(data.getItemStack(base + ".leggings"));
        bot.getInventory().setBoots(data.getItemStack(base + ".boots"));
        var offhand = data.getItemStack(base + ".offhand");
        bot.getInventory().setItemInOffHand(offhand == null ? new ItemStack(org.bukkit.Material.AIR) : offhand);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void snapshotOnQuit(@NotNull PlayerQuitEvent event) {
        var player = event.getPlayer();
        synchronized (this) {
            if (skipNextQuitSnapshot.remove(player.getUniqueId())) {
                return;
            }
        }
        if (manager.isFake(player) && find(player.getName()) != null) {
            save(player);
        }
    }

    public void saveAllOnline() {
        for (var player : manager.getAll()) {
            if (find(player.getName()) != null) {
                save(player);
            }
        }
    }

    private void writeInventory(@NotNull String path, @NotNull Player bot) {
        var base = path + ".inventory";
        data.set(base, null);
        var storage = bot.getInventory().getStorageContents();
        for (int slot = 0; slot < storage.length; slot++) {
            data.set(base + ".storage." + slot, storage[slot]);
        }
        data.set(base + ".helmet", bot.getInventory().getHelmet());
        data.set(base + ".chestplate", bot.getInventory().getChestplate());
        data.set(base + ".leggings", bot.getInventory().getLeggings());
        data.set(base + ".boots", bot.getInventory().getBoots());
        data.set(base + ".offhand", bot.getInventory().getItemInOffHand());
    }

    private static @NotNull String path(@NotNull String name) {
        var key = Base64.getUrlEncoder().withoutPadding().encodeToString(name.toLowerCase().getBytes(StandardCharsets.UTF_8));
        return "bots." + key;
    }

    private void flush() {
        try {
            data.save(file);
        } catch (IOException error) {
            throw new UncheckedIOException("Failed to save " + file.getName(), error);
        }
    }
}
