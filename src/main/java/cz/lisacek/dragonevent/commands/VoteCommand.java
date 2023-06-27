package cz.lisacek.dragonevent.commands;

import cz.lisacek.dragonevent.DragonEvent;
import cz.lisacek.dragonevent.utils.ColorHelper;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class VoteCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] strings) {
        Player player = (Player) commandSender;
        List<String> messages = DragonEvent.getInstance().getConfig().getStringList("vote-command.message");
        for (String message : messages) {
            player.sendMessage(ColorHelper.colorize(message));
        }
        return true;
    }
}
