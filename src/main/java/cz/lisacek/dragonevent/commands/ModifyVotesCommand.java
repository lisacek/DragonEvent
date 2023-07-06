package cz.lisacek.dragonevent.commands;

import cz.lisacek.dragonevent.DragonEvent;
import cz.lisacek.dragonevent.cons.DePlayer;
import cz.lisacek.dragonevent.managers.EventManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ModifyVotesCommand implements CommandExecutor {

    private static final String ADMIN_PERMISSION = "dragonevent.admin";

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if(!commandSender.hasPermission(ADMIN_PERMISSION)) {
            commandSender.sendMessage("§cYou don't have permission to use this command");
            return true;
        }

        if(args.length != 2) {
            commandSender.sendMessage("§cUsage: /modifyvotes <player> <amount>");
            return true;
        }

        DePlayer dePlayer = EventManager.getINSTANCE().getDePlayer(args[0]);
        if(dePlayer == null) {
            commandSender.sendMessage("§cPlayer not found");
            return true;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            commandSender.sendMessage("§cAmount must be a number");
            return true;
        }
        if(amount < 0) {
            commandSender.sendMessage("§cAmount must be positive");
            return true;
        }
        dePlayer.setVotes(dePlayer.getVotes() + amount);
        EventManager.getINSTANCE().getPlayerMap().put(dePlayer.getName(), dePlayer);
        DragonEvent.getInstance().getConnection().update("UPDATE de_votes SET votes = votes + ? WHERE player = ?", amount, dePlayer.getName());
        commandSender.sendMessage("§aVotes have been modified");
        return true;
    }
}
