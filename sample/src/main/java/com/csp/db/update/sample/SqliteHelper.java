package com.csp.db.update.sample;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.csp.db.update.lib.MigrateHelper;

/**
 * @author csp
 */
public class SqliteHelper extends SQLiteOpenHelper {
    public final static String DATABASE_NAME = "sample.db";
    private final static String TAG = "--[SQLiteHelper]";

    public SqliteHelper(Context context, int version) {
        super(context, DATABASE_NAME, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String[] sqls = initForOld();
        execSql(db, sqls, "onCreate: ", db.getVersion(), db.getVersion());
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        MigrateHelper.migrate(db, () -> {
            String[] sqls = initForNew();
            execSql(db, sqls, "onUpgrade: ", oldVersion, newVersion);
        });
    }

    private void execSql(SQLiteDatabase db, String[] sqls, String message, int oldVersion, int newVersion) {
        for (String sql : sqls) {
            try {
                Log.i(TAG, message + sql);
                db.execSQL(sql);
            } catch (Throwable throwable) {
                message = "如果新版本号小于等于旧版本号，请删除数据库重试："
                        + "旧版本 = " + oldVersion + ", 新版本 = " + newVersion;
                Log.i(TAG, message, throwable);
            }
        }
    }

    private String[] initForOld() {
        String[] sqls = new String[4];
        sqls[0] = "CREATE TABLE APP_INFO ("
                + "  PACKAGE_NAME TEXT PRIMARY KEY NOT NULL,"
                + "  OPEN_ID TEXT,"
                + "  OPEN_COUNT INTEGER,"
                + "  OPEN_TIME INTEGER"
                + ")";

        sqls[1] = "INSERT INTO APP_INFO (PACKAGE_NAME, OPEN_ID, OPEN_COUNT, OPEN_TIME) VALUES"
                + "  ('com.csp.sample01', null, 1, 51453485),"
                + "  ('com.csp.sample02', null, 0, 51486443),"
                + "  ('com.csp.sample03', 3, 1, 51223465)";

        sqls[2] = "CREATE TABLE ACCOUNT_INFO ("
                + "  _id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL DEFAULT 0,"
                + "  DATE DATE,"
                + "  MONEY REAL,"
                + "  CATEGORY TEXT,"
                + "  PURPOSE TEXT,"
                + "  REMARK TEXT"
                + ")";

        sqls[3] = "INSERT INTO ACCOUNT_INFO (_id, DATE, MONEY, CATEGORY, PURPOSE, REMARK) VALUES"
                + "  (1, '2018-07-01', 27.0, '健身', '游泳', NULL),"
                + "  (2, '2018-07-02', 17.5, '教育', '买书', NULL),"
                + "  (3, '2018-07-03', 33.0, '餐饮', '买菜', NULL)";

        return sqls;
    }

    private String[] initForNew() {
        String[] sqls = new String[4];
        sqls[0] = "CREATE TABLE APP_INFO ("
                + "  PACKAGE_NAME TEXT PRIMARY KEY NOT NULL,"
                + "  OPEN_ID TEXT NOT NULL,"
                + "  OPEN_COUNT INTEGER,"
                + "  OPEN_TIME INTEGER,"
                + "  VERSION_CODE INTEGER NOT NULL DEFAULT 2,"
                + "  APP_SIZE INTEGER INTEGER NOT NULL"
                + ")";

        sqls[1] = "INSERT INTO APP_INFO (PACKAGE_NAME, OPEN_ID, OPEN_COUNT, OPEN_TIME, VERSION_CODE, APP_SIZE) VALUES"
                + "  ('com.csp.sample01', 1, 1, 51453485, 12, 789456),"
                + "  ('com.csp.sample04', 4, 0, 51486443, 18, 789456),"
                + "  ('com.csp.sample05', 5, 1, 51223465, 21, 789456)";

        sqls[2] = "CREATE TABLE ACCOUNT_INFO ("
                + "  _id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL DEFAULT 0,"
                + "  DATE DATE,"
                + "  MONEY REAL,"
                + "  PURPOSE TEXT,"
                + "  REMARK TEXT NOT NULL"
                + ")";

        sqls[3] = "INSERT INTO ACCOUNT_INFO (_id, DATE, MONEY, PURPOSE, REMARK) VALUES"
                + "  (4, '2018-07-04', 87.0, '旅游', ''),"
                + "  (5, '2018-07-05', 87.5, '打的', ''),"
                + "  (6, '2018-07-06', 83.0, '电器', '')";

        return sqls;
    }
}
