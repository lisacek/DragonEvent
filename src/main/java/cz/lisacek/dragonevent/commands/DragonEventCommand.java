package cz.lisacek.dragonevent.commands;

import cz.lisacek.dragonevent.DragonEvent;
import cz.lisacek.dragonevent.cons.DragonLoc;
import cz.lisacek.dragonevent.cons.SpawnOptions;
import cz.lisacek.dragonevent.managers.EventManager;
import cz.lisacek.dragonevent.utils.ColorHelper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DragonEventCommand implements CommandExecutor {

    private static final String ADMIN_PERMISSION = "dragonevent.admin";
    private static final String HELP_PERMISSION;

    static {
        YamlConfiguration config = DragonEvent.getInstance().getConfig();
        HELP_PERMISSION = config.getString("help.permissions", "dragonevent.help");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        YamlConfiguration config = DragonEvent.getInstance().getConfig();

        if (args.length == 0) {
            assert HELP_PERMISSION != null;
            if (!sender.hasPermission(HELP_PERMISSION)) {
                return true;
            }
            sendHelpMessages(sender, config.getStringList("help.message"));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                if (!sender.hasPermission(ADMIN_PERMISSION)) {
                    sender.sendMessage("§cYou don't have permission to do that!");
                    return true;
                }
                DragonEvent.getInstance().loadConfig();
                sender.sendMessage(ColorHelper.colorize("&dDragonEvent &8&l| &aConfig reloaded!"));
                break;
            case "start":
                if (!sender.hasPermission(ADMIN_PERMISSION)) {
                    sender.sendMessage("§cYou don't have permission to do that!");
                    return true;
                }
                Location location;
                if (args.length > 1) {
                    String loc = args[1];
                    location = new Location(
                            Bukkit.getWorld(Objects.requireNonNull(config.getString("locations." + loc + ".world"))),
                            config.getInt("locations." + loc + ".x"),
                            config.getInt("locations." + loc + ".y"),
                            config.getInt("locations." + loc + ".z")
                    );
                } else {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("§cYou must be a player to do that!");
                        return true;
                    }
                    location = ((Player) sender).getLocation();
                }
                double hp = config.getDouble("dragon.health");
                if (config.getBoolean("dragon.dynamic-health.enable")) {
                    double amplifier = config.getDouble("dragon.dynamic-health.amplifier");
                    int onlinePlayers = Bukkit.getOnlinePlayers().size();
                    hp += hp * (onlinePlayers * amplifier);
                }
                SpawnOptions spawnOptions = new SpawnOptions.SpawnOptionsBuilder()
                        .setDragonLoc(new DragonLoc("test", location))
                        .setDragonLocList(new ArrayList<>())
                        .setEverywhere(false)
                        .setHp(hp)
                        .setRandomLocation(false)
                        .setMoving(config.getBoolean("dragon.moving"))
                        .setGlowing(config.getBoolean("dragon.glow.enable"))
                        .setAnnounceSpawn(config.getBoolean("votifier.settings.announce-spawn.enable"))
                        .build();
                EventManager.getINSTANCE().spawnDragon(spawnOptions);
                break;
            case "killall":
                if (!sender.hasPermission(ADMIN_PERMISSION)) {
                    sender.sendMessage("§cYou don't have permission to do that!");
                    return true;
                }
                EventManager.getINSTANCE().stop();
                sender.sendMessage(ColorHelper.colorize("&dDragonEvent &8&l| &aDragon event stopped!"));
                break;
            default:
                assert HELP_PERMISSION != null;
                if (!sender.hasPermission(HELP_PERMISSION)) {
                    return true;
                }
                sendHelpMessages(sender, config.getStringList("help.message"));
                break;
        }

        return false;
    }

    private void sendHelpMessages(CommandSender sender, List<String> messages) {
        List<String> newMessages = ColorHelper.colorize(messages);
        for (String message : newMessages) {
            sender.sendMessage(message);
        }
    }
}
