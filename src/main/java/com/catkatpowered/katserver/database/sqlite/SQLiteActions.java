package com.catkatpowered.katserver.database.sqlite;

import com.catkatpowered.katserver.database.annotation.SqliteMetadata;
import com.catkatpowered.katserver.database.interfaces.DatabaseActions;
import com.catkatpowered.katserver.database.interfaces.DatabaseConnection;
import com.catkatpowered.katserver.database.interfaces.DatabaseTypeTransfer;
import com.catkatpowered.katserver.database.type.ActionsType;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * 解析 sql 并存储为安全的预编译语句 <br>
 * 执行一句语句流程 <br>
 * 1. 初始化预编译缓存 <br>
 * 2. 解析实体类 注入参数 <br>
 * 3. 提交至数据库 <br>
 * <br>
 * <p>
 * 初始化预编译时需要 <br>
 * 1. 判断表是否存在 <br>
 * 2. 解析数据实体将 Java 数据类型动态推导为 sql 类型 <br>
 * 数据注解 -> 无注解自然语序数据
 * 读取到的第一个变量为主键 <br>
 * 3. 拼接 sql 进行预编译 <br>
 * <br>
 * <p>
 * 解析实体类 <br>
 * 1. 判断是否为主键类型的 CURD 仅有主键作为索引的实体类有预编译缓存 <br>
 * 2. 扫描注解 提取数据 <br>
 * 数据注解 -> 无注解自然语序数据
 * 读取到的第一个变量为主键 <br>
 * 3. 注入到预编译语句
 */
@Slf4j
public class SQLiteActions implements DatabaseActions {

    // 预编译缓存 key 为表名
    // value 为嵌套 Map
    // - 嵌套 Map key 为 CURD 中的一个操作 value 为预编译的 statement
    Map<String, Map<ActionsType, PreparedStatement>> mapping = new HashMap<>();
    // 动态类型推导器
    DatabaseTypeTransfer transfer = new SQLiteTypeTransfer();

    @Override
    public <T> void create(DatabaseConnection connection, String table, T data) {
        Connection jdbc = connection.getJdbcConnection();
        if (!validateTableExist(jdbc, table)) {
            // 创建表
            String sql = createTable(table, data);
            try {
                jdbc.prepareStatement(sql).execute();
            } catch (SQLException exception) {
                log.error(String.valueOf(exception));
            }
        }
        // 判断是否在 mapping 中缓存
        if (!mapping.containsKey(table)) {
            mapping.put(table, new HashMap<>());
        }
        if (!mapping.get(table).containsKey(ActionsType.CREATE)) {
            // 没有缓存 则创建预编译语句
            // 注入变量到预编译语句
            StringBuilder builder = new StringBuilder();
            builder.append("INSERT INTO ").append(table).append("VALUES(");
            for (int i = 0; i < data.getClass().getFields().length; i++) {
                if (i != 0) {
                    builder.append(", ");
                }
                builder.append("?");
            }
            builder.append(");");
            try {
                PreparedStatement statement = connection.getJdbcConnection()
                        .prepareStatement(builder.toString());
                mapping.get(table).put(ActionsType.CREATE, statement);
            } catch (SQLException exception) {
                log.error(String.valueOf(exception));
            }
        }
        // 命中缓存 注入变量到预编译语句
        PreparedStatement statement = this.injectInsertDataToPreparedStatement(
                mapping.get(table).get(ActionsType.CREATE), data);
        try {
            statement.executeQuery();
        } catch (SQLException exception) {
            log.error(String.valueOf(exception));
        }
    }

    @Override
    public <T> void delete(DatabaseConnection connection, String table, T data) {
        // 判断是否在 mapping 中缓存
        if (!mapping.containsKey(table)) {
            mapping.put(table, new HashMap<>());
        }
        if (!mapping.get(table).containsKey(ActionsType.DELETE)) {
            // 没有缓存 则创建预编译语句
            // 注入变量到预编译语句
            String sql = "DELETE FROM " + table + " WHERE ? = ?";
            try {
                PreparedStatement statement = connection.getJdbcConnection().prepareStatement(sql);
                mapping.get(table).put(ActionsType.DELETE, statement);
            } catch (SQLException exception) {
                log.error(String.valueOf(exception));
            }
        }
        Object[] values = this.validateFieldValueExist(data);
        if (values == null) {
            return;
        }
        // 命中缓存 注入变量到预编译语句
        PreparedStatement statement = this.injectDeleteDataToPreparedStatement(
                mapping.get(table).get(ActionsType.DELETE), values);
        try {
            statement.executeQuery();
        } catch (SQLException exception) {
            log.error(String.valueOf(exception));
        }
    }

    @Override
    public <T> List<T> read(DatabaseConnection connection, String table, T data) {
        // 判断是否在 mapping 中缓存
        if (!mapping.containsKey(table)) {
            mapping.put(table, new HashMap<>());
        }
        if (!mapping.get(table).containsKey(ActionsType.READ)) {
            // 没有缓存 则创建预编译语句
            // 注入变量到预编译语句
            String sql = "SELECT * FROM " + table + " WHERE ? = ?";
            try {
                PreparedStatement statement = connection.getJdbcConnection().prepareStatement(sql);
                mapping.get(table).put(ActionsType.READ, statement);
            } catch (SQLException exception) {
                log.error(String.valueOf(exception));
            }
        }
        Object[] values = this.validateFieldValueExist(data);
        if (values == null) {
            return null;
        }
        // 命中缓存 注入变量到预编译语句
        PreparedStatement statement = this.injectReadDataToPreparedStatement(mapping.get(table).get(ActionsType.READ),
                values);
        List<T> result = new ArrayList<>();
        try {
            ResultSet set = statement.executeQuery();
            // 注入结果集到对象
            while (set.next()) {
                result.add(injectResultSetToData(set, data));
            }
        } catch (SQLException exception) {
            log.error(String.valueOf(exception));
        }
        return result;
    }

    @Override
    public <T> void update(DatabaseConnection connection, String table, T data) {
        // 判断是否在 mapping 中缓存
        if (!mapping.containsKey(table)) {
            mapping.put(table, new HashMap<>());
        }
        if (!mapping.get(table).containsKey(ActionsType.UPDATE)) {
            StringBuilder builder = new StringBuilder("UPDATE ").append(table).append(" SET ");
            // 没有缓存 则创建预编译语句
            Field[] fields = data.getClass().getDeclaredFields();
            for (int count = 0; count < fields.length; count++) {
                Field field = fields[count];
                if (count != 0) {
                    builder.append(", ");
                }
                if (field.isAnnotationPresent(SqliteMetadata.class)) {
                    builder.append(field.getAnnotation(SqliteMetadata.class).name()).append(" = ?");
                }
            }
            builder.append(" WHERE ? = ?");
            try {
                PreparedStatement statement = connection.getJdbcConnection()
                        .prepareStatement(builder.toString());
                mapping.get(table).put(ActionsType.UPDATE, statement);
            } catch (SQLException exception) {
                log.error(String.valueOf(exception));
            }
        }
        // 命中缓存 注入变量到预编译语句
        PreparedStatement statement = this.injectUpdateDataToPreparedStatement(
                mapping.get(table).get(ActionsType.UPDATE)
                , data);
        try {
            statement.executeQuery();
        } catch (SQLException exception) {
            log.error(String.valueOf(exception));
        }
    }

    /**
     * 创建一行数据时注入变量到预编译语句
     *
     * @param statement 预编译语句
     * @param data      数据实体
     * @return 注入完成的语句
     */
    private PreparedStatement injectInsertDataToPreparedStatement(PreparedStatement statement,
            Object data) {
        Field[] fields = data.getClass().getDeclaredFields();
        for (int count = 0; count < fields.length; count++) {
            try {
                // 备注下 statement 的索引从 1 开始
                statement.setObject(count + 1, fields[count].get(data));
            } catch (SQLException | IllegalAccessException exception) {
                log.error(String.valueOf(exception));
            }
        }
        return statement;
    }

    /**
     * 删除某行数据时注入变量到预编译语句 只需要注入一个参数
     *
     * @param statement 预编译语句
     * @param data      列名和数据内容
     * @return 注入完成的语句
     */
    private PreparedStatement injectDeleteDataToPreparedStatement(PreparedStatement statement,
            Object[] data) {
        try {
            // 备注下 statement 的索引从 1 开始
            statement.setObject(1, data[0]);
            statement.setObject(2, data[1]);
        } catch (SQLException exception) {
            log.error(String.valueOf(exception));
        }
        return statement;
    }

    /**
     * 读取某行数据时注入变量到预编译语句
     */
    private PreparedStatement injectReadDataToPreparedStatement(PreparedStatement statement, Object[] data) {
        try {
            // 备注下 statement 的索引从 1 开始
            statement.setObject(1, data[0]);
            statement.setObject(2, data[1]);
        } catch (SQLException exception) {
            log.error(String.valueOf(exception));
        }
        return statement;
    }

    /**
     * 更新数据时注入变量到预编译语句
     *
     * @param statement 预编译语句
     * @param data      数据实体
     * @return 注入完成的语句
     */
    private PreparedStatement injectUpdateDataToPreparedStatement(PreparedStatement statement, Object data) {
        Field[] fields = data.getClass().getDeclaredFields();
        boolean havePrimaryKey = this.validatePrimaryKeyExist(fields);
        for (int count = 0; count < fields.length; count++) {
            Field field = fields[count];
            try {
                if (count == 0 && !havePrimaryKey) {
                    // 查询注解 查看是否有列名
                    if (field.isAnnotationPresent(SqliteMetadata.class)) {
                        statement.setObject(count, field.getAnnotation(SqliteMetadata.class).name());
                        statement.setObject(count + 1, field.get(data));
                    } else {
                        // 没有列名 则使用字段名
                        statement.setObject(count, transfer.getDataType(field.getName()));
                        statement.setObject(count + 1, field.get(data));
                    }
                }
                if (havePrimaryKey) {
                    SqliteMetadata metadata = field.getAnnotation(SqliteMetadata.class);
                    statement.setObject(fields.length, metadata.name());
                    statement.setObject(fields.length + 1, field.get(data));
                }
                // 备注下 statement 的索引从 1 开始
                statement.setObject(count + 1, field.get(data));
            } catch (SQLException | IllegalAccessException exception) {
                log.error(String.valueOf(exception));
            }
        }
        return statement;
    }

    /**
     * 回写数据库返回的数据值到数据实体
     */
    private <T> T injectResultSetToData(ResultSet set, T data) {
        // 获取数据实体变量
        Field[] fields = data.getClass().getDeclaredFields();
        // 遍历变量
        try {
            for (Field field : fields) {
                // 获取变量名对应的列名
                String columnName = field.isAnnotationPresent(SqliteMetadata.class)
                        ? field.getAnnotation(SqliteMetadata.class).name() : field.getName();
                // 从结果集中获取数据
                field.set(data, set.getObject(columnName));
            }
        } catch (SQLException | IllegalAccessException e) {
            log.error(String.valueOf(e));
        }
        return data;
    }

    /**
     * 判断表是否存在
     *
     * @param connection 链接
     * @param table      表明
     * @return 是否存在 Boolean 值
     */
    private boolean validateTableExist(Connection connection, String table) {
        try {
            ResultSet result = connection.getMetaData().getTables(null, null, table, null);
            return result.next();
        } catch (SQLException exception) {
            log.error(String.valueOf(exception));
        }
        return false;
    }

    /**
     * 反射访问数据实体的变量 是否存在不为 null 的变量 不存在则返回 null 存在则返回第一个不为 null 变量的变量名和值 返回的是一个
     * Object 数组 下标 0 为列名 下标
     * 1 为值
     */
    private Object[] validateFieldValueExist(Object data) {
        Field[] fields = data.getClass().getDeclaredFields();
        try {
            for (Field field : fields) {
                if (field.get(data) != null) {
                    return new Object[]{getColumnName(field.getName()), field.get(data)};
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 判断数据存在的注解是否有被标记为主键
     *
     * @param fields 变量组
     * @return 任意变量被标记主键则返回 true
     */
    private boolean validatePrimaryKeyExist(Field[] fields) {
        // 判断是否有任意变量被注解标记为主键
        for (Field field : fields) {
            if (field.isAnnotationPresent(SqliteMetadata.class)) {
                SqliteMetadata metadata = field.getAnnotation(SqliteMetadata.class);
                if (metadata.isPrimaryKey()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 创建一个表
     *
     * @param table 表名
     * @param data  元数据
     */
    private String createTable(String table, Object data) {
        StringBuilder builder = new StringBuilder();
        // 反射扫描变量
        // 获取变量列表
        Field[] fields = data.getClass().getDeclaredFields();
        // 判断是否存在主键
        boolean havePrimaryKey = this.validatePrimaryKeyExist(fields);
        builder.append("CREATE TABLE ").append(table).append(" (");

        // 遍历添加到 sql 语句中
        for (int count = 0; count < fields.length; count++) {
            Field field = fields[count];
            // 添加分隔符号
            if (count != 0) {
                builder.append(", ");
            }
            // 获取注解 存在注解则从注解获取数据类型
            if (field.isAnnotationPresent(SqliteMetadata.class)) {
                // 存在注解 获取注解
                SqliteMetadata metadata = field.getAnnotation(SqliteMetadata.class);
                builder.append(metadata.name()).append(" ").append(metadata.type());
                // 添加约束
                if (metadata.isNotNull()) {
                    builder.append(" NOT NULL");
                }
                if (metadata.isPrimaryKey()) {
                    builder.append(" PRIMARY KEY");
                }
                if (metadata.isUnique()) {
                    builder.append(" UNIQUE");
                }
                if (metadata.isAutoincrement()) {
                    builder.append(" AUTO_INCREMENT");
                }
                // 特殊情况 任意变量中注解中没有标记主键
            } else {
                // 没有注解 推导类型 使用变量名全小写
                builder.append(getColumnName(field.getName()))
                        .append(" ")
                        .append(transfer.getDataType(field));
                // 没有注解 指定第一个变量为主键
            }
            if (count == 0 && !havePrimaryKey) {
                builder.append(" PRIMARY KEY");
            }
        }
        // 生成完成
        return builder.append(");").toString();
    }

    /**
     * 字符串全小写 遇到大写字母在大写字母前添加下划线并将该字母小写
     *
     * @param strings 字符串
     * @return 全小写字符串
     */
    private String getColumnName(String strings) {
        char[] chars = strings.toCharArray();
        StringBuilder builder = new StringBuilder();
        for (char c : chars) {
            if (Character.isUpperCase(c)) {
                builder.append("_");
            }
            builder.append(c);
        }
        return builder.toString().toLowerCase(Locale.ROOT);
    }

}
