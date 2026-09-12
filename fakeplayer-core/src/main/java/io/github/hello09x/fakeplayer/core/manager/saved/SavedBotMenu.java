package io.github.hello09x.fakeplayer.core.manager.saved;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.fakeplayer.core.Main;
import io.github.hello09x.fakeplayer.core.manager.FakeplayerManager;
import io.github.hello09x.fakeplayer.core.manager.invsee.InvseeManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Singleton
public class SavedBotMenu implements Listener {

    private static final int PAGE_SIZE = 45;
    private final SavedBotService service;
    private final FakeplayerManager manager;
    private final InvseeManager invseeManager;

    @Inject
    public SavedBotMenu(SavedBotService service, FakeplayerManager manager, InvseeManager invseeManager) {
        this.service = service;
        this.manager = manager;
        this.invseeManager = invseeManager;
    }

    public void open(@NotNull Player viewer, int requestedPage) {
        if (!viewer.isOp()) {
            return;
        }
        var bots = service.list();
        int pageCount = Math.max(1, (bots.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        var holder = new MenuHolder(page);
        var inventory = Bukkit.createInventory(holder, 54, text("Saved Bots - Page " + (page + 1) + "/" + pageCount));
        holder.inventory = inventory;

        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, bots.size());
        for (int index = from; index < to; index++) {
            inventory.setItem(index - from, botItem(bots.get(index)));
        }
        if (page > 0) inventory.setItem(45, named(Material.ARROW, "Previous Page", YELLOW));
        inventory.setItem(49, named(Material.BARRIER, "Close", RED));
        if (page + 1 < pageCount) inventory.setItem(53, named(Material.ARROW, "Next Page", YELLOW));
        viewer.openInventory(inventory);
    }

    @EventHandler
    public void click(@NotNull InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player viewer) || !viewer.isOp()) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot == 45) {
            open(viewer, holder.page - 1);
            return;
        }
        if (slot == 49) {
            viewer.closeInventory();
            return;
        }
        if (slot == 53) {
            open(viewer, holder.page + 1);
            return;
        }
        if (slot < 0 || slot >= PAGE_SIZE) {
            return;
        }
        var bots = service.list();
        int index = holder.page * PAGE_SIZE + slot;
        if (index >= bots.size()) {
            return;
        }
        var saved = bots.get(index);
        var online = manager.get(saved.name());
        if (event.isRightClick() && online != null) {
            invseeManager.invsee(viewer, online);
            return;
        }
        viewer.closeInventory();
        if (online != null) {
            service.despawn(saved);
            viewer.sendMessage(text("Despawned and saved " + saved.name(), YELLOW));
            return;
        }
        service.spawn(viewer, saved).thenAccept(player -> Bukkit.getScheduler().runTask(Main.getInstance(), () ->
                viewer.sendMessage(text("Spawned " + player.getName() + " at its saved location", GREEN))))
                .exceptionally(error -> {
                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> viewer.sendMessage(text("Could not spawn " + saved.name() + ": " + error.getMessage(), RED)));
                    return null;
                });
    }

    private @NotNull ItemStack botItem(@NotNull SavedBot bot) {
        var online = manager.get(bot.name()) != null;
        var item = new ItemStack(Material.PLAYER_HEAD);
        var meta = item.getItemMeta();
        meta.displayName(text(bot.name(), online ? GREEN : GRAY));
        meta.lore(List.of(
                text(online ? "Online" : "Saved / Offline", online ? GREEN : GRAY),
                text(online ? "Left-click to save and despawn" : "Left-click to spawn", YELLOW),
                text(online ? "Right-click to edit inventory and armour" : "Spawns at its saved location", AQUA)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static @NotNull ItemStack named(@NotNull Material material, @NotNull String name, @NotNull net.kyori.adventure.text.format.TextColor color) {
        var item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(text(name, color));
        item.setItemMeta(meta);
        return item;
    }

    private static final class MenuHolder implements InventoryHolder {
        private final int page;
        private Inventory inventory;

        private MenuHolder(int page) {
            this.page = page;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
