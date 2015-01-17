package com.example.vi34.flickrv;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.util.Log;
import android.widget.TextView;

import com.googlecode.flickrjandroid.Flickr;
import com.googlecode.flickrjandroid.photos.Photo;
import com.googlecode.flickrjandroid.photos.PhotoContext;
import com.googlecode.flickrjandroid.photos.PhotoList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.TreeSet;


public class MyIntentService extends IntentService {


    public MyIntentService() {
        super("MyIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String mId = intent.getStringExtra("id");
            int dbId = intent.getIntExtra("dbId",0);

            if(mId != null) {
                Uri uri = ContentUris.withAppendedId(MyProvider.PHOTOS_CONTENT_URI,dbId);
                Cursor c = getContentResolver().query(uri, null,null, null, null);
                if(c.getCount() != 0) {
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

                    getContentResolver().update(uri,cv,null,null);

                }

            } else {
                Flickr flickr = new Flickr(MainActivity.API_KEY, MainActivity.API_SECRET_KEY);

                try {
                    Set<String> extras = new TreeSet<>();
                    extras.add("description");
                    extras.add("owner_name");
                    extras.add("url_n");
                    extras.add("url_c");

                    String s = null;
                    PhotoList photos = flickr.getInterestingnessInterface().getList(s, extras, 10, 0);
                    ContentValues cv = new ContentValues();

                    Cursor c = getContentResolver().query(MyProvider.PHOTOS_CONTENT_URI, new String[]{DBHelper.PHOTO_KEY_IN_FLOW_ID},
                            DBHelper.PHOTO_KEY_PHOTOSTREAM_ID + " = " + 0, null, null);
                    int count = c.getCount();

                    for (int i = 0; i < photos.size(); ++i) {
                        Photo photo = photos.get(i);

                        Bitmap bmp = downloadImage(photo.getSmall320Url());
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        byte imageInByte[] = stream.toByteArray();

                        String idd = photo.getId();
                        cv.put(DBHelper.PHOTO_KEY_AUTHOR, photo.getOwner().getUsername());
                        cv.put(DBHelper.PHOTO_KEY_IMAGE_MEDIUM, imageInByte);
                        cv.put(DBHelper.PHOTO_KEY_ID, photo.getId());
                        cv.put(DBHelper.PHOTO_KEY_LARGE_URL, photo.getMedium800Url());
                        cv.put(DBHelper.PHOTO_KEY_BROWSE_URL, photo.getUrl());
                        cv.put(DBHelper.PHOTO_KEY_PHOTOSTREAM_ID, 0);
                        cv.put(DBHelper.PHOTO_KEY_IN_FLOW_ID, count + 1 + i);
                        // show progress
                        getContentResolver().insert(MyProvider.PHOTOS_CONTENT_URI, cv);
                    }

                } catch (Exception e) {
                }
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
                Log.w("ImageDownloader", "Error " + statusCode + " while retrieving bitmap from " + url);
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
        } catch (IOException e) {
            getRequest.abort();
        } catch (IllegalStateException e) {
            getRequest.abort();
            Log.w("Incorrect URL:" + url, e);
        } catch (Exception e) {
            getRequest.abort();
            Log.w("Error while retrieving bitmap from " + url, e);
        } finally {
            if ((client instanceof AndroidHttpClient)) {
                ((AndroidHttpClient) client).close();
            }
        }
        return null;
    }
}
