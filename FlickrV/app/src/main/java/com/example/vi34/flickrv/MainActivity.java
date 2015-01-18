package com.example.vi34.flickrv;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String API_KEY = "a0f7f5818e3f6a67f9bca18df31fdc34";
    public static final String API_SECRET_KEY = "dfbdeb38e1eea60a";
    private PhotoAdapter myAdapter;
    private GridView gridView;
    private TextView txtPhotostream;
    private ViewFlipper viewFlipper;
    private Handler handler;
    private int myProgress = 0;
    private int currentPage = 1;
    private boolean updating = false;
    private ProgressBar progressBar;
    private Intent intent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = (ProgressBar) findViewById(R.id.progressBarHorizontal);
        txtPhotostream = (TextView) findViewById(R.id.txt_photostream);
        txtPhotostream.setText(R.string.title);
        viewFlipper = (ViewFlipper) findViewById(R.id.view_flipper);
        intent = new Intent(this, FullImageActivity.class);
        gridView = (GridView) findViewById(R.id.gridView1);
        List<MyPhoto> list1 = new ArrayList<MyPhoto>();
        myAdapter = new PhotoAdapter(list1);
        gridView.setAdapter(myAdapter);
        gridView.setOnItemClickListener(listener);
        getLoaderManager().initLoader(1, null, this);
        load();

        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                switch (message.what) {
                    case 0:
                        if (myProgress == 0) {
                            updating = true;
                            progressBar.setVisibility(View.VISIBLE);
                        }
                        myProgress++;

                        if (myProgress == MyIntentService.photosPerPage) {
                            progressBar.setVisibility(View.INVISIBLE);
                            updating = false;
                        }
                        progressBar.setProgress(myProgress);
                        break;
                    case 1:
                        progressBar.setVisibility(View.INVISIBLE);
                        updating = false;
                }

                return true;
            }
        });
        MyIntentService.setHandler(handler);
    }

    AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View v,
                                int position, long id) {
            intent.putExtra("id", myAdapter.mData.get(position).id);
            intent.putExtra("dbId", myAdapter.mData.get(position).dbId);
            intent.putExtra("browse", myAdapter.mData.get(position).browseUrl);
            intent.putExtra("title", myAdapter.mData.get(position).author);
            startActivity(intent);
        }
    };

    void load() {
        if (checkNet()) {
            Intent servIntent = new Intent(this, MyIntentService.class);
            servIntent.putExtra("page", currentPage);
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
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, MyProvider.PHOTOS_CONTENT_URI, null, DBHelper.PHOTO_KEY_PAGE + " = " + currentPage, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        try {
            if (cursor.getCount() != 0) {
                myAdapter.mData.clear();
                while (cursor.moveToNext()) {
                    String name = cursor.getString(1);
                    byte[] img = cursor.getBlob(4);
                    String id = cursor.getString(2);
                    MyPhoto photo = new MyPhoto(id, name, img);
                    photo.fullUrl = cursor.getString(3);
                    photo.dbId = cursor.getInt(0);
                    photo.browseUrl = cursor.getString(8);
                    myAdapter.mData.add(photo);
                }
                myAdapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        myAdapter = new PhotoAdapter(new ArrayList<MyPhoto>());
        gridView.setAdapter(myAdapter);
    }

    public void nextPage(View view) {
        myProgress = 0;
        viewFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.go_next_in));
        viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.go_next_out));
        viewFlipper.showNext();

        currentPage++;
        if (currentPage % 2 == 0) {
            gridView = (GridView) findViewById(R.id.gridView2);

        } else {
            gridView = (GridView) findViewById(R.id.gridView1);
        }
        myAdapter = new PhotoAdapter(new ArrayList<MyPhoto>());
        gridView.setAdapter(myAdapter);
        gridView.setOnItemClickListener(listener);
        myAdapter.notifyDataSetChanged();
        getLoaderManager().restartLoader(1, null, MainActivity.this);
        load();
    }

    public void prevPage(View view) {
        if (currentPage != 1) {
            myProgress = 0;
            viewFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.go_prev_in));
            viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.go_prev_out));
            viewFlipper.showPrevious();

            currentPage--;
            if (currentPage % 2 == 0) {
                gridView = (GridView) findViewById(R.id.gridView2);
            } else {
                gridView = (GridView) findViewById(R.id.gridView1);
            }
            myAdapter = new PhotoAdapter(new ArrayList<MyPhoto>());
            gridView.setAdapter(myAdapter);
            gridView.setOnItemClickListener(listener);
            myAdapter.notifyDataSetChanged();
            getLoaderManager().restartLoader(1, null, MainActivity.this);
            load();
        }
    }

    public void refresh(View view) {
        if (checkNet() && !updating) {
            Intent servIntent = new Intent(this, MyIntentService.class);
            servIntent.putExtra("update", true);
            servIntent.putExtra("page", currentPage);
            startService(servIntent);
            updating = true;
            myProgress = 0;
        }
    }
}
