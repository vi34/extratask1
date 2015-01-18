package com.example.vi34.flickrv;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayInputStream;

/**
 * Created by vi34 on 17.01.15.
 */
public class MyPhoto {
    String author;
    String fullUrl;
    String browseUrl;
    String id;
    int dbId;
    byte[] image;
    private Bitmap bmp = null;

    public MyPhoto(String id, String author, byte[] image) {
        this.id = id;
        this.author = author;
        this.image = image;
    }

    public Bitmap getBitmap() {
        if (bmp == null) {
            ByteArrayInputStream imageStream = new ByteArrayInputStream(image);
            bmp = BitmapFactory.decodeStream(imageStream);
        }
        return bmp;
    }

}
