package ru.sigmacall.sigma.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import ru.sigmacall.sigma.AppConf;

public class SigmaSQLiteHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "sigma.db";
    public static final int DATABASE_VERSION = 5;

    public static final String TABLE_HISTORY = "history";
    public static final String COL_HIS_ID = "_id";
    public static final String COL_HIS_PHONE_ID = "phoneid";
    public static final String COL_HIS_PHONE_NUMBER = "phonenumber";
    public static final String COL_HIS_PHONE_AREA = "area";
    public static final String COL_HIS_PHONE_PRICE = "price";
    public static final String COL_HIS_PHONE_DURATION = "duration";
    public static final String COL_HIS_CONTACT_URI = "contacturi";
    public static final String COL_HIS_CONTACT_ID = "contactid";
    public static final String COL_HIS_TIMESTAMP = "ts";

    public static final String TABLE_FAVORITES = "favorites";
    public static final String COL_FAV_ID = "_id";
    public static final String COL_FAV_PHONE_ID = "phoneid";
    public static final String COL_FAV_PHONE_AREA = "area";

    public static final String CREATE_TABLE_HISTORY = "create table " + TABLE_HISTORY + " ("
            + COL_HIS_ID + " integer primary key autoincrement,"
            + COL_HIS_PHONE_ID + " text,"
            + COL_HIS_PHONE_NUMBER + " text,"
            + COL_HIS_PHONE_AREA + " text,"
            + COL_HIS_PHONE_PRICE + " real,"
            + COL_HIS_PHONE_DURATION + " integer,"
            + COL_HIS_CONTACT_URI + " text,"
            + COL_HIS_CONTACT_ID + " text,"
            + COL_HIS_TIMESTAMP + " datetime DEFAULT (datetime('now','localtime'))"
            + ")";

    public static final String CREATE_TABLE_FAVORITES = "create table " + TABLE_FAVORITES + " ("
            + COL_FAV_ID + " integer primary key autoincrement,"
            + COL_FAV_PHONE_ID + " text,"
            + COL_FAV_PHONE_AREA + " text"
            + ")";

    public SigmaSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_HISTORY);
        db.execSQL(CREATE_TABLE_FAVORITES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        AppConf.log("Upgrading database from ver " + oldVersion + " to " + newVersion
                + ". All data destroyed.");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
        onCreate(db);
    }
}
