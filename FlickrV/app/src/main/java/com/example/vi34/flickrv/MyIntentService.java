package com.example.vi34.flickrv;

import android.app.IntentService;
import android.app.WallpaperManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.Environment;
import android.os.Handler;

import com.googlecode.flickrjandroid.Flickr;
import com.googlecode.flickrjandroid.photos.Photo;
import com.googlecode.flickrjandroid.photos.PhotoList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.TreeSet;


public class MyIntentService extends IntentService {


    public MyIntentService() {
        super("MyIntentService");
    }

    private static Handler handler;
    public static final int photosPerPage = 12;

    public static void setHandler(Handler handler) {
        MyIntentService.handler = handler;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String mId = intent.getStringExtra("id");
            int dbId = intent.getIntExtra("dbId", 0);
            int page = intent.getIntExtra("page", 1);
            boolean update = intent.getBooleanExtra("update", false);
            boolean wallpaper = intent.getBooleanExtra("wallpaper", false);
            boolean save = intent.getBooleanExtra("save", false);

            if (mId != null) {
                Uri uri = ContentUris.withAppendedId(MyProvider.PHOTOS_CONTENT_URI, dbId);
                Cursor c = getContentResolver().query(uri, null, null, null, null);
                if (c.getCount() != 0) {
                    c.moveToNext();
                    ContentValues cv = new ContentValues();
                    String url = c.getString(3);
                    Bitmap bmp = downloadImage(url);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte imageInByte[] = stream.toByteArray();
                    cv.put(DBHelper.PHOTO_KEY_AUTHOR, c.getString(1));
                    cv.put(DBHelper.PHOTO_KEY_IMAGE_MEDIUM, c.getBlob(4));
                    cv.put(DBHelper.PHOTO_KEY_ID, mId);
                    cv.put(DBHelper.PHOTO_KEY_LARGE_URL, url);
                    cv.put(DBHelper.PHOTO_KEY_BROWSE_URL, c.getString(8));
                    cv.put(DBHelper.PHOTO_KEY_PHOTOSTREAM_ID, c.getInt(7));
                    cv.put(DBHelper.PHOTO_KEY_IN_FLOW_ID, c.getInt(6));
                    cv.put(DBHelper.PHOTO_KEY_IMAGE_LARGE, imageInByte);

                    getContentResolver().update(uri, cv, null, null);

                }
                c.close();

            } else if (!wallpaper) {
                Flickr flickr = new Flickr(MainActivity.API_KEY, MainActivity.API_SECRET_KEY);

                try {
                    Cursor c1 = getContentResolver().query(MyProvider.PHOTOS_CONTENT_URI, new String[]{DBHelper.PHOTO_KEY_IN_FLOW_ID},
                            DBHelper.PHOTO_KEY_PAGE + " = " + page, null, null);
                    if (c1.getCount() < photosPerPage || update) {
                        if (update) {
                            getContentResolver().delete(MyProvider.PHOTOS_CONTENT_URI, DBHelper.PHOTO_KEY_PAGE + " = " + page, null);
                        }

                        Set<String> extras = new TreeSet<>();
                        extras.add("description");
                        extras.add("owner_name");
                        extras.add("url_l");
                        extras.add("url_c");
                        extras.add("url_q");

                        Cursor c = getContentResolver().query(MyProvider.PHOTOS_CONTENT_URI, new String[]{DBHelper.PHOTO_KEY_IN_FLOW_ID},
                                DBHelper.PHOTO_KEY_PHOTOSTREAM_ID + " = " + 0, null, null);
                        int count = c.getCount();
                        c.close();
                        String s = null;
                        PhotoList photos = flickr.getInterestingnessInterface().getList(s, extras, photosPerPage, page);
                        ContentValues cv = new ContentValues();

                        for (int i = 0; i < photos.size(); ++i) {
                            Photo photo = photos.get(i);

                            Bitmap bmp = downloadImage(photo.getLargeSquareUrl());
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
                            byte imageInByte[] = stream.toByteArray();

                            cv.put(DBHelper.PHOTO_KEY_AUTHOR, photo.getOwner().getUsername());
                            cv.put(DBHelper.PHOTO_KEY_IMAGE_MEDIUM, imageInByte);
                            cv.put(DBHelper.PHOTO_KEY_ID, photo.getId());
                            cv.put(DBHelper.PHOTO_KEY_LARGE_URL, photo.getLargeUrl());
                            cv.put(DBHelper.PHOTO_KEY_BROWSE_URL, photo.getUrl());
                            cv.put(DBHelper.PHOTO_KEY_PHOTOSTREAM_ID, 0);
                            cv.put(DBHelper.PHOTO_KEY_IN_FLOW_ID, count + 1 + i);
                            cv.put(DBHelper.PHOTO_KEY_PAGE, page);

                            // show progress
                            if (handler != null) {
                                handler.obtainMessage(0).sendToTarget();
                            }
                            getContentResolver().insert(MyProvider.PHOTOS_CONTENT_URI, cv).getLastPathSegment();
                        }
                        if (handler != null) {
                            handler.obtainMessage(1).sendToTarget();
                        }
                    }
                    c1.close();
                } catch (Exception e) {
                }
            } else if (wallpaper || save) {
                Uri uri = ContentUris.withAppendedId(MyProvider.PHOTOS_CONTENT_URI, dbId);
                Cursor c = getContentResolver().query(uri, null, null, null, null);
                if (c.getCount() != 0) {
                    c.moveToNext();
                    byte img[] = c.getBlob(5);
                    ByteArrayInputStream imageStream = new ByteArrayInputStream(img);
                    Bitmap bmp = BitmapFactory.decodeStream(imageStream);
                    if (!save) {
                        WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());
                        try {
                            wallpaperManager.setBitmap(bmp);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {

                        try {
                            String root = Environment.getExternalStorageDirectory().toString();
                            File myDir = new File(root + "/saved_images");
                            myDir.mkdirs();
                            File file = new File(myDir, c.getString(2) + ".jpg");
                            if (file.exists()) {
                                file.delete();
                            }
                            FileOutputStream fOut = new FileOutputStream(file);
                            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                            fOut.flush();
                            fOut.close();

                        } catch (Exception e) {
                        }
                    }
                }
                c.close();

            }
        }
    }

    public static Bitmap downloadImage(String url) {
        final HttpClient client = AndroidHttpClient.newInstance("Android");
        final HttpGet getRequest = new HttpGet(url);
        try {
            HttpResponse response = client.execute(getRequest);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                return null;
            }
            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = null;
                try {
                    inputStream = entity.getContent();
                    return BitmapFactory.decodeStream(inputStream);
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    entity.consumeContent();
                }
            }
        } catch (Exception e) {
            getRequest.abort();
        } finally {
            if ((client instanceof AndroidHttpClient)) {
                ((AndroidHttpClient) client).close();
            }
        }
        return null;
    }
}
