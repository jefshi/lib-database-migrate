package com.csp.db.update.sample;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = "--[MainActivity]";
    private int mVersion = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deleteDatabase(SQLiteHelper.DATABASE_NAME);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_01) {
            ++mVersion;
            createSqlite(mVersion, R.string.create);
        } else if (id == R.id.btn_02) {
            ++mVersion;
            createSqlite(mVersion, R.string.update);
        } else if (id == R.id.btn_03) {
            deleteDatabase(SQLiteHelper.DATABASE_NAME);
        } else if (id == R.id.btn_04) {
            if (getDatabasePath(SQLiteHelper.DATABASE_NAME).exists())
                queryAll(mVersion);
            else
                Toast.makeText(this, "数据库未创建", Toast.LENGTH_SHORT).show();
        }
    }

    private void createSqlite(int version, int resId) {
        try {
            new SQLiteHelper(this, version).getWritableDatabase();
            Toast.makeText(this, "当前数据库版本：" + version, Toast.LENGTH_SHORT).show();
        } catch (Throwable throwable) {
            Log.e(TAG, getString(resId), throwable);
        }
    }

    private void queryAll(int version) {
        SQLiteDatabase db = new SQLiteHelper(this, version)
                .getWritableDatabase();

        query(db, "APP_INFO");
        query(db, "ACCOUNT_INFO");
        db.close();
    }

    private void query(SQLiteDatabase db, String tableName) {
        StringBuilder builder = new StringBuilder();
        Cursor cursor = null;
        try {
            String sql = "SELECT * FROM " + tableName;
            Log.i(TAG, sql);
            cursor = db.rawQuery(sql, null);
            String[] columnNames = cursor.getColumnNames();
            int num = 0;
            while (cursor.moveToNext()) {
                builder.delete(0, builder.length());
                builder.append("查询表[")
                        .append(tableName)
                        .append("][").append(num++).append("]: ");

                for (int i = 0; i < columnNames.length; i++) {
                    builder.append(columnNames[i])
                            .append(" = ")
                            .append(cursor.getString(i))
                            .append(", ");
                }
                builder.append('\n');
                Log.i(TAG, builder.toString());
            }
        } catch (Throwable throwable) {
            Log.e(TAG, "查询表：" + tableName, throwable);
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }
}
