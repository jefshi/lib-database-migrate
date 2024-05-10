# Android SQLite 版本升级数据迁移帮助类

开发者只要负责新版本数据库所有表的创建，即可完成数据库升级（旧数据保留）
- 只要能获取`android.database.sqlite.SQLiteDatabase`对象即可

支持场景：
1. 新增字段
2. 删除字段
3. 不修改字段名称，仅修改字段属性（添加`NULL DEFAULT`等属性），不过：
    - 新旧表存在相同字段，旧表允许为 NULL，而新表不允许为 NULL。则旧表该字段实际值为 NULL 的数据集会迁移失败，所以需要先手动升级这部分数据再使用本工具

不支持的场景：
1. 新增字段为`NOT NULL`且无`DEFAULT`，实际内容由其他旧字段计算而来。解决方案：
	- 方案1：自动升级前，先手动添加该字段，并填充字段内容
2. 仅修改字段名称

迁移过程说明：全程通过 SQL 完成，不依赖反射等较危险的操作

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
public class DbHelper extends SQLiteOpenHelper {

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 手动升级
        if (oldVersion <= 0) {
            xxx
        }

        // 自动升级
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

1. [yuweiguocn/GreenDaoUpgradeHelper](https://github.com/yuweiguocn/GreenDaoUpgradeHelper)：这个项目主要是给 GreenDao 库管理的数据库升级用的，里面的 SQL 让我意识到了旧数据迁移用 SQL 就能完成大部分，进而进一步完善可迁移的场景与尽可能使用 SQL
2. [SQLite 官网](https://www.sqlite.org/lang.html)：必须得重点感谢啊，sqlite_master 系统表，PRAGMA 语句是完成这个项目的关键
