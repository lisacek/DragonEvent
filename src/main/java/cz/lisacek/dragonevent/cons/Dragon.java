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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class Dragon {

    private Map<Player, Double> damageMap = new HashMap<>();

    private final EnderDragon dragon;

    private final BukkitTask isDead;
    private final BukkitTask announceHp;
    private BossBar countdownBossBar;
    private int hue = 0;
    private Color rgb = java.awt.Color.getHSBColor((float) this.hue / 360.0F, 1.0F, 1.0F);
    private String hex = String.format("<#%02X%02X%02X>", 0, 0, 0);
    private final double maxHealth;
    private final boolean isMoving;

    private final Location clone;

    private final DecimalFormat DF = new DecimalFormat("#.##");

    public Dragon(EnderDragon dragon, double maxHealth, boolean isMoving) {
        this.isMoving = isMoving;
        YamlConfiguration config = DragonEvent.getInstance().getConfig();
        this.dragon = dragon;
        this.isDead = isDead();
        this.clone = dragon.getLocation().clone();
        this.announceHp = announceHp();
        if(!isMoving) {
            Bukkit.getScheduler().runTaskTimer(DragonEvent.getInstance(), (task) -> {
                if(dragon.getHealth() == 1) {
                    dragon.setHealth(0);
                    task.cancel();
                } else {
                    keepLocation();
                }
            }, 0, 1L);
        }

        if (config.getBoolean("bossbar.enable")) {
            if (!config.getBoolean("bossbar.rainbow")) {
                BarColor color = BarColor.valueOf(config.getString("bossbar.color"));
                this.countdownBossBar = Bukkit.createBossBar(ColorHelper.colorize(config.getString("bossbar.text")
                        .replace("%dragon_health%", String.valueOf(dragon.getHealth()))
                        .replace("%max_health%", String.valueOf(maxHealth))), color, BarStyle.SOLID);
            } else {
                this.countdownBossBar = Bukkit.createBossBar(ColorHelper.colorize(config.getString("bossbar.text")
                        .replace("%dragon_health%", String.valueOf(dragon.getHealth()))
                        .replace("%max_health%", String.valueOf(maxHealth))), BarColor.BLUE, BarStyle.SOLID);
            }
            this.countdownBossBar.setProgress(1.0D);
            this.countdownBossBar.setVisible(true);
            Bukkit.getOnlinePlayers().forEach(this.countdownBossBar::addPlayer);
        }
        this.maxHealth = maxHealth;
    }

    public BukkitTask isDead() {
        return Bukkit.getScheduler().runTaskTimer(DragonEvent.getInstance(), () -> {
            if (dragon.isDead()) {
                if (dragon.getAttribute(Attribute.GENERIC_FLYING_SPEED) != null) {
                    dragon.getAttribute(Attribute.GENERIC_FLYING_SPEED).setBaseValue(0);
                }
                this.isDead.cancel();
                if (countdownBossBar != null)
                    countdownBossBar.removeAll();
                if (dragon.getKiller() == null) return;

                DePlayer dePlayer = EventManager.getINSTANCE().getPlayerMap().get(dragon.getKiller().getName());
                dePlayer.setKills(dePlayer.getKills() + 1);
                EventManager.getINSTANCE().getPlayerMap().put(dragon.getKiller().getName(), dePlayer);

                if (DragonEvent.getInstance().getConnection().getInfo().isSqlLite()) {
                    DragonEvent.getInstance().getConnection().update("INSERT OR IGNORE INTO `de_stats` (`player`, `kills`, `damage`) VALUES (?, 0, ?)", dragon.getKiller().getName(), damageMap.get(dragon.getKiller()));
                    DragonEvent.getInstance().getConnection().update("UPDATE `de_stats` SET `kills` = `kills` + 1, `damage` = `damage` + ? WHERE `player` = ?", damageMap.get(dragon.getKiller()), dragon.getKiller().getName());
                } else {
                    DragonEvent.getInstance().getConnection().update("INSERT IGNORE INTO `de_stats` (`player`, `kills`, `damage`) VALUES (?, 0, ?)", dragon.getKiller().getName(), damageMap.get(dragon.getKiller()));
                    DragonEvent.getInstance().getConnection().update("UPDATE `de_stats` SET `kills` = `kills` + 1, `damage` = `damage` + ? WHERE `player` = ?", damageMap.get(dragon.getKiller()), dragon.getKiller().getName());
                }

                getDamageMap().forEach((p, d) -> {
                    if (p.getName().equals(dragon.getKiller().getName())) return;
                    if (DragonEvent.getInstance().getConnection().getInfo().isSqlLite()) {
                        DragonEvent.getInstance().getConnection().update("INSERT OR IGNORE INTO `de_stats` (`player`, `kills`, `damage`) VALUES (?, 0, ?)", dragon.getKiller().getName(), damageMap.get(dragon.getKiller()));
                        DragonEvent.getInstance().getConnection().update("UPDATE `de_stats` SET ``damage` = `damage` + ? WHERE `player` = ?", damageMap.get(dragon.getKiller()), dragon.getKiller().getName());
                    } else {
                        DragonEvent.getInstance().getConnection().update("INSERT IGNORE INTO `de_stats` (`player`, `kills`, `damage`) VALUES (?, 0, ?)", dragon.getKiller().getName(), damageMap.get(dragon.getKiller()));
                        DragonEvent.getInstance().getConnection().update("UPDATE `de_stats` SET `damage` = `damage` + ? WHERE `player` = ?", damageMap.get(dragon.getKiller()), dragon.getKiller().getName());
                    }
                    DePlayer dP = EventManager.getINSTANCE().getPlayerMap().get(p.getName());
                    dP.setDamage((long) (dP.getDamage() + d));
                    EventManager.getINSTANCE().getPlayerMap().put(p.getName(), dP);
                });

                sortPlayers();
                distributeRewards();
                announceWinners();
                YamlConfiguration config = DragonEvent.getInstance().getConfig();

                if (config.getBoolean("titles.killed.enable")) {
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        player.sendTitle(ColorHelper.colorize(DragonEvent.getInstance().getConfig().getString("titles.killed.title")), ColorHelper.colorize(DragonEvent.getInstance().getConfig().getString("titles.killed.subtitle")),
                                DragonEvent.getInstance().getConfig().getInt("titles.killed.fadein"),
                                DragonEvent.getInstance().getConfig().getInt("titles.killed.stay"),
                                DragonEvent.getInstance().getConfig().getInt("titles.killed.fadeout"));
                        List<String> messages = DragonEvent.getInstance().getConfig().getStringList("announcements.killed.message");
                        for (String message : messages) {
                            player.sendMessage(ColorHelper.colorize(message.replace(
                                    "%player%",
                                    dragon.getKiller().getName()
                            )));
                        }
                    });
                }
            }
            YamlConfiguration config = DragonEvent.getInstance().getConfig();
            if (countdownBossBar != null) {
                if (config.getBoolean("bossbar.rainbow")) {
                    increaseHue();
                    countdownBossBar.setColor(BarColor.valueOf(BossBarColorHelper.getColorNameFromRgb(this.rgb.getRed(), this.rgb.getGreen(), this.rgb.getBlue())));
                }
            }
            if (config.getBoolean("dragon.glow.rainbow")) {
                //random ChatColor except for RESET, MAGIC, BOLD, WHITE, BLACK, UNDERLINE, STRIKETHROUGH, ITALIC, OBFUSCATED, GRAY, DARK_GRAY
                GlowHelper.setGlowing(dragon, null);
            }
        }, 5, 5);
    }

    public BukkitTask announceHp() {
        YamlConfiguration config = DragonEvent.getInstance().getConfig();
        if (!config.getBoolean("bossbar.enable") && !config.getBoolean("actionbar.enable")) return null;
        return Bukkit.getScheduler().runTaskTimer(DragonEvent.getInstance(), () -> {
            dragon.getNearbyEntities(50, 50, 50).forEach(entity -> {
                if (config.getBoolean("actionbar.enable")) {
                    if (entity instanceof Player) {
                        Player player = (Player) entity;
                        String text = config.getString("actionbar.text");
                        boolean damagePlayer = damageMap.containsKey(player);
                        if (damagePlayer) {
                            text = text
                                    .replace("%dragon_health%", DF.format(dragon.getHealth()))
                                    .replace("%dealt_damage%", DF.format(damageMap.get(player)));
                        } else {
                            text = text
                                    .replace("%dragon_health%", DF.format(dragon.getHealth()))
                                    .replace("%dealt_damage%", "0");
                        }
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ColorHelper.colorize(text)));
                    }
                }
                if (dragon.isDead()) {
                    this.announceHp.cancel();
                }
                if (countdownBossBar != null) {
                    //set countdownBossBar base on dragon health
                    countdownBossBar.setProgress(dragon.getHealth() / maxHealth);
                    //rename  base on dragon health
                    countdownBossBar.setTitle(ColorHelper.colorize(config.getString("bossbar.text")
                            .replace("%dragon_health%", DF.format(dragon.getHealth()))
                            .replace("%max_health%", DF.format(maxHealth))));
                }
            });
        }, 1, 1);
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
                    if (i < config.getConfigurationSection("rewards.top").getKeys(false).size()) {
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
        if (!config.getBoolean("rewards.enable")) return;
        AtomicInteger x = new AtomicInteger(1);
        damageMap.keySet().forEach(player -> {
            List<String> commands = config.getStringList("rewards.top." + x.getAndIncrement());
            for (String command : commands) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
            }

            if (config.getBoolean("rewards.regular.enable") && player.hasPermission(config.getString("rewards.regular.permission"))) {
                int chance = config.getInt("rewards.regular.chance");
                if (ThreadLocalRandom.current().nextInt(100) > chance) {
                    List<String> regular = config.getStringList("rewards.regular.rewards");
                    for (String command : regular) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
                    }
                }
            }

            if (config.getBoolean("rewards.extra.enable") && player.hasPermission(config.getString("rewards.extra.permission"))) {
                int chance = config.getInt("rewards.extra.chance");
                if (ThreadLocalRandom.current().nextInt(100) > chance) {
                    List<String> extra = config.getStringList("rewards.extra.rewards");
                    for (String command : extra) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
                    }
                }
            }
        });
    }

    public Map<Player, Double> getDamageMap() {
        return damageMap;
    }

    public EnderDragon getDragon() {
        return dragon;
    }

    public boolean isMoving() {
        return isMoving;
    }

    public void keepLocation() {
        dragon.teleport(clone);
    }

    private void increaseHue() {
        this.hue += 10;
        this.hue %= 360;
        this.rgb = java.awt.Color.getHSBColor((float) this.hue / 360.0F, 1.0F, 1.0F);
        this.hex = String.format("<#%02X%02X%02X>", this.rgb.getRed(), this.rgb.getGreen(), this.rgb.getBlue());
    }
}
