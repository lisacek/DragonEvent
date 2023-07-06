package cz.lisacek.dragonevent.commands;

import cz.lisacek.dragonevent.DragonEvent;
import cz.lisacek.dragonevent.managers.EventManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ResetVotesCommand implements CommandExecutor {

    private static final String ADMIN_PERMISSION = "dragonevent.admin";

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(!commandSender.hasPermission(ADMIN_PERMISSION)) {
            commandSender.sendMessage("§cYou don't have permission to use this command");
            return true;
        }
        DragonEvent.getInstance().getConnection().update("DELETE FROM de_votes");
        DragonEvent.getInstance().getConnection().update("DELETE FROM de_offline_votes");
        EventManager.getINSTANCE().getPlayerMap().clear();
        DragonEvent.getInstance().getTop10().clear();
        DragonEvent.getInstance().resetTopPlayers();
        DragonEvent.getInstance().loadPlayerData();
        DragonEvent.getInstance().loadTopPlayers();
        commandSender.sendMessage("§aVotes have been reset");
        return true;
    }
}
