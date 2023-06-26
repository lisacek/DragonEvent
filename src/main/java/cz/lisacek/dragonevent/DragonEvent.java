package cz.lisacek.dragonevent;

import cz.lisacek.dragonevent.commands.DragonEventCommand;
import cz.lisacek.dragonevent.commands.VoteCommand;
import cz.lisacek.dragonevent.commands.VoteTopCommand;
import cz.lisacek.dragonevent.cons.DePlayer;
import cz.lisacek.dragonevent.cons.Pair;
import cz.lisacek.dragonevent.listeners.EventsListener;
import cz.lisacek.dragonevent.listeners.VotifierListener;
import cz.lisacek.dragonevent.managers.EventManager;
import cz.lisacek.dragonevent.managers.PlaceholderManager;
import cz.lisacek.dragonevent.managers.VoteManager;
import cz.lisacek.dragonevent.sql.ConnectionInfo;
import cz.lisacek.dragonevent.sql.DatabaseConnection;
import cz.lisacek.dragonevent.utils.Console;
import cz.lisacek.dragonevent.utils.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;
import java.util.Map;

public final class DragonEvent extends JavaPlugin {

    private static DragonEvent instance;
    private YamlConfiguration config;

    private VotifierListener listener;

    private DatabaseConnection connection;

    private Map<String, Integer> top10votes;
    private Map<String, Integer> top10kills;
    private Map<String, Long> top10damage;


    @Override
    public void onEnable() {
        instance = this;
        // Plugin startup logic
        loadConfig();
        getCommand("dragonevent").setExecutor(new DragonEventCommand());
        getServer().getPluginManager().registerEvents(new EventsListener(), this);

        Console.info("&7Connecting to database...");
        if (config.getString("database.type").equalsIgnoreCase("sqlite")) {
            ConnectionInfo info = new ConnectionInfo("database.db");
            connection = new DatabaseConnection(info);
        } else {
            ConnectionInfo info = new ConnectionInfo(
                    config.getString("database.host"),
                    config.getInt("database.port"),
                    config.getString("database.username"),
                    config.getString("database.password"),
                    config.getString("database.database")
            );
            connection = new DatabaseConnection(info);
        }
        try {
            connection.connect();
            Console.info("&7Connected to database!");
        } catch (Exception e) {
            e.printStackTrace();
            Console.info("&cError while connecting to database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (connection.getInfo().isSqlLite()) {
            connection.update("CREATE TABLE IF NOT EXISTS de_stats (" +
                    "    id INTEGER CONSTRAINT de_stats_pk PRIMARY KEY AUTOINCREMENT," +
                    "    player VARCHAR(90) CONSTRAINT de_stats_pk2 UNIQUE," +
                    "    kills INTEGER," +
                    "    damage BIGINT" +
                    ");;");
            connection.update("CREATE TABLE IF NOT EXISTS de_votes (" +
                    "    id INTEGER CONSTRAINT de_votes_pk PRIMARY KEY AUTOINCREMENT," +
                    "    player VARCHAR(90) CONSTRAINT de_votes_pk2 UNIQUE," +
                    "    votes INTEGER," +
                    "    last_vote BIGINT" +
                    ");");
        } else {
            connection.update("CREATE TABLE IF NOT EXISTS de_stats (" +
                    "    id INTEGER PRIMARY KEY AUTO_INCREMENT," +
                    "    player VARCHAR(90) UNIQUE," +
                    "    kills INTEGER," +
                    "    damage BIGINT" +
                    ");");
            connection.update("CREATE TABLE IF NOT EXISTS de_votes (" +
                    "    id INTEGER PRIMARY KEY AUTO_INCREMENT," +
                    "    player VARCHAR(90) UNIQUE," +
                    "    votes INTEGER," +
                    "    last_vote BIGINT" +
                    ");");
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            Bukkit.getOnlinePlayers().forEach(p -> {
                DragonEvent.getInstance().getConnection().query("SELECT * FROM de_stats JOIN de_votes ON de_votes.player = ? WHERE de_stats.player = ?", p.getName(), p.getName()).thenAcceptAsync(rs -> {
                    try {
                        if (rs.next()) {
                            DePlayer dePlayer = new DePlayer(p.getName());
                            dePlayer.setKills(rs.getInt("kills"));
                            dePlayer.setDamage(rs.getLong("damage"));
                            dePlayer.setVotes(rs.getInt("votes"));
                            dePlayer.setLastVote(rs.getLong("last_vote"));
                            EventManager.getINSTANCE().getPlayerMap().put(p.getName(), dePlayer);
                        } else {
                            DePlayer dePlayer = new DePlayer(p.getName());
                            EventManager.getINSTANCE().getPlayerMap().put(p.getName(), dePlayer);
                        }
                    } catch (SQLException e) {

                    }
                });
            });
            Console.info("&7Loading top players...");
            top10votes = VoteManager.getINSTANCE().getTop10votes();
            top10kills = VoteManager.getINSTANCE().getTop10kills();
            top10damage = VoteManager.getINSTANCE().getTop10damage();
            Console.info("&7Loaded top players!");
        }, 1);

        Console.info("&7Checking for PlaceholderAPI...");
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderManager().register();
            Console.info("&7PlaceholderAPI found, placeholders registered!");
        } else {
            Console.info("&cPlaceholderAPI not found, placeholders unavailable!");
        }
        Console.info("&7Checking for votifier...");
        //check if nuvotifier is installed
        if (getServer().getPluginManager().getPlugin("Votifier") != null) {
            String message = "&7Votifier plugin found, ";
            if (config.getBoolean("votifier.settings.enable")) {
                message += "support enabled. &a:)!";
                listener = new VotifierListener();
                getServer().getPluginManager().registerEvents(listener, this);
                if (config.getBoolean("vote-command.enable")) {
                    getCommand("vote").setExecutor(new VoteCommand());
                }
                if (config.getBoolean("vote-top.enable")) {
                    getCommand("votetop").setExecutor(new VoteTopCommand());
                }
                if (config.getBoolean("votifier.settings.reminder.enable")) VoteManager.getINSTANCE().reminderTask();
            } else {
                message += "but votifier function is disabled. &c:(!";
            }
            Console.info(message);
        } else {
            Console.info("&cVotifier plugin not found, function is unavailable!");
        }

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            top10votes = VoteManager.getINSTANCE().getTop10votes();
            top10kills = VoteManager.getINSTANCE().getTop10kills();
            top10damage = VoteManager.getINSTANCE().getTop10damage();
        }, 20 * 60, 20 * 60);
        Bukkit.getScheduler().runTaskTimerAsynchronously(getInstance(), new UpdateChecker(), 0L, 1728000L);
        Console.info("&7Plugin &aenabled&7!");
    }

    public DatabaseConnection getConnection() {
        return connection;
    }

    //get top on position
    public Pair<String, Object> getTopVotes(int position) {
        String top = "";
        int i = 1;
        for (Map.Entry<String, Integer> entry : top10votes.entrySet()) {
            if (i == position) {
                top = entry.getKey();
                break;
            }
            i++;
        }
        return new Pair<>(top, top10votes.get(top));
    }

    //get top on position kills
    public Pair<String, Object> getTopKills(int position) {
        String top = "";
        int i = 1;
        for (Map.Entry<String, Integer> entry : top10kills.entrySet()) {
            if (i == position) {
                top = entry.getKey();
                break;
            }
            i++;
        }
        return new Pair<>(top, top10kills.get(top));
    }

    //get top on position damage
    public Pair<String, Object> getTopDamage(int position) {
        String top = "";
        int i = 1;
        for (Map.Entry<String, Long> entry : top10damage.entrySet()) {
            if (i == position) {
                top = entry.getKey();
                break;
            }
            i++;
        }
        return new Pair<>(top, top10damage.get(top));
    }

    @Override
    public void onDisable() {
        if (listener != null) listener.getBossBar().removeAll();
        Console.info("&7Plugin &cdisabled&7!");
    }


    public Map<String, Integer> getTop10() {
        return top10votes;
    }

    public void loadConfig() {
        try {
            File file = new File(getDataFolder(), "config.yml");
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                saveResource("config.yml", false);
            }
            config = new YamlConfiguration();
            config.load(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public YamlConfiguration getConfig() {
        return config;
    }

    public static DragonEvent getInstance() {
        return instance;
    }
}
