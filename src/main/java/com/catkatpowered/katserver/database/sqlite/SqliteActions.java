package com.catkatpowered.katserver.database.sqlite;

import com.catkatpowered.katserver.database.interfaces.DatabaseActions;
import com.catkatpowered.katserver.database.interfaces.DatabaseConnection;

import java.util.List;

public class SqliteActions implements DatabaseActions {

    @Override
    public void create(DatabaseConnection connection, String table, Object data) {

    }

    @Override
    public void delete(DatabaseConnection connection, String table, Object data) {

    }

    @Override
    public <T> List<T> read(DatabaseConnection connection, String table, T data) {
        return null;
    }

    @Override
    public void update(DatabaseConnection connection, String table, Object data) {

    }
}