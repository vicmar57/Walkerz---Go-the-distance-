/*
================================================================================

Walkerz server

authors:
victor martinov
yael lustig
all rights ressrved =]
17/9/2018

================================================================================
 */
 
 package com.google.samples.quickstart.signin;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

//this is a debugger activity - show to gps point
public class TestGpsPoints extends AppCompatActivity {
    private LocalDB localDB;
    private PointSQLadapter point_adapter=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_gps_points);
        ListView myPointsView =findViewById(R.id.test_List);
        localDB=LocalDB.getsInstance(getApplicationContext());
        point_adapter=new PointSQLadapter(getApplicationContext(), localDB.gps_getAllLines(),false);
        myPointsView.setAdapter(point_adapter);
        TextView text=findViewById(R.id.DIST_CHECK);
        text.setText(String.valueOf(localDB.getCurrDistance()));
/*      //for debuging
        Location loc = new Location("ME");
        loc.setLatitude(200);
        loc.setLongitude(9);
        localDB.gps_insertLine(loc);*/
        point_adapter.changeCursor(localDB.gps_getAllLines());

    }

    class PointSQLadapter extends CursorAdapter {

        public PointSQLadapter(Context context, Cursor c, boolean autoRequery){
            super(context, c, autoRequery);
        }
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater layoutInflater=LayoutInflater.from(getApplicationContext());
            View view=layoutInflater.inflate(R.layout.point_layout,parent,false);
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView longT=view.findViewById(R.id.LONG);
            TextView latT=view.findViewById(R.id.LAT);

            double p_long=cursor.getDouble(cursor.getColumnIndex(localDB.Longitude));
            double p_lat=cursor.getDouble(cursor.getColumnIndex(localDB.Latitude));

            longT.setText(String.valueOf(p_long));
            latT.setText(String.valueOf(p_lat));
        }
    }
}
