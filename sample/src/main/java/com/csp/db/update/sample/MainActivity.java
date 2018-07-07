package com.csp.db.update.sample;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = "--[MainActivity]";
    private int mVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_01) {
            mVersion = 1;
            createSqlite(mVersion, R.string.create);
        } else if (id == R.id.btn_02) {
            mVersion = 2;
            createSqlite(mVersion, R.string.update);
        } else if (id == R.id.btn_03) {
            deleteDatabase(SQLiteHelper.DATABASE_NAME);
        } else if (id == R.id.btn_04) {
            queryAll();
        }
    }

    private void createSqlite(int version, int resId) {
        try {
            new SQLiteHelper(this, 2).getWritableDatabase();
        } catch (Throwable throwable) {
            Log.e(TAG, getString(resId), throwable);
        }
    }

    private void queryAll() {
        SQLiteDatabase db = new SQLiteHelper(this, mVersion)
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
            while (cursor.moveToNext()) {
                builder.delete(0, builder.length());
                for (int i = 0; i < columnNames.length; i++) {
                    builder.append("查询表[")
                            .append(tableName)
                            .append("][").append(i).append("]: ")
                            .append(columnNames[i])
                            .append(" = ")
                            .append(cursor.getString(i))
                            .append(", ");
                }
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
