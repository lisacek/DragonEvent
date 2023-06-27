package cz.lisacek.dragonevent.commands;

import cz.lisacek.dragonevent.DragonEvent;
import cz.lisacek.dragonevent.cons.DragonLoc;
import cz.lisacek.dragonevent.cons.SpawnOptions;
import cz.lisacek.dragonevent.managers.EventManager;
import cz.lisacek.dragonevent.utils.ColorHelper;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class DragonEventCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        YamlConfiguration config = DragonEvent.getInstance().getConfig();
        if (args.length == 0) {
            if (!commandSender.hasPermission(config.getString("help.permissions"))) {
                return true;
            }
            List<String> messages = config.getStringList("help.message");
            for (String message : messages) {
                commandSender.sendMessage(ColorHelper.colorize(message));
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!commandSender.hasPermission("dragonevent.reload")) {
                    commandSender.sendMessage("§cYou don't have permission to do that!");
                    return true;
                }
                DragonEvent.getInstance().loadConfig();
                commandSender.sendMessage(ColorHelper.colorize("&dDragonEvent &8&l| &aConfig reloaded!"));
                break;
            case "start":
                if (!commandSender.hasPermission("dragonevent.start")) {
                    commandSender.sendMessage("§cYou don't have permission to do that!");
                    return true;
                }
                Player player = (Player) commandSender;
                double hp = config.getDouble("dragon.health");
                if (config.getBoolean("dragon.dynamic-health.enable")) {
                    double amplifier = config.getDouble("dragon.dynamic-health.amplifier");
                    int onlinePlayers = Bukkit.getOnlinePlayers().size();
                    hp = hp + (hp * (onlinePlayers * amplifier));
                }
                SpawnOptions spawnOptions = new SpawnOptions
                        .SpawnOptionsBuilder()
                        .setDragonLoc(new DragonLoc("test", player.getLocation()))
                        .setDragonLocList(new ArrayList<>())
                        .setEverywhere(false)
                        .setHp(hp)
                        .setRandomLocation(false)
                        .setMoving(config.getBoolean("dragon.moving"))
                        .setGlowing(config.getBoolean("dragon.glow.enable"))
                        .setAnnounceSpawn(config.getBoolean("votifier.settings.announce-spawn.enable"))
                        .build();
                EventManager.getINSTANCE().spawnDragon(spawnOptions);
                return true;
            case "stop":
                if (!commandSender.hasPermission("dragonevent.stop")) {
                    commandSender.sendMessage("§cYou don't have permission to do that!");
                    return true;
                }
                EventManager.getINSTANCE().stop();
                commandSender.sendMessage(ColorHelper.colorize("&dDragonEvent &8&l| &aDragon event stopped!"));
                return true;
            default:
                if (!commandSender.hasPermission(config.getString("help.permissions"))) {
                    return true;
                }
                List<String> messages = config.getStringList("help.message");
                for (String message : messages) {
                    commandSender.sendMessage(ColorHelper.colorize(message));
                }
                return true;
        }
        return false;
    }

}
