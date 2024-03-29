package cz.lisacek.dragonevent.commands;

import cz.lisacek.dragonevent.DragonEvent;
import cz.lisacek.dragonevent.utils.ColorHelper;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class VoteTopCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] strings) {
        String position = DragonEvent.getInstance().getConfig().getString("vote-top.positions", "&7%pos%. &e%player% &7- &e%votes% votes");
        AtomicInteger x = new AtomicInteger();
        List<String> messages = DragonEvent.getInstance().getConfig().getStringList("vote-top.message");
        for (String message : messages) {
            if(message.contains("%positions%")) {
                DragonEvent.getInstance().getTop10().forEach((player, votes) -> {
                    x.getAndIncrement();
                    commandSender.sendMessage(ColorHelper.colorize(position.replace("%pos%", String.valueOf(x)).replace("%player%", player).replace("%votes%", String.valueOf(votes))));
                });
            } else {
                commandSender.sendMessage(ColorHelper.colorize(message));
            }
        }
        return true;
    }
}
