package io.github.hello09x.fakeplayer.core.command.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.hello09x.fakeplayer.core.manager.saved.SavedBotMenu;
import io.github.hello09x.fakeplayer.core.manager.saved.SavedBotService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;

@Singleton
public class SavedBotCommand extends AbstractCommand {

    private final SavedBotService service;
    private final SavedBotMenu menu;

    @Inject
    public SavedBotCommand(SavedBotService service, SavedBotMenu menu) {
        this.service = service;
        this.menu = menu;
    }

    public void save(@NotNull CommandSender sender, @NotNull CommandArguments args) throws WrapperCommandSyntaxException {
        var bot = super.getFakeplayer(sender, args);
        service.save(bot);
        sender.sendMessage(text("Saved " + bot.getName() + ". It is now available in /fp gui", GREEN));
    }

    public void gui(@NotNull Player sender, @NotNull CommandArguments args) {
        menu.open(sender, 0);
    }
}
