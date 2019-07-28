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

import android.database.sqlite.SQLiteOpenHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.Date;

//nice link from google to SQLite db construction - https://www.androidhive.info/2013/09/android-sqlite-database-with-multiple-tables/

public class LocalDB extends SQLiteOpenHelper{

    //DB: anme and version
    public static final int VERSION=2;
    public static final String DB_NAME="local_DB";

    //tables name and version
    public static final String T_NAME_TOP5="Top5";
    public static final String T_NAME_STATUS="Status";
    public static final String T_NAME_COMPETITION="Competition";
    public static final String T_NAME_GPS="GPS_points";

    //columns names - top5 and competition and status    todo save to db when received from server
    public static final String KEY_ID="_id";
    public static final String U_NAME="u_name";
    public static final String MAIL="mail";
    public static final String DIST="distance";
    public static final String UID="uid";

    //columns names - gps todo save to db when gps activated.
    public static final String Longitude="Longtitude";
    public static final String Latitude="Lattitude";
    public static final String U_google_ID="U_google_ID";

    //status columm
    public static final String START="start";
    public static final String STOP="stop";

    //singleTone
    private static LocalDB sInstance;

    //methods
    private LocalDB(Context context){
        super(context,DB_NAME,null,VERSION);
    }

    public static LocalDB getsInstance(Context con){
        if(sInstance==null)
            sInstance=new LocalDB(con);
        return sInstance;
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        //creation of the tables:
        StringBuilder sql_top5=new StringBuilder();
        StringBuilder sql_status=new StringBuilder();
        StringBuilder sql_comp=new StringBuilder();
        StringBuilder sql_gps=new StringBuilder();

        //top5 creation:
        sql_top5.append("CREATE TABLE " + T_NAME_TOP5);
        sql_top5.append("(");
        sql_top5.append(KEY_ID+" INTEGER PRIMARY KEY,");
        sql_top5.append(U_NAME+" TEXT,");
        sql_top5.append(DIST+" FLOAT");
        sql_top5.append(")");
        db.execSQL(sql_top5.toString());

        //status creation
        sql_status.append("CREATE TABLE " + T_NAME_STATUS);
        sql_status.append("(");
        sql_status.append(KEY_ID+" INTEGER PRIMARY KEY,");
        sql_status.append(U_NAME+" TEXT,");
        sql_status.append(UID+" TEXT, ");
        sql_status.append(MAIL+" TEXT,");
        sql_status.append(DIST+" FLOAT, ");// real = float, FLOAT=double
        sql_status.append(START+" FLOAT, ");//real = float, FLOAT=double
        sql_status.append(STOP+" FLOAT");//real = float, FLOAT=double
        sql_status.append(")");
        db.execSQL(sql_status.toString());

        //competition creation
        sql_comp.append("CREATE TABLE " + T_NAME_COMPETITION);
        sql_comp.append("(");
        sql_comp.append(KEY_ID+" INTEGER PRIMARY KEY,");
        sql_comp.append(U_NAME+" TEXT,");
        sql_comp.append(MAIL+" TEXT,");
        sql_comp.append(DIST+" FLOAT"); //Float is like double
        sql_comp.append(")");
        db.execSQL(sql_comp.toString());

        //gps creation
        sql_gps.append("CREATE TABLE " + T_NAME_GPS);
        sql_gps.append("(");
        sql_gps.append(KEY_ID+" INTEGER PRIMARY KEY,");
        sql_gps.append(U_google_ID+" TEXT,");
        sql_gps.append(Longitude+" FLOAT,");
        sql_gps.append(Latitude+" FLOAT");
        sql_gps.append(")");
        db.execSQL(sql_gps.toString());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
/*        // If you need to add a column
        if (newVersion == 2 && oldVersion == 1) {
            db.execSQL("ALTER TABLE " + TAB_NAME + " ADD COLUMN " + KEY_IMAGE + " TEXT");
        }*/
    }

    public void status_insertLine(user us){
        ContentValues contentValues=new ContentValues();
        contentValues.put(U_NAME,us.u_name);
        contentValues.put(MAIL,us.mail);
        contentValues.put(DIST,us.dist);
        contentValues.put(UID,us.UID);
        contentValues.put(START,0);
        contentValues.put(STOP,0);
        getWritableDatabase().insert(T_NAME_STATUS,null,contentValues);
    }

    public void gps_insertLine(Location location){
        ContentValues contentValues=new ContentValues();
        contentValues.put(Longitude,location.getLongitude());
        contentValues.put(Latitude,location.getLatitude());
        contentValues.put(U_google_ID,sInstance.getUID());
        getWritableDatabase().insert(T_NAME_GPS,null,contentValues);
    }

    //Cursor is a pointer to a line in the table (and all after this line)
    public Cursor gps_getAllLines(){
        SQLiteDatabase db=getReadableDatabase();
        Cursor cursor=db.rawQuery("select * from "+ T_NAME_GPS, null);
        return cursor;
    }

    public boolean gps_table_empty(){
        SQLiteDatabase db=getReadableDatabase();
        Cursor cursor=db.rawQuery("select * from "+ T_NAME_GPS, null);
        if(cursor.moveToFirst())
            return true;
        return false;
    }

    public boolean status_table_empty(){//false if empty
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor=db.rawQuery("select * from " + T_NAME_STATUS, null);
        if(cursor.moveToFirst()) //returns false if empty
            return true;
        return false;
    }

    public Cursor getSpecifiDdata(String query){
        SQLiteDatabase db=getReadableDatabase();
        return  db.rawQuery(query,null);
    }

    public void status_updateLine(double delta){ //update only the distance by delta
        double dist=0;
        SQLiteDatabase db=getWritableDatabase();
        Cursor cursor=db.rawQuery("select * from "+ T_NAME_STATUS, null);
        if(cursor.moveToFirst()){
            dist = cursor.getDouble(cursor.getColumnIndex(DIST));
        }
        ContentValues contentValues=new ContentValues();
        double roundDist=Double.parseDouble(new DecimalFormat("##.####").format( dist+delta));
        contentValues.put(DIST, roundDist);
        int check=db.update(T_NAME_STATUS,contentValues,DIST+">=0",null);
        return;
    }

    public void status_updateStart(double start){
        SQLiteDatabase db=getWritableDatabase();
        int check=-1;
        Cursor cursor=db.rawQuery("select * from "+ T_NAME_STATUS, null);
        if(cursor.moveToFirst()){
            ContentValues contentValues=new ContentValues();
            contentValues.put(START, start);
            check=db.update(T_NAME_STATUS,contentValues,null,null);
        }
        return;
    }

    public void status_updateDist(double dist){
        SQLiteDatabase db=getWritableDatabase();
        int check=-1;
        Cursor cursor=db.rawQuery("select * from "+ T_NAME_STATUS, null);
        if(cursor.moveToFirst()){
            ContentValues contentValues=new ContentValues();
            contentValues.put(DIST, dist);
            check=db.update(T_NAME_STATUS,contentValues,null,null);
        }
        return;
    }

    public void status_updateStop(double stop){
        SQLiteDatabase db=getWritableDatabase();
        Cursor cursor=db.rawQuery("select * from "+ T_NAME_STATUS, null);
        if(cursor.moveToFirst()){
            ContentValues contentValues=new ContentValues();
            contentValues.put(STOP, stop);
            int check=db.update(T_NAME_STATUS,contentValues,null,null);
        }
        return;
    }

    public void gps_deleteAll(){
        SQLiteDatabase db=getWritableDatabase();
        db.delete(T_NAME_GPS,null,null);
        return;
    }

    public double getCurrDistance(){
        double dist=0;
        SQLiteDatabase db=getWritableDatabase();
        Cursor cursor=db.rawQuery("select * from "+ T_NAME_STATUS, null);
        if (cursor.moveToFirst())
            dist=cursor.getDouble(cursor.getColumnIndex(DIST));
        return(dist);//dist
    }

    public String getUID(){
        String uid = "";
        SQLiteDatabase db=getWritableDatabase();
        Cursor cursor=db.rawQuery("select * from "+ T_NAME_STATUS, null);
        if (cursor.moveToFirst())
            uid = cursor.getString(cursor.getColumnIndex(UID));
        return(uid);
    }

    public double getCurrStart(){
        double start=0;
        SQLiteDatabase db=getWritableDatabase();
        Cursor cursor=db.rawQuery("select * from "+ T_NAME_STATUS, null);
        if (cursor.moveToFirst())
            start=cursor.getDouble(cursor.getColumnIndex(START));
        return(start);
    }

    public double getCurrStop(){
        double stop=0;
        SQLiteDatabase db=getWritableDatabase();
        Cursor cursor=db.rawQuery("select * from "+ T_NAME_STATUS, null);
        if (cursor.moveToFirst())
            stop=cursor.getDouble(cursor.getColumnIndex(STOP));
        return(stop);
    }

    //good function, not in use
    public void comp_insertJson(String responseString) {
        SQLiteDatabase db=getWritableDatabase();
        try {
            JSONArray jsonArray = new JSONArray(responseString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject currentJSON = jsonArray.getJSONObject(i);
                ContentValues contentValues=new ContentValues();
                contentValues.put(U_NAME,currentJSON.getString("u_name"));
                contentValues.put(MAIL,currentJSON.getString("u_mail"));
                contentValues.put(DIST,currentJSON.getLong("u_dist"));
                db.insert(T_NAME_COMPETITION,null,contentValues);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("on compDB:", "could not convert string to JSON: ");
        }
    }
}

