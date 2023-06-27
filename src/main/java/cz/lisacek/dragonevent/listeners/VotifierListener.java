package cz.lisacek.dragonevent.listeners;

import com.vexsoftware.votifier.model.VotifierEvent;
import cz.lisacek.dragonevent.DragonEvent;
import cz.lisacek.dragonevent.cons.DePlayer;
import cz.lisacek.dragonevent.cons.Dragon;
import cz.lisacek.dragonevent.managers.EventManager;
import cz.lisacek.dragonevent.managers.VoteManager;
import cz.lisacek.dragonevent.sql.DatabaseConnection;
import cz.lisacek.dragonevent.utils.BossBarColorHelper;
import cz.lisacek.dragonevent.utils.ColorHelper;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.Objects;

public class VotifierListener implements Listener {

    private final BossBar bossBar = Bukkit.createBossBar(DragonEvent.getInstance().getConfig().getString("votifier.settings.bossbar.text"), BarColor.BLUE, BarStyle.SOLID);
    private int hue = 0;
    private java.awt.Color rgb = java.awt.Color.getHSBColor((float) this.hue / 360.0F, 1.0F, 1.0F);

    public VotifierListener() {
        if (DragonEvent.getInstance().getConfig().getBoolean("votifier.settings.bossbar.enable")) {
            barTask();
        }
        if (!DragonEvent.getInstance().getConfig().getBoolean("votifier.settings.bossbar.rainbow")) {
            bossBar.setColor(BarColor.valueOf(DragonEvent.getInstance().getConfig().getString("votifier.settings.bossbar.color")));
        }
    }

    public void barTask() {
        Bukkit.getScheduler().runTaskTimer(DragonEvent.getInstance(), () -> {
            Dragon dragon = VoteManager.getINSTANCE().getDragon();
            YamlConfiguration config = DragonEvent.getInstance().getConfig();
            if (dragon == null || dragon.getDragon().isDead()) {
                if (config.getBoolean("votifier.settings.bossbar.rainbow")) {
                    increaseHue();
                    bossBar.setColor(BarColor.valueOf(BossBarColorHelper.getColorNameFromRgb(this.rgb.getRed(), this.rgb.getGreen(), this.rgb.getBlue())));
                }
                bossBar.setProgress((double) VoteManager.getINSTANCE().getVotes() / VoteManager.getINSTANCE().getVotesNeeded());
                bossBar.setTitle(ColorHelper.colorize(Objects.requireNonNull(DragonEvent.getInstance().getConfig().getString("votifier.settings.bossbar.text"))
                        .replace("%votes%", String.valueOf(VoteManager.getINSTANCE().getVotes()))
                        .replace("%goal%", String.valueOf(VoteManager.getINSTANCE().getVotesNeeded()))
                ));
                bossBar.setVisible(true);
                Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);
            } else {
                bossBar.setVisible(false);
                Bukkit.getOnlinePlayers().forEach(bossBar::removePlayer);
            }
        }, 5, 5);
    }

    @EventHandler
    public void onVote(VotifierEvent event) {
        DatabaseConnection connection = DragonEvent.getInstance().getConnection();
        YamlConfiguration config = DragonEvent.getInstance().getConfig();
        String playerName = event.getVote().getUsername();
        Player p = Bukkit.getPlayer(playerName);
        if (p == null) {
            boolean offlineVotes = config.getBoolean("votifier.settings.offline-votes");
            if (!offlineVotes) return;
        }
        if (config.getBoolean("votifier.settings.announce-votes.enable")) {
            Bukkit.getOnlinePlayers().forEach(player -> {
                if (!player.getName().equals(playerName)) {
                    List<String> messages = config.getStringList("votifier.settings.announce-votes.message");
                    for (String message : messages) {
                        player.sendMessage(ColorHelper.colorize(message
                                .replace("%player%", playerName)
                                .replace("%service%", event.getVote().getServiceName())
                                .replace("%address%", event.getVote().getAddress())
                                .replace("%time%", event.getVote().getTimeStamp())));
                    }
                }
            });
        }

        if (config.getBoolean("votifier.settings.vote-reward.enable")) {
            List<String> commands = config.getStringList("votifier.settings.vote-reward.commands");
            for (String command : commands) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", playerName));
            }
        }

        connection.update(connection.getInfo().isSqlLite() ? "INSERT OR IGNORE INTO de_votes (player, votes, last_vote) VALUES (?,?,?)" : "INSERT IGNORE INTO de_votes (player, votes, last_vote) VALUES (?,?,?)", playerName, 1, System.currentTimeMillis());
        connection.update("UPDATE de_votes SET votes = votes + 1, last_vote = ? WHERE player = ?", System.currentTimeMillis(), playerName);

        DePlayer dePlayer = EventManager.getINSTANCE().getPlayerMap().get(playerName);
        dePlayer.setVotes(dePlayer.getVotes() + 1);
        dePlayer.setLastVote(System.currentTimeMillis());
        EventManager.getINSTANCE().getPlayerMap().put(playerName, dePlayer);
        VoteManager.getINSTANCE().incrementVotes();
    }

    private void increaseHue() {
        this.hue += 10;
        this.hue %= 360;
        this.rgb = java.awt.Color.getHSBColor((float) this.hue / 360.0F, 1.0F, 1.0F);
    }

    public BossBar getBossBar() {
        return bossBar;
    }
}
