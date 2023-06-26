package cz.basicland.blibs.shared.databases.hikari;

import cz.basicland.blibs.shared.dataholder.Config;
import lombok.Getter;

@Getter
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

    public static ConnectionInfo load(Config config, String name) {
        if (config == null)
            return null;

        ConnectionInfo info;
        if (name.endsWith(".db")) {
            info = new ConnectionInfo(name);
            return info;
        }

        if (!name.contains("Databases.")) {
            info = new ConnectionInfo(
                    config.getString(name + ".Host"),
                    config.getInt(name + ".Port", 3306),
                    config.getString(name + ".User"),
                    config.getString(name + ".Pass"),
                    config.getString(name + ".DB")
            );
        } else {
            info = new ConnectionInfo(
                    config.getString("Databases." + name + ".Host"),
                    config.getInt("Databases." + name + ".Port", 3306),
                    config.getString("Databases." + name + ".User"),
                    config.getString("Databases." + name + ".Pass"),
                    config.getString("Databases." + name + ".DB")
            );
        }

        return info;
    }

    @Override
    public String toString() {
        return user + ":" + password + "@" + host + ":" + port + "/" + database;
    }
}