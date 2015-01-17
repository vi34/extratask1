package com.example.vi34.flickrv;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.flickrjandroid.Flickr;
import com.googlecode.flickrjandroid.FlickrException;
import com.googlecode.flickrjandroid.photos.PhotoList;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    public static final String API_KEY = "a0f7f5818e3f6a67f9bca18df31fdc34";
    public static final String API_SECRET_KEY= "dfbdeb38e1eea60a";
    private PhotoAdapter myAdapter;
    private GridView gridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Intent intent = new Intent(this, FullImageActivity.class);
        gridView = (GridView) findViewById(R.id.gridView1);
        List<MyPhoto> list1 = new ArrayList<MyPhoto>();
        myAdapter = new PhotoAdapter(list1);
        gridView.setAdapter(myAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                int dbid = myAdapter.mData.get(position).dbId;
                intent.putExtra("id",myAdapter.mData.get(position).id);
                intent.putExtra("dbId",myAdapter.mData.get(position).dbId);
                startActivity(intent);
            }
        });
        getLoaderManager().initLoader(1,null,this);
        load();
        DBHelper h = new DBHelper(this);
        h.clearBase(h.getWritableDatabase());
    }

    void load()
    {
        if(checkNet())  {
            Intent servIntent = new Intent(this, MyIntentService.class);

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
        return new CursorLoader(this, MyProvider.PHOTOS_CONTENT_URI,null,null,null,null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        if(cursor.getCount() != 0) {
            myAdapter.mData.clear();
            while (cursor.moveToNext()) {
                String name = cursor.getString(1);
                byte[] img = cursor.getBlob(4);
                String id = cursor.getString(2);
                MyPhoto photo = new MyPhoto(id, name, img);
                photo.fullUrl = cursor.getString(3);
                photo.dbId = cursor.getInt(0);
                myAdapter.mData.add(photo);
            }
            myAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        myAdapter = new PhotoAdapter(new ArrayList<MyPhoto>());
        gridView.setAdapter(myAdapter);
    }
}
