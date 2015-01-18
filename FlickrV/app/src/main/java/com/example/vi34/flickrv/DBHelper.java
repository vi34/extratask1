package com.example.vi34.flickrv;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by vi34 on 16.01.15.
 */
public class DBHelper extends SQLiteOpenHelper {
    public static final String DBNAME = "mydb";
    public static final String PHOTOS_TABLE = "photos";
    public static final Integer VERSION = 2;

    public static final String PHOTO_KEY_AUTHOR = "author";
    public static final String PHOTO_KEY_IMAGE_MEDIUM = "img_medium";
    public static final String PHOTO_KEY_IMAGE_LARGE = "img_large";
    public static final String PHOTO_KEY_ID = "photo_id";
    public static final String PHOTO_KEY_IN_FLOW_ID = "photo_flow_id";
    public static final String PHOTO_KEY_PHOTOSTREAM_ID = "photostream_id";
    public static final String PHOTO_KEY_LARGE_URL = "large_url";
    public static final String PHOTO_KEY_BROWSE_URL = "browse_url";
    public static final String PHOTO_KEY_PAGE = "photo_page";


    public DBHelper(Context context) {
        super(context, DBNAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + PHOTOS_TABLE + " ("
                + "_id integer primary key autoincrement,"
                + PHOTO_KEY_AUTHOR + " text,"
                + PHOTO_KEY_ID + " text,"
                + PHOTO_KEY_LARGE_URL + " text,"
                + PHOTO_KEY_IMAGE_MEDIUM + " blob,"
                + PHOTO_KEY_IMAGE_LARGE + " blob,"
                + PHOTO_KEY_IN_FLOW_ID + " integer,"
                + PHOTO_KEY_PHOTOSTREAM_ID + " integer,"
                + PHOTO_KEY_BROWSE_URL + " text,"
                + PHOTO_KEY_PAGE + " integer" + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists " + PHOTOS_TABLE);
        onCreate(db);
    }
}