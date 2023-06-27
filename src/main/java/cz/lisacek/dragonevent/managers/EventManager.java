package cz.lisacek.dragonevent.managers;

import cz.lisacek.dragonevent.DragonEvent;
import cz.lisacek.dragonevent.cons.DePlayer;
import cz.lisacek.dragonevent.cons.Dragon;
import cz.lisacek.dragonevent.cons.SpawnOptions;
import cz.lisacek.dragonevent.utils.ColorHelper;
import cz.lisacek.dragonevent.utils.Console;
import cz.lisacek.dragonevent.utils.GlowHelper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class EventManager {

    private static final EventManager INSTANCE = new EventManager();

    private final List<Dragon> dragonList = new ArrayList<>();

    private final Map<String, DePlayer> playerMap = new HashMap<>();

    public Dragon spawnDragon(SpawnOptions spawnOptions) {
        double hp = spawnOptions.getHp();
        final Dragon[] dragon = {null};
        if (spawnOptions.isEverywhere()) {
            spawnOptions.getDragonLocList().forEach(dragonLoc -> {
                EnderDragon entity = (EnderDragon) dragonLoc.getLocation().getWorld().spawnEntity(dragonLoc.getLocation(), EntityType.ENDER_DRAGON);
                dragon[0] = setupEntity(spawnOptions, hp, entity);
            });
        }
        if (spawnOptions.isRandomLocation()) {
            int random = (int) (Math.random() * spawnOptions.getDragonLocList().size());
            EnderDragon entity = (EnderDragon) spawnOptions.getDragonLocList().get(random).getLocation().getWorld().spawnEntity(spawnOptions.getDragonLocList().get(random).getLocation(), EntityType.ENDER_DRAGON);
            dragon[0] = setupEntity(spawnOptions, hp, entity);
        }
        if (spawnOptions.getDragonLoc() != null) {
            EnderDragon entity = (EnderDragon) spawnOptions.getDragonLoc().getLocation().getWorld().spawnEntity(spawnOptions.getDragonLoc().getLocation(), EntityType.ENDER_DRAGON);
            dragon[0] = setupEntity(spawnOptions, hp, entity);
        }
        if (spawnOptions.isAnnounceSpawn()) {
            Bukkit.getOnlinePlayers().forEach(player -> {
                List<String> messages = DragonEvent.getInstance().getConfig().getStringList("votifier.settings.announce-spawn.message");
                for (String message : messages) {
                    player.sendMessage(ColorHelper.colorize(message
                            .replace("%x%", String.valueOf(dragon[0].getDragon().getLocation().getBlockX()))
                            .replace("%y%", String.valueOf(dragon[0].getDragon().getLocation().getBlockY()))
                            .replace("%world%", dragon[0].getDragon().getLocation().getWorld().getName())
                            .replace("%z%", String.valueOf(dragon[0].getDragon().getLocation().getBlockZ()))));
                }
            });
        } else if (DragonEvent.getInstance().getConfig().getBoolean("announcements.spawn.enable")) {
            if (DragonEvent.getInstance().getConfig().getBoolean("titles.spawn.enable")) {
                Bukkit.getOnlinePlayers().forEach(player -> {
                    player.sendTitle(ColorHelper.colorize(DragonEvent.getInstance().getConfig().getString("titles.spawn.title")), ColorHelper.colorize(DragonEvent.getInstance().getConfig().getString("titles.spawn.subtitle")),
                            DragonEvent.getInstance().getConfig().getInt("titles.spawn.fadein"),
                            DragonEvent.getInstance().getConfig().getInt("titles.spawn.stay"),
                            DragonEvent.getInstance().getConfig().getInt("titles.spawn.fadeout"));
                    List<String> messages = DragonEvent.getInstance().getConfig().getStringList("announcements.spawn.message");
                    for (String message : messages) {
                        player.sendMessage(ColorHelper.colorize(message));
                    }
                });
            }
        }
        return dragon[0];
    }

    private Dragon setupEntity(SpawnOptions spawnOptions, double hp, EnderDragon entity) {
        entity.setHealth(hp);
        entity.setPhase(spawnOptions.isMoving() ? EnderDragon.Phase.CIRCLING : EnderDragon.Phase.HOVER);
        entity.setGlowing(spawnOptions.isGlowing());
        if (spawnOptions.isGlowing()) {
            GlowHelper.setGlowing(entity, ChatColor.valueOf(DragonEvent.getInstance().getConfig().getString("dragon.glow.color")));
        }
        if(spawnOptions.isMoving()) {
            entity.setMaxHealth(hp);
        }
        Dragon d = new Dragon(entity, spawnOptions.getHp(), spawnOptions.isMoving());
        dragonList.add(d);
        return d;
    }

    //get DePlayer
    public DePlayer getDePlayer(String player) {
        CompletableFuture<DePlayer> completableFuture = new CompletableFuture<>();
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player);
        if (!playerMap.containsKey(player)) {
            DragonEvent.getInstance().getConnection().query("SELECT * FROM de_stats JOIN de_votes ON de_votes.player = ? WHERE de_stats.player = ?", offlinePlayer.getName(), offlinePlayer.getName()).thenAcceptAsync(rs -> {
                try {
                    if (rs.next()) {
                        DePlayer dePlayer = new DePlayer(offlinePlayer.getName());
                        dePlayer.setKills(rs.getInt("kills"));
                        dePlayer.setDamage(rs.getLong("damage"));
                        dePlayer.setVotes(rs.getInt("votes"));
                        dePlayer.setLastVote(rs.getLong("last_vote"));
                        EventManager.getINSTANCE().getPlayerMap().put(offlinePlayer.getName(), dePlayer);
                        completableFuture.complete(dePlayer);
                    } else {
                        DePlayer dePlayer = new DePlayer(offlinePlayer.getName());
                        EventManager.getINSTANCE().getPlayerMap().put(offlinePlayer.getName(), dePlayer);
                        completableFuture.complete(dePlayer);
                    }
                } catch (SQLException e) {
                    Console.info("&cAn error occurred while getting the player data of " + offlinePlayer.getName());
                }
            });
            return completableFuture.join();
        }
        return playerMap.get(player);
    }

    //getdragon by entity
    public Dragon getDragonByEntity(Entity entity) {
        for (Dragon dragon : dragonList) {
            if (dragon.getDragon().equals(entity)) {
                return dragon;
            }
        }
        return null;
    }

    public void stop() {
        dragonList.forEach(dragon -> {
            dragon.getDragon().setHealth(0);
            dragon.getDragon().remove();
        });
        dragonList.clear();
    }

    //remove deplayer by name
    public void removeDePlayer(String player) {
        playerMap.remove(player);
    }

    public Map<String, DePlayer> getPlayerMap() {
        return playerMap;
    }

    public List<Dragon> getDragonList() {
        return dragonList;
    }

    public static EventManager getINSTANCE() {
        return INSTANCE;
    }
}
