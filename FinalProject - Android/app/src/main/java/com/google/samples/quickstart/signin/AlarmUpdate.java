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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.StatusLine;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.protocol.HTTP;

//update GPS points remote db
public class AlarmUpdate extends BroadcastReceiver {

    int checkTread = 0; //check if there's connection to the server
    //Model SQL Local DataBase
    private LocalDB localDB; //our local db
    private static final String TAG = "alarmGPS_update";


    @Override
    public void onReceive(Context context, Intent in) {

        localDB = LocalDB.getsInstance(context);

        //check connection to server
        final String srv_ip = context.getResources().getString(R.string.ipAddr).substring(7, context.getResources().getString(R.string.ipAddr).length() - 6);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                if (!isReachable(srv_ip, 5000, 3000)) {
                    checkTread = 1;
                }
            }
        });
        t.start();

        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //no connection to server.
        if (checkTread == 1) {
            //Toast.makeText(context, "server is unavailable", Toast.LENGTH_SHORT).show();
            checkTread = 0;
            return;
        }

        //there is a connection o server. proceed.

        Date currentTime = Calendar.getInstance().getTime();
        DateFormat formatter = DateFormat.getInstance(); // Date and time
        Log.d("AlarmUPDATE reached", formatter.format(currentTime)); //for debugging

        /*localDB.gps_deleteAll(); //todo for debugging
        Location loc = new Location("ME");
        loc.setLatitude(200.0d);
        loc.setLongitude(100.0d);
        localDB.gps_insertLine(loc);
        localDB.gps_insertLine(loc);*/

        //update remote db with gps points that are in the local db
        //check if there are coord records in the local db
        if (localDB.gps_table_empty()) //pending gps records to update todo back check
        {
            String searchQuery = "SELECT  * FROM GPS_points";
            Cursor cursor = localDB.getSpecifiDdata(searchQuery); //get all lines from gps points table

            //read whole SQL gps_points taable to Json object.
            final JSONArray resultSet = new JSONArray();
            cursor.moveToFirst();
            while (cursor.isAfterLast() == false) {
                int totalColumn = cursor.getColumnCount();
                JSONObject rowObject = new JSONObject();

                for (int i = 0; i < totalColumn; i++) {
                    if (cursor.getColumnName(i) != null) {
                        try {
                            if (cursor.getString(i) != null) {
                                Log.d("TAG_NAME", cursor.getString(i));
                                rowObject.put(cursor.getColumnName(i), cursor.getString(i));
                            } else {
                                rowObject.put(cursor.getColumnName(i), "");
                            }
                        } catch (Exception e) {
                            Log.d("TAG_NAME", e.getMessage());
                        }
                    }
                }
                resultSet.put(rowObject);
                cursor.moveToNext();
            }
            cursor.close();

            final String urlString = context.getResources().getString(R.string.ipAddr) + "update";
            final String urlString2 = context.getResources().getString(R.string.ipAddr) + "update_dist";

            new Thread() { //talk to server to update GPS coordinates and distance for current user.
                @Override
                public void run() {
                    try {

                        // Create the POST object and add the parameters
                        HttpPost httpPost = new HttpPost(urlString);
                        StringEntity entity = new StringEntity(resultSet.toString(), HTTP.UTF_8);
                        entity.setContentType("application/json");
                        httpPost.setEntity(entity);

                        HttpClient client = new DefaultHttpClient();
                        HttpResponse response = client.execute(httpPost);

                        Log.e(TAG, "POST Received: " + response.toString());

                        StatusLine statusLine = response.getStatusLine();
                        int statusCode = statusLine.getStatusCode();
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        response.getEntity().writeTo(out);
                        String responseString = out.toString();
                        Log.e(TAG, "POST Received: " + responseString);

                    } catch (Exception e) {
                        Log.e("log_tag", "Error in http connection " + e.toString());
                        e.printStackTrace();
                        //throw new CustomException("Could not establish network connection");
                    }

                    //update user's distance
                    try {
                        // Build the JSON object to pass parameters
                        //(u_name, u_pass, u_email, u_dist, u_last_up)
                        JSONObject jsonObj = new JSONObject();
                        jsonObj.put("dist", localDB.getCurrDistance());
                        String var = localDB.getUID();
                        jsonObj.put("uid", localDB.getUID());

                        // Create the POST object and add the parameters
                        HttpPost httpPost = new HttpPost(urlString2);
                        StringEntity entity = new StringEntity(jsonObj.toString(), HTTP.UTF_8);
                        entity.setContentType("application/json");
                        httpPost.setEntity(entity);

                        HttpClient client = new DefaultHttpClient();
                        HttpResponse response = client.execute(httpPost);

                        Log.e(TAG, "POST Received: " + response.toString());

                        StatusLine statusLine = response.getStatusLine();
                        int statusCode = statusLine.getStatusCode();
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        response.getEntity().writeTo(out);
                        String responseString = out.toString();
                        Log.e(TAG, "POST Received: " + responseString);

                    } catch (Exception e) {
                        Log.e("log_tag", "Error in http connection " + e.toString());
                        e.printStackTrace();
                    }
                }
            }.start();

            localDB.gps_deleteAll(); // erase gps points delivered to db
            Toast.makeText(context, "updating remote db with collected coordinates", Toast.LENGTH_SHORT).show();
        } else //empty gps db
            Toast.makeText(context, "no gps points to update", Toast.LENGTH_SHORT).show();
    }

    private static boolean isReachable(String addr, int openPort, int timeOutMillis) { //check connection to server
        // Any Open port on other machine
        // openPort =  22 - ssh, 80 or 443 - webserver, 25 - mailserver etc.
        try {
            try (Socket soc = new Socket()) {
                soc.connect(new InetSocketAddress(addr, openPort), timeOutMillis);
            }
            return true;
        } catch (IOException ex) {
            return false;
        }
    }


}
//end class AlarmUpdate
