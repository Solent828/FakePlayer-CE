package io.github.hello09x.fakeplayer.core.manager.invsee;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.devtools.core.utils.ComponentUtils;
import io.github.hello09x.fakeplayer.core.manager.FakeplayerList;
import io.github.hello09x.fakeplayer.core.manager.FakeplayerManager;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.kyori.adventure.text.Component.text;

/**
 * Built-in inventory editor with storage, armour and off-hand slots.
 *
 * @author tanyaofei
 * @since 2024/8/12
 **/
@Singleton
public class SimpleInvseeManagerImpl extends AbstractInvseeManager {

    @Inject
    public SimpleInvseeManagerImpl(FakeplayerManager manager, FakeplayerList fakeplayerList) {
        super(manager, fakeplayerList);
    }

    @Override
    protected @Nullable InventoryView openInventory(@NotNull Player viewer, @NotNull Player whom) {
        var holder = new BotInventoryHolder(whom);
        var inventory = Bukkit.createInventory(holder, 54, text("Bot Inventory: " + whom.getName()));
        holder.inventory = inventory;

        var storage = whom.getInventory().getStorageContents();
        for (int slot = 0; slot < Math.min(36, storage.length); slot++) {
            inventory.setItem(slot, storage[slot]);
        }
        inventory.setItem(45, whom.getInventory().getHelmet());
        inventory.setItem(46, whom.getInventory().getChestplate());
        inventory.setItem(47, whom.getInventory().getLeggings());
        inventory.setItem(48, whom.getInventory().getBoots());
        inventory.setItem(50, whom.getInventory().getItemInOffHand());
        return viewer.openInventory(inventory);
    }

    @EventHandler
    public void saveEquipment(@NotNull InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof BotInventoryHolder holder)) {
            return;
        }
        var target = holder.target;
        if (!target.isOnline() || manager.isNotFake(target)) {
            return;
        }
        var storage = new org.bukkit.inventory.ItemStack[36];
        for (int slot = 0; slot < storage.length; slot++) {
            storage[slot] = event.getInventory().getItem(slot);
        }
        target.getInventory().setStorageContents(storage);
        target.getInventory().setHelmet(event.getInventory().getItem(45));
        target.getInventory().setChestplate(event.getInventory().getItem(46));
        target.getInventory().setLeggings(event.getInventory().getItem(47));
        target.getInventory().setBoots(event.getInventory().getItem(48));
        target.getInventory().setItemInOffHand(event.getInventory().getItem(50));
    }

    private static final class BotInventoryHolder implements InventoryHolder {
        private final Player target;
        private Inventory inventory;

        private BotInventoryHolder(Player target) {
            this.target = target;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }


}
