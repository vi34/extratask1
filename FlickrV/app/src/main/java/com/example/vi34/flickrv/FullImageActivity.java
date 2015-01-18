package com.example.vi34.flickrv;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.vi34.flickrv.util.SystemUiHider;

import java.io.ByteArrayInputStream;


public class FullImageActivity extends Activity implements View.OnTouchListener, LoaderManager.LoaderCallbacks<Cursor> {

    private static final boolean AUTO_HIDE = true;
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    private boolean TOGGLE_ON_CLICK = true;
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    private SystemUiHider mSystemUiHider;

    private static final int NONE = 0;
    private static final int ZOOM = 2;

    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();
    private int mode = NONE;
    private PointF start = new PointF();
    private PointF mid = new PointF();
    private float oldDist = 1f;

    private ProgressBar progressBar;
    private ImageView imageView;
    private String photoId;
    private String browseUrl;
    private Bitmap bmp;
    private MyPhoto thisPhoto;
    private int dbId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_image);
        photoId = getIntent().getStringExtra("id");
        dbId = getIntent().getIntExtra("dbId", 0);
        browseUrl = getIntent().getStringExtra("browse");
        this.setTitle(getIntent().getStringExtra("title"));
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setIndeterminate(true);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.fullscreen_content);

        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && AUTO_HIDE) {
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TOGGLE_ON_CLICK) {
                    TOGGLE_ON_CLICK = false;
                    mSystemUiHider.show();
                } else {
                    TOGGLE_ON_CLICK = true;
                    mSystemUiHider.hide();
                }
            }
        });

        findViewById(R.id.button_wallpaper).setOnClickListener(mClickListener);
        findViewById(R.id.button_browse).setOnClickListener(mBrowseListener);
        findViewById(R.id.button_save).setOnClickListener(mSaveListener);
        imageView = (ImageView) contentView;
        contentView.setOnTouchListener(this);
        getLoaderManager().initLoader(1, null, this);
        load();
    }

    void load() {
        if (checkNet()) {
            Intent servIntent = new Intent(this, MyIntentService.class);
            servIntent.putExtra("id", photoId);
            servIntent.putExtra("dbId", dbId);
            startService(servIntent);
        }
    }

    private Boolean checkNet() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        } else {
            Toast.makeText(this, "Check your Internet Connection", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        delayedHide(100);
    }

    View.OnClickListener mSaveListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (progressBar.getVisibility() == View.INVISIBLE) {
                Intent servIntent = new Intent(getApplicationContext(), MyIntentService.class);
                servIntent.putExtra("wallpaper", true);
                servIntent.putExtra("save", true);
                servIntent.putExtra("dbId", dbId);
                startService(servIntent);

            }
        }
    };

    View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (progressBar.getVisibility() == View.INVISIBLE) {
                Intent servIntent = new Intent(getApplicationContext(), MyIntentService.class);
                servIntent.putExtra("wallpaper", true);
                servIntent.putExtra("dbId", dbId);
                startService(servIntent);
            }
        }
    };


    View.OnClickListener mBrowseListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(browseUrl));
            startActivity(intent);
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = ContentUris.withAppendedId(MyProvider.PHOTOS_CONTENT_URI, dbId);
        return new CursorLoader(this, uri, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor.getCount() != 0) {
            cursor.moveToNext();
            String idd = cursor.getString(2);
            byte[] img = cursor.getBlob(5);
            if (img != null) {
                ByteArrayInputStream imageStream = new ByteArrayInputStream(img);
                bmp = BitmapFactory.decodeStream(imageStream);

                RectF drawableRect = new RectF(0, 0, bmp.getWidth(), bmp.getHeight());
                RectF viewRect = new RectF(0, 0, imageView.getWidth(), imageView.getHeight());
                matrix.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER);
                imageView.setScaleType(ImageView.ScaleType.MATRIX);
                imageView.setImageMatrix(matrix);
                imageView.setImageBitmap(bmp);
                imageView.invalidate();
                progressBar.setVisibility(View.INVISIBLE);

                thisPhoto = new MyPhoto(idd, cursor.getString(1), cursor.getBlob(4));
                thisPhoto.dbId = cursor.getInt(0);
                thisPhoto.fullUrl = cursor.getString(3);
                thisPhoto.browseUrl = cursor.getString(8);
                browseUrl = thisPhoto.browseUrl;
            }

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        ImageView view = (ImageView) v;
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist > 10f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == ZOOM) {
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        matrix.set(savedMatrix);
                        float scale = newDist / oldDist;
                        matrix.postScale(scale, scale, mid.x, mid.y);
                    }
                }
                break;
        }

        view.setImageMatrix(matrix);
        return true;
    }


    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }
}
