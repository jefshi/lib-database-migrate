package com.csp.db.update.lib;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 数据库升级时数据迁移帮助类：https://github.com/jefshi/DatabaseUpdateHelper
 * 注意项：
 * 1. 新旧表存在相同字段，旧表允许为 NULL，而新表不允许为 NULL。则旧表该字段实际值为 NULL 的数据集会迁移失败，所以需要先手动升级这部分数据再使用本工具
 * <p>
 * Created by csp on 2018/07/07
 * Modified by csp on 2018/07/07
 *
 * @author csp
 * @version 1.0.0
 */
public final class MigrateHelper {
    private static boolean sDebug = false;

    private final String TEMP_SUFFIX = "_TEMP_" + System.currentTimeMillis();
    private final SQLiteDatabase mDatabase;
    private List<String> oldTableNames;
    private List<String> newTableNames;

    private MigrateHelper(SQLiteDatabase database) {
        mDatabase = database;
    }

    public static void setDebug(boolean debug) {
        sDebug = debug;
    }

    public static void migrate(SQLiteDatabase db, OnCreateAllTablesListener listener) {
        new MigrateHelper(db).migrate(listener);
    }

    private void migrate(OnCreateAllTablesListener listener) {
        if (mDatabase == null) {
            throw new IllegalArgumentException("SQLiteDatabase can't be null");
        }

        if (listener == null) {
            throw new IllegalArgumentException("OnCreateAllTablesListener can't be null");
        }

        oldTableNames = selectAllTables();
        if (oldTableNames != null) {
            createTemporaryTables();
        }

        printLog("CREATE ALL TABLE: BEGIN");
        listener.onCreateAllTables();
        printLog("CREATE ALL TABLE: END");

        if (oldTableNames == null) {
            return;
        }

        newTableNames = selectAllTables();
        if (newTableNames != null) {
            migrateData();
        }
    }

    private List<String> selectAllTables() {
        String sql = "SELECT tbl_name FROM sqlite_master WHERE type = 'table' AND tbl_name NOT IN" +
                "('sqlite_sequence','sqlite_stat1','sqlite_stat2','sqlite_stat3','sqlite_stat4')";

        List<String> tableNames = null;
        Cursor cursor = null;
        try {
            printLog(sql);
            cursor = mDatabase.rawQuery(sql, null);
            tableNames = new ArrayList<>(cursor.getCount());
            String tableName;
            while (cursor.moveToNext()) {
                tableName = cursor.getString(0);
                tableNames.add(tableName);
            }
        } catch (Throwable throwable) {
            printLog("SELECT ALL TABLE NAME", throwable);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return tableNames;
    }

    private void createTemporaryTables() {
        String sql;
        String tempTableName;
        for (String tableName : oldTableNames) {
            tempTableName = tableName + TEMP_SUFFIX;
            sql = "DROP TABLE IF EXISTS `" + tempTableName + '`';
            execSql(sql);

            sql = "CREATE TEMPORARY TABLE IF NOT EXISTS `"
                    + tempTableName + "` AS SELECT * FROM `" + tableName + '`';
            execSql(sql);

            sql = "DROP TABLE IF EXISTS `" + tableName + '`';
            execSql(sql);
        }
    }

    private void migrateData() {
        String sql;
        StringBuilder builder = new StringBuilder();

        String tempTableName;
        String column;

        List<TableInfo> newTableInfos;
        List<TableInfo> tempTableInfos;
        List<String> selectColumns = new ArrayList<>();
        List<String> intoColumns = new ArrayList<>();
        String intoConcat;
        String selectConcat;
        for (String tableName : oldTableNames) {
            if (!newTableNames.contains(tableName)) {
                continue;
            }

            newTableInfos = selectTableInfo(tableName);
            if (newTableInfos.isEmpty()) {
                continue;
            }

            tempTableName = tableName + TEMP_SUFFIX;
            tempTableInfos = selectTableInfo(tempTableName);
            if (tempTableInfos.isEmpty()) {
                continue;
            }

            selectColumns.clear();
            intoColumns.clear();
            // 新旧表存在相同字段，旧表允许为 NULL，而新表不允许为 NULL，造成部分数据迁移失败。如果新表有 DEFAULT 还能尝试迁移（目前未实现），如果没有 DEFAULT 那只能先手动升级再使用本工具
            // temporary table columns list
            for (TableInfo tableInfo : tempTableInfos) {
                if (newTableInfos.contains(tableInfo)) {
                    column = '`' + tableInfo.name + '`';
                    intoColumns.add(column);
                    selectColumns.add(column);
                }
            }
            // NOT NULL and default value is null columns list
            for (TableInfo tableInfo : newTableInfos) {
                if (tableInfo.notnull && tableInfo.dfltValue == null
                        && !tempTableInfos.contains(tableInfo)) {
                    column = '`' + tableInfo.name + '`';
                    intoColumns.add(column);
                    selectColumns.add("'' AS " + column);
                }
            }

            if (intoColumns.size() == 0) {
                continue;
            }

            intoConcat = TextUtils.join(",", intoColumns);
            selectConcat = TextUtils.join(",", selectColumns);

            builder.delete(0, builder.length());
            builder.append("INSERT OR IGNORE INTO `")
                    .append(tableName)
                    .append("` (")
                    .append(intoConcat)
                    .append(") SELECT ")
                    .append(selectConcat)
                    .append(" FROM ")
                    .append(tempTableName);
            execSql(builder.toString());

            sql = "DROP TABLE IF EXISTS " + tempTableName;
            execSql(sql);
        }
    }

    private List<TableInfo> selectTableInfo(String tableName) {
        String sql = "PRAGMA table_info(`" + tableName + "`)";
        printLog(sql);

        List<TableInfo> tableInfos = new ArrayList<>();
        try (Cursor cursor = mDatabase.rawQuery(sql, null)) {
            if (cursor == null) {
                return new ArrayList<>();
            }

            TableInfo tableInfo;
            while (cursor.moveToNext()) {
                tableInfo = new TableInfo();
                tableInfo.cid = cursor.getInt(0);
                tableInfo.name = cursor.getString(1);
                tableInfo.type = cursor.getString(2);
                tableInfo.notnull = cursor.getInt(3) == 1;
                tableInfo.dfltValue = cursor.getString(4);
                tableInfo.pk = cursor.getInt(5) == 1;

                printLog("Table Info：" + tableInfo);
                tableInfos.add(tableInfo);
            }
        } catch (Throwable throwable) {
            tableInfos.clear();
            printLog(sql, throwable);
        }
        return tableInfos;
    }

    private void execSql(String sql) {
        Throwable throwable = null;
        try {
            mDatabase.execSQL(sql);
        } catch (Throwable tr) {
            throwable = tr;
        }
        printLog(sql, throwable);
    }

    private void printLog(Object message) {
        printLog(message, null);
    }

    private void printLog(Object message, Throwable throwable) {
        String tag = null;
        if (throwable != null || sDebug) {
            tag = getTag(new Exception().getStackTrace()[2]);
        }

        if (throwable != null) {
            Log.e(tag, String.valueOf(message), throwable);
        } else if (sDebug) {
            Log.d(tag, String.valueOf(message));
        }
    }

    private static String getTag(StackTraceElement element) {
        String className = element.getClassName();
        String methodName = element.getMethodName();
        String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
        return "--[" + simpleClassName + "][" + methodName + ']';
    }

    @FunctionalInterface
    public interface OnCreateAllTablesListener {
        /**
         * 创建所有的新表
         */
        void onCreateAllTables();
    }

    private static class TableInfo {
        private int cid;
        private String name;
        private String type;
        private boolean notnull;
        private String dfltValue;
        private boolean pk;

        @Override
        public boolean equals(Object o) {
            return this == o
                    || o != null && getClass() == o.getClass() && Objects.equals(name, ((TableInfo) o).name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        @NonNull
        public String toString() {
            if (sDebug) {
                return super.toString();
            } else {
                return "{\"cid\":" + cid +
                        ",\"name\":" + name +
                        ",\"type\":" + type +
                        ",\"notnull\": " + notnull +
                        ",\"dfltValue\":" + dfltValue +
                        ",\"pk\":" + pk +
                        "}";
            }
        }
    }
}
