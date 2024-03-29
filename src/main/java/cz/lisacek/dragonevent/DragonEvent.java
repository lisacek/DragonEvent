package cz.lisacek.dragonevent;

import cz.lisacek.dragonevent.commands.*;
import cz.lisacek.dragonevent.cons.DePlayer;
import cz.lisacek.dragonevent.cons.Dragon;
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
import cz.lisacek.dragonevent.utils.ConfigUpdater;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.SQLException;
import java.util.*;

public final class DragonEvent extends JavaPlugin {

    private static DragonEvent instance;
    private YamlConfiguration config;

    private VotifierListener listener;

    private DatabaseConnection connection;

    private Map<String, Integer> top10votes;
    private Map<String, Integer> top10kills;
    private Map<String, Double> top10damage;

    private Map<String, List<String>> voteServices = new HashMap<>();

    private int votes = 0;


    @Override
    public void onEnable() {
        instance = this;
        // Plugin startup logic
        loadConfig();
        ConfigUpdater.run();
        Objects.requireNonNull(getCommand("dragonevent")).setExecutor(new DragonEventCommand());
        getServer().getPluginManager().registerEvents(new EventsListener(), this);

        Console.info("&7Connecting to the database...");
        if (Objects.requireNonNull(config.getString("database.type")).equalsIgnoreCase("sqlite")) {
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
            Console.info("&7Connected to the database!");
        } catch (Exception e) {
            e.printStackTrace();
            Console.info("&cError while connecting to the database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Create database tables if they don't exist
        createDatabaseTables();

        // Load player data from the database
        loadPlayerData();

        // Load top players
        loadTopPlayers();

        // Check for PlaceholderAPI
        checkPlaceholderAPI();

        // Check for Votifier plugin
        checkVotifier();

        // Schedule tasks
        scheduleTasks();

        // Start metrics
        Console.info("&7Starting metrics...");
        new Metrics(this, 18892);

        Console.info("&7Plugin &aenabled&7!");
    }

    private void autoSpawn() {
        YamlConfiguration config = DragonEvent.getInstance().getConfig();
        if (!config.getBoolean("dragon.auto-spawn.enable", false)) return;
        EventManager.getINSTANCE().loadLocations();
        EventManager.getINSTANCE().autoSpawn(config);
    }

    private void createDatabaseTables() {
        Console.info("&7Creating database tables...");
        boolean isSqlite = connection.getInfo().isSqlLite();

        String statsTableQuery = isSqlite ?
                "CREATE TABLE IF NOT EXISTS de_stats (" +
                        "    id INTEGER CONSTRAINT de_stats_pk PRIMARY KEY AUTOINCREMENT," +
                        "    player VARCHAR(90) CONSTRAINT de_stats_pk2 UNIQUE," +
                        "    kills INTEGER," +
                        "    damage BIGINT" +
                        ");;" :
                "CREATE TABLE IF NOT EXISTS de_stats (" +
                        "    id INTEGER PRIMARY KEY AUTO_INCREMENT," +
                        "    player VARCHAR(90) UNIQUE," +
                        "    kills INTEGER," +
                        "    damage BIGINT" +
                        ");";

        String votesTableQuery = isSqlite ?
                "CREATE TABLE IF NOT EXISTS de_votes (" +
                        "    id INTEGER CONSTRAINT de_votes_pk PRIMARY KEY AUTOINCREMENT," +
                        "    player VARCHAR(90) CONSTRAINT de_votes_pk2 UNIQUE," +
                        "    votes INTEGER," +
                        "    last_vote BIGINT" +
                        ");" :
                "CREATE TABLE IF NOT EXISTS de_votes (" +
                        "    id INTEGER PRIMARY KEY AUTO_INCREMENT," +
                        "    player VARCHAR(90) UNIQUE," +
                        "    votes INTEGER," +
                        "    last_vote BIGINT" +
                        ");";

        String offlineVotes = isSqlite ? "create TABLE IF NOT EXISTS de_offline_votes" +
                "(" +
                "    id       integer" +
                "        constraint offline_votes_pk" +
                "            primary key autoincrement," +
                "    username varchar(16)," +
                "    service  varchar(255)," +
                "    time     bigint" +
                ");" : "create TABLE IF NOT EXISTS de_offline_votes" +
                "(" +
                "    id       integer primary key auto_increment," +
                "    username varchar(16)," +
                "    service  varchar(255)," +
                "    time     bigint" +
                ");";

        String voteparty = isSqlite ? "create TABLE IF NOT EXISTS de_vote_party" +
                    "(" +
                    "    id  integer" +
                    "        constraint de_vote_party_pk" +
                    "            primary key," +
                    "    num integer" +
                    ");" :
                "create TABLE IF NOT EXISTS de_vote_party" +
                        "(" +
                        "    id  integer primary key," +
                        "    num integer" +
                        ");";

        connection.update(statsTableQuery);
        connection.update(votesTableQuery);
        connection.update(offlineVotes);
        connection.update(voteparty);
        Console.info("&7Database tables created!");
    }

    public void loadPlayerData() {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            Console.info("&7Loading player data from the database...");

            Bukkit.getOnlinePlayers().forEach(p -> DragonEvent.getInstance().getConnection().query(
                    "SELECT * FROM de_stats JOIN de_votes ON de_votes.player = ? WHERE de_stats.player = ?",
                    p.getName(), p.getName()
            ).thenAcceptAsync(rs -> {
                try {
                    DePlayer dePlayer = new DePlayer(p.getName());
                    if (rs.next()) {
                        dePlayer.setKills(rs.getInt("kills"));
                        dePlayer.setDamage(rs.getDouble("damage"));
                        dePlayer.setVotes(rs.getInt("votes"));
                        dePlayer.setLastVote(rs.getLong("last_vote"));
                    }
                    EventManager.getINSTANCE().getPlayerMap().put(p.getName(), dePlayer);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }));

            connection.update(connection.getInfo().isSqlLite()  ? "INSERT OR IGNORE INTO de_vote_party (id, num) VALUES (1, 0)"
                    :  "INSERT IGNORE INTO de_vote_party (id, num) VALUES (1, 0)");
            connection.query("SELECT * FROM de_vote_party WHERE id = 1").thenAcceptAsync(rs -> {
                try {
                    if (rs.next()) {
                       VoteManager.getINSTANCE().setVotes(rs.getInt("num"));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });

            Console.info("&7Player data loaded!");
        }, 1);
    }

    //

    public void loadTopPlayers() {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            Console.info("&7Loading top players...");

            top10votes = VoteManager.getINSTANCE().getTop10votes();
            top10kills = VoteManager.getINSTANCE().getTop10kills();
            top10damage = VoteManager.getINSTANCE().getTop10damage();

            Console.info("&7Top players loaded!");
        }, 1);
    }

    //reset top
    public void resetTopPlayers() {
        top10votes.clear();
        top10kills.clear();
        top10damage.clear();
    }

    private void checkPlaceholderAPI() {
        Console.info("&7Checking for PlaceholderAPI...");
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderManager().register();
            Console.info("&7PlaceholderAPI found, placeholders registered!");
        } else {
            Console.info("&cPlaceholderAPI not found, placeholders unavailable!");
        }
    }

    private void checkVotifier() {
        Console.info("&7Checking for Votifier...");
        if (getServer().getPluginManager().getPlugin("Votifier") != null) {
            String message = "&7Votifier plugin found, ";
            if (config.getBoolean("votifier.settings.enable")) {
                message += "support enabled. &a:)!";
                listener = new VotifierListener();
                getServer().getPluginManager().registerEvents(listener, this);
                if (config.getBoolean("vote-command.enable")) {
                    Objects.requireNonNull(getCommand("vote")).setExecutor(new VoteCommand());
                }
                if (config.getBoolean("vote-top.enable")) {
                    Objects.requireNonNull(getCommand("votetop")).setExecutor(new VoteTopCommand());
                }
                if (config.getBoolean("votifier.settings.reminder.enable")) {
                    VoteManager.getINSTANCE().reminderTask();
                }
                if (config.getBoolean("votifier.settings.offline-votes", false)) {
                    getCommand("offlinewards").setExecutor(new OfflineRewardsCommand());
                    offlineVotesCleaner();
                    getCommand("resetvotes").setExecutor(new ResetVotesCommand());
                    getCommand("modifyvotes").setExecutor(new ModifyVotesCommand());
                }
                if (config.getBoolean("votifier.settings.vote-reward.services.enable", false)) {
                    config.getConfigurationSection("votifier.settings.vote-reward.services").getKeys(false).forEach(service -> {
                        if (service.equalsIgnoreCase("enable")) return;
                        voteServices.put(config.getString("votifier.settings.vote-reward.services." + service + ".name"), config.getStringList("votifier.settings.vote-reward.services." + service + ".commands"));
                    });
                    Console.info("&7Vote reward services loaded! (&a" + voteServices.size() + "&7)");
                }
            } else {
                message += "but votifier function is disabled. &c:(!";
            }
            Console.info(message);
        } else {
            Console.info("&cVotifier plugin not found, function is unavailable!");
        }
    }

    private void offlineVotesCleaner() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            int deleteOlderThan = config.getInt("votifier.settings.offline-votes-delete-after", 604800);
            if (deleteOlderThan != 0) {
                long currentTimeMillis = System.currentTimeMillis();
                long olderThanMillis = currentTimeMillis - (deleteOlderThan * 1000L);
                connection.update("DELETE FROM de_offline_votes WHERE time < ?", olderThanMillis);
            }
        }, 0, 72000);
    }

    private void scheduleTasks() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            top10votes = VoteManager.getINSTANCE().getTop10votes();
            top10kills = VoteManager.getINSTANCE().getTop10kills();
            top10damage = VoteManager.getINSTANCE().getTop10damage();
        }, 20 * 60, 20 * 60);
        Bukkit.getScheduler().runTaskTimerAsynchronously(getInstance(), new UpdateChecker(), 0L, 1728000L);
        autoSpawn();
    }

    public DatabaseConnection getConnection() {
        return connection;
    }

    //get top on position
    public Pair<String, Object> getTopVotes(int position) {
        return getPair(position, top10votes);
    }

    public Pair<String, Object> getTopKills(int position) {
        return getPair(position, top10kills);
    }

    public Pair<String, Object> getTopDamage(int position) {
        List<Map.Entry<String, Double>> sortedEntries = new ArrayList<>(top10damage.entrySet());
        sortedEntries.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        if (position <= sortedEntries.size()) {
            Map.Entry<String, Double> entry = sortedEntries.get(position - 1);
            return new Pair<>(entry.getKey(), entry.getValue());
        }

        return new Pair<>("none", 0.0d);
    }

    private Pair<String, Object> getPair(int position, Map<String, Integer> top10kills) {
        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(top10kills.entrySet());
        sortedEntries.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        if (position <= sortedEntries.size()) {
            Map.Entry<String, Integer> entry = sortedEntries.get(position - 1);
            return new Pair<>(entry.getKey(), entry.getValue());
        }

        return new Pair<>("none", 0);
    }


    @Override
    public void onDisable() {
        if (listener != null) listener.getBossBar().removeAll();
        if (connection != null) connection.close();
        EventManager.getINSTANCE().getDragonList().forEach(Dragon::remove);
        Console.info("&7Plugin &cdisabled&7!");
    }


    public Map<String, Integer> getTop10() {
        return top10votes;
    }

    public void loadConfig() {
        try {
            File file = new File(getDataFolder(), "config.yml");
            if (!file.exists()) {
                boolean success = file.getParentFile().mkdirs();
                if (!success) {
                    Console.info("&cFailed to create config.yml file!");
                }
                saveResource("config.yml", false);
            }
            config = new YamlConfiguration();
            config.load(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public @NotNull YamlConfiguration getConfig() {
        return config;
    }

    public static DragonEvent getInstance() {
        return instance;
    }
}
