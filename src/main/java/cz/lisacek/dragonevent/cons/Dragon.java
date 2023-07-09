package cz.lisacek.dragonevent.cons;

import cz.lisacek.dragonevent.DragonEvent;
import cz.lisacek.dragonevent.managers.EventManager;
import cz.lisacek.dragonevent.utils.BossBarColorHelper;
import cz.lisacek.dragonevent.utils.ColorHelper;
import cz.lisacek.dragonevent.utils.GlowHelper;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class Dragon {

    private final EnderDragon dragon;
    private final double maxHealth;
    private final Location clone;
    private final DecimalFormat DF = new DecimalFormat("#.##");
    private final BukkitTask isDead;
    private final BukkitTask announceHp;
    private BossBar countdownBossBar;
    private int hue = 0;
    private Map<Player, Double> damageMap = new HashMap<>();
    private Color rgb = Color.getHSBColor((float) hue / 360.0F, 1.0F, 1.0F);

    public Dragon(EnderDragon dragon, double maxHealth, boolean isMoving) {
        this.dragon = dragon;
        this.isDead = isDead();
        this.clone = dragon.getLocation().clone();
        this.announceHp = announceHp();
        if (!isMoving) {
            Bukkit.getScheduler().runTaskTimer(DragonEvent.getInstance(), (task) -> {
                if (dragon.getHealth() == 1) {
                    dragon.setHealth(0);
                    task.cancel();
                } else {
                    keepLocation();
                }
            }, 0, 1L);
        }
        initializeBossBar(maxHealth);
        this.maxHealth = maxHealth;
    }

    private void initializeBossBar(double maxHealth) {
        YamlConfiguration config = DragonEvent.getInstance().getConfig();
        if (config.getBoolean("bossbar.enable")) {
            String bossBarText = ColorHelper.colorize(Objects.requireNonNull(config.getString("bossbar.text"))
                    .replace("%dragon_health%", String.valueOf(dragon.getHealth()))
                    .replace("%max_health%", String.valueOf(maxHealth)));

            if (!config.getBoolean("bossbar.rainbow")) {
                BarColor color = BarColor.valueOf(config.getString("bossbar.color"));
                this.countdownBossBar = Bukkit.createBossBar(bossBarText, color, BarStyle.SOLID);
            } else {
                this.countdownBossBar = Bukkit.createBossBar(bossBarText, BarColor.BLUE, BarStyle.SOLID);
            }

            this.countdownBossBar.setProgress(1.0D);
            this.countdownBossBar.setVisible(true);
            Bukkit.getOnlinePlayers().forEach(this.countdownBossBar::addPlayer);
        }
    }

    public BukkitTask isDead() {
        return Bukkit.getScheduler().runTaskTimer(DragonEvent.getInstance(), () -> {
            if (dragon.isDead()) {
                handleDragonDeath();
            }
            updateCountdownBossBar();
            updateDragonGlow();
        }, 5, 5);
    }

    private void handleDragonDeath() {
        if (dragon.getAttribute(Attribute.GENERIC_FLYING_SPEED) != null) {
            Objects.requireNonNull(dragon.getAttribute(Attribute.GENERIC_FLYING_SPEED)).setBaseValue(0);
        }
        this.isDead.cancel();

        if (countdownBossBar != null) {
            countdownBossBar.removeAll();
        }

        if (dragon.isGlowing()) {
            GlowHelper.unregisterTeam(dragon);
        }

        Player killer = dragon.getKiller();
        if (killer == null) {
            return;
        }

        updatePlayerStats(killer, damageMap.get(killer));

        for (Map.Entry<Player, Double> entry : damageMap.entrySet()) {
            Player player = entry.getKey();
            if (player.getName().equals(killer.getName())) {
                continue;
            }
            updatePlayerStats(killer, entry.getValue());
        }

        sortPlayers();
        distributeRewards();
        announceWinners();
        YamlConfiguration config = DragonEvent.getInstance().getConfig();
        sendKilledTitles(config);
        sendKilledAnnouncements(config);
    }

    private void updatePlayerStats(Player player, double damage) {
        DePlayer dePlayer = EventManager.getINSTANCE().getPlayerMap().get(player.getName());
        dePlayer.setKills(dePlayer.getKills() + 1);
        dePlayer.setDamage(dePlayer.getDamage() + (long) damage);
        EventManager.getINSTANCE().getPlayerMap().put(player.getName(), dePlayer);

        String insertQuery = DragonEvent.getInstance().getConnection().getInfo().isSqlLite() ?
                "INSERT OR IGNORE INTO `de_stats` (`player`, `kills`, `damage`) VALUES (?, 0, ?)" :
                "INSERT IGNORE INTO `de_stats` (`player`, `kills`, `damage`) VALUES (?, 0, ?)";
        String updateQuery = DragonEvent.getInstance().getConnection().getInfo().isSqlLite() ?
                "UPDATE `de_stats` SET `kills` = `kills` + 1, `damage` = `damage` + ? WHERE `player` = ?" :
                "UPDATE `de_stats` SET `damage` = `damage` + ? WHERE `player` = ?";

        DragonEvent.getInstance().getConnection().update(insertQuery, player.getName(), damage);
        DragonEvent.getInstance().getConnection().update(updateQuery, damage, player.getName());
    }

    private void updateCountdownBossBar() {
        YamlConfiguration config = DragonEvent.getInstance().getConfig();
        if (countdownBossBar != null && config.getBoolean("bossbar.rainbow")) {
            increaseHue();
            countdownBossBar.setColor(BarColor.valueOf(BossBarColorHelper.getColorNameFromRgb(this.rgb.getRed(), this.rgb.getGreen(), this.rgb.getBlue())));
        }
    }

    private void updateDragonGlow() {
        YamlConfiguration config = DragonEvent.getInstance().getConfig();
        if (config.getBoolean("dragon.glow.rainbow")) {
            // Random ChatColor except for RESET, MAGIC, BOLD, WHITE, BLACK, UNDERLINE, STRIKETHROUGH, ITALIC, OBFUSCATED, GRAY, DARK_GRAY
            GlowHelper.setGlowing(dragon, null);
        }
    }

    private void sendKilledTitles(YamlConfiguration config) {
        if (config.getBoolean("titles.killed.enable")) {
            String title = ColorHelper.colorize(config.getString("titles.killed.title"));
            String subtitle = ColorHelper.colorize(config.getString("titles.killed.subtitle"));
            int fadeIn = config.getInt("titles.killed.fadein");
            int stay = config.getInt("titles.killed.stay");
            int fadeOut = config.getInt("titles.killed.fadeout");

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
            }
        }
    }

    private void sendKilledAnnouncements(YamlConfiguration config) {
        if (config.getBoolean("announcements.killed.enable")) {
            List<String> messages = config.getStringList("announcements.killed.message");

            for (Player player : Bukkit.getOnlinePlayers()) {
                for (String message : messages) {
                    String formattedMessage = message.replace("%player%", Objects.requireNonNull(dragon.getKiller()).getName());
                    player.sendMessage(ColorHelper.colorize(formattedMessage));
                }
            }
        }
    }

    public BukkitTask announceHp() {
        YamlConfiguration config = DragonEvent.getInstance().getConfig();
        if (!config.getBoolean("bossbar.enable") && !config.getBoolean("actionbar.enable")) {
            return null;
        }

        return Bukkit.getScheduler().runTaskTimer(DragonEvent.getInstance(), () -> dragon.getNearbyEntities(50, 50, 50).forEach(entity -> {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                if (config.getBoolean("actionbar.enable")) {
                    sendActionBarMessage(player, config);
                }
                if (dragon.isDead()) {
                    this.announceHp.cancel();
                }
                updateCountdownBossBar(config);
            }
        }), 1, 1);
    }

    private void sendActionBarMessage(Player player, YamlConfiguration config) {
        String actionBarText = config.getString("actionbar.text");
        boolean damagePlayer = damageMap.containsKey(player);
        String dealtDamage = damagePlayer ? DF.format(damageMap.get(player)) : "0";
        assert actionBarText != null;
        String formattedText = actionBarText
                .replace("%dragon_health%", DF.format(dragon.getHealth()))
                .replace("%dealt_damage%", dealtDamage);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ColorHelper.colorize(formattedText)));
    }

    private void updateCountdownBossBar(YamlConfiguration config) {
        if (countdownBossBar != null) {
            double healthPercentage = dragon.getHealth() / maxHealth;
            String bossBarText = config.getString("bossbar.text");
            assert bossBarText != null;
            String formattedText = bossBarText
                    .replace("%dragon_health%", DF.format(dragon.getHealth()))
                    .replace("%max_health%", DF.format(maxHealth));
            countdownBossBar.setProgress(healthPercentage);
            countdownBossBar.setTitle(ColorHelper.colorize(formattedText));
        }
    }

    private void sortPlayers() {
        YamlConfiguration config = DragonEvent.getInstance().getConfig();
        if (!config.getBoolean("announcements.top.enable")) return;
        damageMap = damageMap.entrySet().stream().sorted(Map.Entry.<Player, Double>comparingByValue().reversed()).collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, java.util.LinkedHashMap::new));
    }

    private void announceWinners() {
        YamlConfiguration config = DragonEvent.getInstance().getConfig();
        if (!config.getBoolean("announcements.top.enable")) return;
        String topLine = config.getString("announcements.top.positions");
        List<String> topAnnouncements = config.getStringList("announcements.top.message");
        for (String message : topAnnouncements) {
            if (message.contains("%positions%")) {
                int i = 0;
                for (Map.Entry<Player, Double> entry : damageMap.entrySet()) {
                    if (i < Objects.requireNonNull(config.getConfigurationSection("rewards.top")).getKeys(false).size()) {
                        assert topLine != null;
                        message = topLine
                                .replace("%pos%", String.valueOf(i + 1))
                                .replace("%player%", entry.getKey().getName())
                                .replace("%dealt_damage%", DF.format(entry.getValue()));
                        i++;
                        Bukkit.broadcastMessage(ColorHelper.colorize(message));
                    }
                }
            } else {
                Bukkit.broadcastMessage(ColorHelper.colorize(message));
            }
        }
    }

    private void distributeRewards() {
        YamlConfiguration config = DragonEvent.getInstance().getConfig();
        if (!config.getBoolean("rewards.enable")) {
            return;
        }

        AtomicInteger x = new AtomicInteger(1);
        damageMap.keySet().forEach(player -> {
            List<String> topRewards = config.getStringList("rewards.top." + x.getAndIncrement());
            executeCommands(topRewards, player);

            if (shouldGiveRegularReward(player, config)) {
                List<String> regularRewards = config.getStringList("rewards.regular.rewards");
                executeCommands(regularRewards, player);
            }

            if (shouldGiveExtraReward(player, config)) {
                List<String> extraRewards = config.getStringList("rewards.extra.rewards");
                executeCommands(extraRewards, player);
            }
        });
    }

    private void executeCommands(List<String> commands, Player player) {
        for (String command : commands) {
            String formattedCommand = command.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCommand);
        }
    }

    private boolean shouldGiveRegularReward(Player player, YamlConfiguration config) {
        return config.getBoolean("rewards.regular.enable") &&
                player.hasPermission(config.getString("rewards.regular.permission", "dragonevent.regular")) &&
                shouldExecuteChance(config.getInt("rewards.regular.chance"));
    }

    private boolean shouldGiveExtraReward(Player player, YamlConfiguration config) {
        return config.getBoolean("rewards.extra.enable") &&
                player.hasPermission(config.getString("rewards.extra.permission", "dragonevent.extra")) &&
                shouldExecuteChance(config.getInt("rewards.extra.chance"));
    }

    private boolean shouldExecuteChance(int chance) {
        return ThreadLocalRandom.current().nextInt(100) > chance;
    }

    public Map<Player, Double> getDamageMap() {
        return damageMap;
    }

    public EnderDragon getDragon() {
        return dragon;
    }

    public void keepLocation() {
        dragon.teleport(clone);
    }

    private void increaseHue() {
        this.hue += 10;
        this.hue %= 360;
        this.rgb = java.awt.Color.getHSBColor((float) this.hue / 360.0F, 1.0F, 1.0F);
    }

    public void remove() {
        if (countdownBossBar != null) {
            countdownBossBar.removeAll();
        }
        if (announceHp != null) {
            announceHp.cancel();
        }
        if (dragon != null) {
            dragon.setHealth(0);
            dragon.remove();
        }
    }
}