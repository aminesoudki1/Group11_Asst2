package com.example.amine.group11_asst2;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper
{
    private static int DB_VERSION = 1;
    private String table_name;
    private String db_name;
    public DBHelper(Context context,String db_name, String table_name){
        super(context,db_name,null,DB_VERSION);
        this.table_name = table_name;
        this.db_name = db_name;
    }
    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}
