package com.catkatpowered.katserver.database;

import com.catkatpowered.katserver.KatServer;
import com.catkatpowered.katserver.database.interfaces.DatabaseActions;
import com.catkatpowered.katserver.database.interfaces.DatabaseConnector;
import com.catkatpowered.katserver.database.interfaces.DatabaseType;
import com.catkatpowered.katserver.database.sqlite.SqliteActions;
import com.catkatpowered.katserver.database.sqlite.SqliteConnector;
import lombok.Getter;

public class KatDatabase {

    private static final KatDatabase Instance = new KatDatabase();

    // 这会还得 Controller 暂存连接器
    @Getter
    private final DatabaseConnector connector;
    @Getter
    private final DatabaseActions actions;


    private KatDatabase() {

        // 加载数据库
        // 读配置文件
        DatabaseType type = DatabaseType.lookup(
            (String) KatServer.KatConfigAPI.getConfig("database_type"));
        String url = (String) KatServer.KatConfigAPI.getConfig("database_url");
        String username = (String) KatServer.KatConfigAPI.getConfig("database_username");
        String password = (String) KatServer.KatConfigAPI.getConfig("database_password");

        connector = pickConnector(type);
        actions = pickActions(type);

        connector.loadDatabase(url, username, password);
    }

    public static KatDatabase getInstance() {
        return Instance;
    }

    /**
     * 根据数据库类型返回实例
     */
    DatabaseConnector pickConnector(DatabaseType type) {
        switch (type) {
            case MySQL, PostgreSQL, MongoDB -> {
            }
            case SQLite -> {
                return new SqliteConnector();
            }
        }
        return null;
    }

    DatabaseActions pickActions(DatabaseType type) {
        switch (type) {
            case MySQL, PostgreSQL, MongoDB -> {
            }
            case SQLite -> {
                return new SqliteActions();
            }
        }
        return null;
    }
}