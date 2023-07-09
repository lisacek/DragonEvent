package cz.lisacek.dragonevent.listeners;

import cz.lisacek.dragonevent.DragonEvent;
import cz.lisacek.dragonevent.cons.DePlayer;
import cz.lisacek.dragonevent.cons.Dragon;
import cz.lisacek.dragonevent.managers.EventManager;
import cz.lisacek.dragonevent.sql.DatabaseConnection;
import cz.lisacek.dragonevent.utils.ColorHelper;
import cz.lisacek.dragonevent.utils.Console;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EventsListener implements Listener {

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof EnderDragon)) {
            return;
        }
        processDragonDamage(event);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        YamlConfiguration config = DragonEvent.getInstance().getConfig();
        if (event.getEntity() instanceof EnderDragon) {
            if (config.getBoolean("dragon.griefing")) return;
            event.blockList().clear();
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        YamlConfiguration config = DragonEvent.getInstance().getConfig();
        if(!config.getBoolean("dragon.modify-damage.enable")) return;
        if (event.getDamager() instanceof EnderDragon) {
            event.setDamage(config.getDouble("dragon.modify-damage.damage"));
        }
    }


    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        DatabaseConnection connection = DragonEvent.getInstance().getConnection();
        loadPlayer(event, connection);
        YamlConfiguration config = DragonEvent.getInstance().getConfig();
        if (config.getBoolean("votifier.settings.offline-votes", false)) return;
        loadOfflineVotes(event, connection, config);
    }

    @EventHandler
    public void onPlayerJoin(PlayerQuitEvent event) {
        EventManager.getINSTANCE().removeDePlayer(event.getPlayer().getName());
    }

    private void processDragonDamage(EntityDamageByEntityEvent event) {
        YamlConfiguration config = DragonEvent.getInstance().getConfig();
        EntityDamageByEntityEvent.DamageCause damageCause = event.getCause();
        if (damageCause == EntityDamageByEntityEvent.DamageCause.ENTITY_EXPLOSION ||
                damageCause == EntityDamageByEntityEvent.DamageCause.BLOCK_EXPLOSION) {
            if (config.getBoolean("dragon.disable-explosion-damage")) {
                event.setCancelled(true);
                event.setDamage(0);
            }
        } else if (event.getDamager() instanceof Player) {
            Player damager = (Player) event.getDamager();
            Dragon dragon = EventManager.getINSTANCE().getDragonByEntity(event.getEntity());
            if (dragon != null) {
                double damage = event.getDamage();
                dragon.getDamageMap().computeIfPresent(damager, (player, currentDamage) -> currentDamage + damage);
                dragon.getDamageMap().putIfAbsent(damager, damage);
            }
        }
    }

    private void loadOfflineVotes(PlayerJoinEvent event, DatabaseConnection connection, YamlConfiguration config) {
        connection.query("SELECT * FROM de_offline_votes WHERE username = ?", event.getPlayer().getName()).thenAcceptAsync(rs -> {
            try {
                int votes = 0;
                while (rs.next()) {
                    votes++;
                }
                processOfflineVotes(event, config, votes);
            } catch (SQLException e) {
                Console.info("&cError while loading player data from database: " + e.getMessage());
            }
        }).exceptionally(throwable -> {
            Console.info("&cError while loading player data from database: " + throwable.getMessage());
            return null;
        });
    }


    private void loadPlayer(PlayerJoinEvent event, DatabaseConnection connection) {
        connection.update(connection.getInfo().isSqlLite()
                ? "INSERT OR IGNORE INTO de_votes (player, votes, last_vote) VALUES (?,0,0)"
                : "INSERT IGNORE INTO de_votes (player, votes, last_vote) VALUES (?,0,0)", event.getPlayer().getName());
        connection.update(connection.getInfo().isSqlLite()
                ? "INSERT OR IGNORE INTO `de_stats` (`player`, `kills`, `damage`) VALUES (?,0,0)"
                : "INSERT IGNORE INTO `de_stats` (`player`, `kills`, `damage`) VALUES (?,0,0)", event.getPlayer().getName());

        connection.query("SELECT * FROM de_stats JOIN de_votes ON de_votes.player = ? WHERE de_stats.player = ?", event.getPlayer().getName(), event.getPlayer().getName()).thenAcceptAsync(rs -> {
            try {
                if (rs.next()) {
                    DePlayer dePlayer = new DePlayer(event.getPlayer().getName());
                    dePlayer.setKills(rs.getInt("kills"));
                    dePlayer.setDamage(rs.getDouble("damage"));
                    dePlayer.setVotes(rs.getInt("votes"));
                    dePlayer.setLastVote(rs.getLong("last_vote"));
                    EventManager.getINSTANCE().getPlayerMap().put(event.getPlayer().getName(), dePlayer);
                } else {
                    DePlayer dePlayer = new DePlayer(event.getPlayer().getName());
                    EventManager.getINSTANCE().getPlayerMap().put(event.getPlayer().getName(), dePlayer);
                }
            } catch (SQLException e) {
                Console.info("&cError while loading player data from database: " + e.getMessage());
            }
        });
    }

    private void processOfflineVotes(PlayerJoinEvent event, YamlConfiguration config, int votes) {
        //distribute rewards
        List<List<String>> rewards = new ArrayList<>();
        Bukkit.getScheduler().runTask(DragonEvent.getInstance(), () -> {
            for (int x = 0; x < votes; x++) {
                if (config.getBoolean("votifier.settings.vote-reward.enable")) {
                    List<String> commands = config.getStringList("votifier.settings.vote-reward.commands");
                    rewards.add(commands);
                }
            }
        });

        Bukkit.getScheduler().runTaskLater(DragonEvent.getInstance(), () -> {
            if (votes > 0) {
                DePlayer dePlayer = EventManager.getINSTANCE().getPlayerMap().get(event.getPlayer().getName());
                dePlayer.getOfflineRewards().addAll(rewards);
                dePlayer.setOfflineRewardsCount(votes);
                EventManager.getINSTANCE().getPlayerMap().put(event.getPlayer().getName(), dePlayer);
                offlineVotesMessage(event, config, votes);
            }
        }, 10L);
    }

    private void offlineVotesMessage(PlayerJoinEvent event, YamlConfiguration config, int finalVotes) {
        Bukkit.getScheduler().runTask(DragonEvent.getInstance(), () -> {
            if (config.getBoolean("votifier.settings.offline-vote-received.join-message.enable", false)) {
                List<String> message = ColorHelper.colorize(config.getStringList("votifier.settings.offline-vote-received.join-message.messages"));
                for (String line : message) {
                    event.getPlayer().sendMessage(line.replace("%rewards%", String.valueOf(finalVotes)));
                }
            }
        });
    }
}
