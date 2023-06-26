package cz.lisacek.dragonevent.listeners;

import cz.lisacek.dragonevent.DragonEvent;
import cz.lisacek.dragonevent.cons.DePlayer;
import cz.lisacek.dragonevent.cons.Dragon;
import cz.lisacek.dragonevent.managers.EventManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.SQLException;

public class EventsListener implements Listener {

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof EnderDragon) {
            YamlConfiguration config = DragonEvent.getInstance().getConfig();
            if (event.getCause() == EntityDamageByEntityEvent.DamageCause.ENTITY_EXPLOSION ||
                    event.getCause() == EntityDamageByEntityEvent.DamageCause.BLOCK_EXPLOSION) {
                if (config.getBoolean("dragon.disable-explosion-damage")) {
                    event.setCancelled(true);
                    event.setDamage(0);
                }
            } else if (event.getDamager() instanceof Player) {
                Player damager = (Player) event.getDamager();
                Dragon dragon = EventManager.getINSTANCE().getDragonByEntity(event.getEntity());
                if (dragon != null) {
                    if (!dragon.getDamageMap().containsKey(damager)) {
                        dragon.getDamageMap().put(damager, 0.0);
                    }
                    dragon.getDamageMap().put(damager, dragon.getDamageMap().get(damager) + event.getDamage());
                }
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (DragonEvent.getInstance().getConnection().getInfo().isSqlLite()) {
            DragonEvent.getInstance().getConnection().update("INSERT OR IGNORE INTO de_votes (player, votes, last_vote) VALUES (?,0,0)", event.getPlayer().getName());
            DragonEvent.getInstance().getConnection().update("INSERT OR IGNORE INTO `de_stats` (`player`, `kills`, `damage`) VALUES (?,0,0)", event.getPlayer().getName());
        } else {
            DragonEvent.getInstance().getConnection().update("INSERT IGNORE INTO de_votes (player, votes, last_vote) VALUES (?,0,0)", event.getPlayer().getName());
            DragonEvent.getInstance().getConnection().update("INSERT IGNORE INTO `de_stats` (`player`, `kills`, `damage`) VALUES (?,0,0)", event.getPlayer().getName());
        }
        DragonEvent.getInstance().getConnection().query("SELECT * FROM de_stats JOIN de_votes ON de_votes.player = ? WHERE de_stats.player = ?", event.getPlayer().getName(), event.getPlayer().getName()).thenAcceptAsync(rs -> {
            try {
                if (rs.next()) {
                    DePlayer dePlayer = new DePlayer(event.getPlayer().getName());
                    dePlayer.setKills(rs.getInt("kills"));
                    dePlayer.setDamage(rs.getLong("damage"));
                    dePlayer.setVotes(rs.getInt("votes"));
                    dePlayer.setLastVote(rs.getLong("last_vote"));
                    EventManager.getINSTANCE().getPlayerMap().put(event.getPlayer().getName(), dePlayer);
                } else {
                    DePlayer dePlayer = new DePlayer(event.getPlayer().getName());
                    EventManager.getINSTANCE().getPlayerMap().put(event.getPlayer().getName(), dePlayer);
                }
            } catch (SQLException e) {

            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerQuitEvent event) {
        EventManager.getINSTANCE().removeDePlayer(event.getPlayer().getName());
    }
}
