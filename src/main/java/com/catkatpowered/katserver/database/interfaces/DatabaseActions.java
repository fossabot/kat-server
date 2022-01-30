package com.catkatpowered.katserver.database.interfaces;

import java.util.List;

public interface DatabaseActions {
    // 增加一行数据
    void create(DatabaseConnection connection, String table, Object data);
    // 删除一行数据
    void delete(DatabaseConnection connection, String table, Object data);
    // 查询一组数据
    <T> List<T> read(DatabaseConnection connection, String table, T data);
    // 更新一行数据
    void update(DatabaseConnection connection, String table, Object data);
}