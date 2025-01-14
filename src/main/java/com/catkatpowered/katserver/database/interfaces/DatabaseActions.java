package com.catkatpowered.katserver.database.interfaces;

import java.util.List;

/**
 * DatabaseActions 接口和接口实现类是解析注解对象
 *
 * @author hanbings
 */
public interface DatabaseActions {
    // 增加一行数据
    <T> void create(DatabaseConnection connection, String table, T data);
    // 删除一行数据
    <T> void delete(DatabaseConnection connection, String table, T data);
    // 查询一组数据
    <T> List<T> read(DatabaseConnection connection, String table, T data);
    // 更新一行数据
    <T> void update(DatabaseConnection connection, String table, T data);
}
