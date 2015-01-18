package com.example.vi34.flickrv;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

/**
 * Created by vi34 on 17.01.15.
 */
public class MyProvider extends ContentProvider {
    static final String PHOTO_ID = "_id";
    public static final String AUTHORITY = "com.example.vi34.flickrv";
    public static final Uri PHOTOS_CONTENT_URI = Uri.parse("content://"
            + AUTHORITY + "/" + DBHelper.PHOTOS_TABLE);
    static final int URI_PHOTOS = 1;
    static final int URI_PHOTOS_ID = 2;
    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, DBHelper.PHOTOS_TABLE, URI_PHOTOS);
        uriMatcher.addURI(AUTHORITY, DBHelper.PHOTOS_TABLE + "/#", URI_PHOTOS_ID);
    }

    private SQLiteDatabase db;
    private DBHelper dbHelper;

    @Override
    public boolean onCreate() {
        dbHelper = new DBHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        switch (uriMatcher.match(uri)) {
            case URI_PHOTOS:
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = PHOTO_ID + " ASC";
                }
                builder.setTables(DBHelper.PHOTOS_TABLE);
                break;
            case URI_PHOTOS_ID:
                String id = uri.getLastPathSegment();
                builder.setTables(DBHelper.PHOTOS_TABLE);
                if (TextUtils.isEmpty(selection)) {
                    selection = PHOTO_ID + " = " + id;
                } else {
                    selection = selection + " AND " + PHOTO_ID + " = " + id;
                }
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
        db = dbHelper.getWritableDatabase();
        Cursor cursor = builder.query(db, projection, selection, selectionArgs, null, null, sortOrder);

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        String table;
        Uri resultUri;
        switch (uriMatcher.match(uri)) {
            case URI_PHOTOS:
                table = DBHelper.PHOTOS_TABLE;
                resultUri = PHOTOS_CONTENT_URI;
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
        db = dbHelper.getWritableDatabase();
        long rowID = db.insert(table, null, values);
        resultUri = ContentUris.withAppendedId(resultUri, rowID);
        getContext().getContentResolver().notifyChange(resultUri, null);
        return resultUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        String table;
        String id;
        switch (uriMatcher.match(uri)) {
            case URI_PHOTOS:
                table = DBHelper.PHOTOS_TABLE;
                break;
            case URI_PHOTOS_ID:
                id = uri.getLastPathSegment();
                table = DBHelper.PHOTOS_TABLE;
                if (TextUtils.isEmpty(selection)) {
                    selection = PHOTO_ID + " = " + id;
                } else {
                    selection = selection + " AND " + PHOTO_ID + " = " + id;
                }
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
        db = dbHelper.getWritableDatabase();
        int cnt = db.delete(table, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return cnt;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        String table;
        String id;
        switch (uriMatcher.match(uri)) {
            case URI_PHOTOS_ID:
                id = uri.getLastPathSegment();
                table = DBHelper.PHOTOS_TABLE;
                if (TextUtils.isEmpty(selection)) {
                    selection = PHOTO_ID + " = " + id;
                } else {
                    selection = selection + " AND " + PHOTO_ID + " = " + id;
                }
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
        db = dbHelper.getWritableDatabase();
        int cnt = db.update(table, values, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return cnt;
    }
}