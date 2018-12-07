# Android SQLite 版本升级数据迁移帮助类
开发者只要负责新版本数据库所有表的创建，即可完成数据库升级（旧数据保留）

### 如何依赖
这个还在摸索中，望大神指教，尽快弄成以下形式：

```
dependencies {
    implementation '***:x.x.x'
}
```

#### 使用说明

在数据库升级的回调方法中调用 ```MigrateHelper.migrate() ``` 方法即可。
以下是 ``` SQLiteOpenHelper ``` 为例进行说明

``` java
@Override
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	MigrateHelper.migrate(db, new MigrateHelper.OnCreateAllTablesListener() {
	    @Override
	    public void onCreateAllTables() {
	        // 新版本数据库所有表的创建
	    }
	};
}
```

#### 源码说明

详细的日后补充吧，这里只说明下，全部升级过程都通过 SQL 完成，不依赖反射等较危险的操作

#### 感谢

1. [yuweiguocn/GreenDaoUpgradeHelper](https://github.com/yuweiguocn/GreenDaoUpgradeHelper)
这个项目主要是给 GreenDao 库管理的数据库升级用的，里面的 SQL 让我意识到了旧数据迁移用 SQL 就能完成大部分。
2. [SQLite 官网](https://www.sqlite.org/lang.html)
SQLite 官网必须得重点感谢啊，sqlite_master 系统表，PRAGMA 语句是完成这个项目的关键
