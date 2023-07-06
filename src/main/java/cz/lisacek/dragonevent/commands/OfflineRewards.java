package cz.lisacek.dragonevent.commands;

import cz.lisacek.dragonevent.DragonEvent;
import cz.lisacek.dragonevent.cons.DePlayer;
import cz.lisacek.dragonevent.managers.EventManager;
import cz.lisacek.dragonevent.utils.ColorHelper;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.checkerframework.checker.units.qual.A;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class OfflineRewards implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        DePlayer dePlayer = EventManager.getINSTANCE().getDePlayer(commandSender.getName());
        if (dePlayer == null) {
            commandSender.sendMessage("§cUnknown player");
            return true;
        }
        YamlConfiguration config = DragonEvent.getInstance().getConfig();
        if (dePlayer.getOfflineRewards().isEmpty()) {
            List<String> messages = ColorHelper.colorize(config.getStringList("votifier.settings.offline-vote-received.messages.none"));
            for (String message : messages) {
                commandSender.sendMessage(message.replace("%player%", commandSender.getName()));
            }
            return true;
        }
        for (List<String> commands : dePlayer.getOfflineRewards()) {
            for (String cmd : commands) {
                commandSender.getServer().dispatchCommand(commandSender.getServer().getConsoleSender(), cmd.replace("%player%", commandSender.getName()));
            }
        }
        dePlayer.getOfflineRewards().clear();
        List<String> messages = ColorHelper.colorize(config.getStringList("votifier.settings.offline-vote-received.messages.claim"));
        for (String message : messages) {
            commandSender.sendMessage(message.replace("%player%", commandSender.getName()));
        }
        EventManager.getINSTANCE().getPlayerMap().put(commandSender.getName(), dePlayer);
        DragonEvent.getInstance().getConnection().update("DELETE FROM de_offline_votes WHERE username = ?", commandSender.getName());
        DragonEvent.getInstance().getConnection().update("UPDATE de_votes SET votes = votes + ? WHERE player = ?", dePlayer.getOfflineRewardsCount(), commandSender.getName());
        return true;
    }
}
