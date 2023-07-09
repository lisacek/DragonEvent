package cz.lisacek.dragonevent.managers;

import cz.lisacek.dragonevent.DragonEvent;
import cz.lisacek.dragonevent.cons.Dragon;
import cz.lisacek.dragonevent.cons.DragonLoc;
import cz.lisacek.dragonevent.cons.SpawnOptions;
import cz.lisacek.dragonevent.utils.ColorHelper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

public class VoteManager {

    private static final VoteManager INSTANCE = new VoteManager();
    private final int votesNeeded;

    private final List<Location> dragonLocList = new ArrayList<>();

    private Dragon dragon;

    private VoteManager() {
        this.votesNeeded = DragonEvent.getInstance().getConfig().getInt("votifier.settings.goal");
        Objects.requireNonNull(DragonEvent.getInstance().getConfig().getConfigurationSection("locations")).getKeys(false).forEach(l -> {
            Location location = new Location(Bukkit.getWorld(Objects.requireNonNull(DragonEvent.getInstance().getConfig().getString("locations." + l + ".world"))),
                    DragonEvent.getInstance().getConfig().getDouble("locations." + l + ".x"),
                    DragonEvent.getInstance().getConfig().getDouble("locations." + l + ".y"),
                    DragonEvent.getInstance().getConfig().getDouble("locations." + l + ".z"));
            dragonLocList.add(location);
        });

    }

    private int votes = 0;

    public void reminderTask() {
        Bukkit.getScheduler().runTaskTimer(DragonEvent.getInstance(), () -> Bukkit.getOnlinePlayers().forEach(player -> {
            List<String> messages = DragonEvent.getInstance().getConfig().getStringList("votifier.settings.reminder.message");
            for (String message : messages) {
                player.sendMessage(ColorHelper.colorize(message
                        .replace("%votes%", String.valueOf(votes))
                        .replace("%player%", player.getName())
                        .replace("%votesNeeded%", String.valueOf(votesNeeded))));
            }
        }), 0, DragonEvent.getInstance().getConfig().getInt("votifier.settings.reminder.interval") * 20L);
    }

    public int getVotes() {
        return votes;
    }

    public int getVotesNeeded() {
        return votesNeeded;
    }

    public void incrementVotes() {
        YamlConfiguration config = DragonEvent.getInstance().getConfig();
        votes++;
        if (votes >= votesNeeded) {
            votes = 0;
            dragon = EventManager.getINSTANCE().spawnDragon(config, dragonLocList);
            DragonEvent.getInstance().getConnection().update("UPDATE de_vote_party SET num = ? WHERE id = ?", 0, 1);
        } else {
            DragonEvent.getInstance().getConnection().update("UPDATE de_vote_party SET num = ? WHERE id = ?", votes, 1);
        }
    }

    public void setVotes(int votes) {
        this.votes = votes;
    }

    //get top 10 de players by votes
    public Map<String, Integer> getTop10votes() {
        Map<String, Integer> top10 = new HashMap<>();
        DragonEvent.getInstance().getConnection().query("SELECT * FROM de_votes ORDER BY votes DESC LIMIT 10").thenAcceptAsync(rs -> {
            try {
                while (rs.next()) {
                    top10.put(rs.getString("player"), rs.getInt("votes"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return top10;
    }

    //top 10 de players by kills
    public Map<String, Integer> getTop10kills() {
        Map<String, Integer> top10 = new HashMap<>();
        DragonEvent.getInstance().getConnection().query("SELECT * FROM de_stats ORDER BY kills DESC LIMIT 10").thenAcceptAsync(rs -> {
            try {
                while (rs.next()) {
                    top10.put(rs.getString("player"), rs.getInt("kills"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return top10;
    }

    //top 10 de players by damage
    public Map<String, Double> getTop10damage() {
        Map<String, Double> top10 = new HashMap<>();
        DragonEvent.getInstance().getConnection().query("SELECT * FROM de_stats ORDER BY damage DESC LIMIT 10").thenAcceptAsync(rs -> {
            try {
                while (rs.next()) {
                    top10.put(rs.getString("player"), rs.getDouble("damage"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return top10;
    }

    public Dragon getDragon() {
        return dragon;
    }

    public static VoteManager getINSTANCE() {
        return INSTANCE;
    }

}
