package cz.lisacek.dragonevent.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import cz.lisacek.dragonevent.DragonEvent;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public class DatabaseConnection {

    private final ConnectionInfo info;

    private HikariDataSource hikari;

    public DatabaseConnection(ConnectionInfo info) {
        this.info = info;
    }

    public void connect() {
        if (!info.isSqlLite()) {
            this.hikari = new HikariDataSource();
            hikari.setDataSourceClassName("com.mysql.cj.jdbc.MysqlDataSource");
            hikari.addDataSourceProperty("serverName", info.getHost());
            hikari.addDataSourceProperty("port", info.getPort());
            hikari.addDataSourceProperty("databaseName", info.getDatabase());
            hikari.addDataSourceProperty("user", info.getUser());
            hikari.addDataSourceProperty("password", info.getPassword());
            hikari.addDataSourceProperty("characterEncoding", "utf8");
        } else {
            //check if file exists
            File file = new File(DragonEvent.getInstance().getDataFolder() + "/" + info.getDatabase());
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            HikariConfig config = new HikariConfig();
            config.setDriverClassName("org.sqlite.JDBC");
            config.setJdbcUrl("jdbc:sqlite:" + DragonEvent.getInstance().getDataFolder() + "/" + info.getDatabase());
            this.hikari = new HikariDataSource(config);
        }

        try {
            hikari.validate();
        } catch (IllegalStateException e) {
            throw new IllegalStateException(e);
        }

        Connection connection;
        try {
            connection = hikari.getConnection();
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }

        if (connection == null)
            throw new IllegalStateException("No connection");
    }

    public CompletableFuture<Boolean> update(String query, Object... parameters) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = hikari.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {

                if (parameters != null) {
                    for (int i = 0; i < parameters.length; i++) {
                        statement.setObject(i + 1, parameters[i]);
                    }
                }

                statement.execute();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<ResultSet> query(String query, Object... parameters) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = hikari.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {

                if (parameters != null) {
                    for (int i = 0; i < parameters.length; i++) {
                        statement.setObject(i + 1, parameters[i]);
                    }
                }

                CachedRowSet resultCached = RowSetProvider.newFactory().createCachedRowSet();
                ResultSet resultSet = statement.executeQuery();

                resultCached.populate(resultSet);
                resultSet.close();

                return resultCached;
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return null;
        });
    }

    public void close() {
        if (hikari != null)
            hikari.close();
    }

    public ConnectionInfo getInfo() {
        return info;
    }

    public HikariDataSource getHikari() {
        return hikari;
    }
}