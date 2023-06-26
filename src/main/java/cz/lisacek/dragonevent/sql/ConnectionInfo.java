package cz.lisacek.dragonevent.sql;

import org.bukkit.configuration.file.YamlConfiguration;

public class ConnectionInfo {
    private final String host;
    private final int port;
    private final String user;
    private final String password;
    private final String database;
    private final boolean sqlLite;

    public ConnectionInfo(String host, int port, String user, String password, String database) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.database = database;
        this.sqlLite = false;
    }

    public ConnectionInfo(String database) {
        this.host = "";
        this.port = 3306;
        this.user = "";
        this.password = "";
        this.database = database;
        this.sqlLite = true;
    }

    public boolean isSqlLite() {
        return sqlLite;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getHost() {
        return host;
    }

    public String getPassword() {
        return password;
    }

    public String getUser() {
        return user;
    }

    @Override
    public String toString() {
        return user + ":" + password + "@" + host + ":" + port + "/" + database;
    }
}