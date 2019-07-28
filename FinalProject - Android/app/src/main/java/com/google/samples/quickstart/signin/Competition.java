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

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.StatusLine;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.protocol.HTTP;

//this activity pool user data from the server, and inform the current time competition
public class Competition extends AppCompatActivity {

    public static final String TAG = "COMP:";
    ArrayList<user> compUsersList = new ArrayList<>();
    private UserAdapter user_adapter = null;
    boolean suc = true;

    //functions
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_competition);
        ListView myUsersView = findViewById(R.id.LIST);
        user_adapter = new UserAdapter();
        myUsersView.setAdapter(user_adapter);
        getData();
        if (!suc) {
            suc = true;
            String text = "could not get data from server";
            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        }
        user_adapter.notifyDataSetChanged();
        Log.e("Note", ":onCreate");
    }


    public void getData() { //get the data from server and notify the adapter
        final String uri = getString(R.string.ipAddr) + "users/comp";
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try { //get the data - hhtp get to the server
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpGet httpGet = new HttpGet(uri);
                    httpGet.setHeader(HTTP.CONTENT_ENCODING, HTTP.UTF_8);
                    HttpResponse response = httpclient.execute(httpGet);
                    Log.e(TAG, "Get Received: " + response.toString());

                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    String responseString = out.toString();
                    //update competition table.
                    comp_insertJson(compUsersList, responseString);
                    Log.e(TAG, "Get Received: " + responseString);
                } catch (Throwable e) {
                    suc = false;
                    Log.e(TAG, "Get Received Error: " + e.getMessage());
                }}
        });
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            suc = false;
            Log.e(TAG, "coud not join: " + e.getMessage());
        }
        if (!suc) {
            suc = true;
            Toast.makeText(getApplicationContext(),  "could not get data from server", Toast.LENGTH_SHORT).show();
            return;
        }
        user_adapter.notifyDataSetChanged();
    }

    class UserAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return compUsersList.size();
        }

        @Override
        public Object getItem(int position) {
            return compUsersList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater layoutInflater = LayoutInflater.from(getApplicationContext());
                convertView = layoutInflater.inflate(R.layout.userline_layout, parent, false);//to complite
            }
            user us = compUsersList.get(position);

            TextView u_name = convertView.findViewById(R.id.T_uname);
            TextView u_mail = convertView.findViewById(R.id.T_umail);
            TextView u_dist = convertView.findViewById(R.id.T_udist);

            u_name.setText(us.u_name);
            u_mail.setText(us.mail);
            u_dist.setText(String.valueOf(us.dist));
            return convertView;
        }
    }

    public List<user> comp_insertJson(List<user> list, String responseString) {
        try {//put a jason in a list
            JSONArray jsonArray = new JSONArray(responseString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject currentJSON = jsonArray.getJSONObject(i);
                String name = currentJSON.getString("u_name");
                String mail = currentJSON.getString("u_mail");
                double dist = currentJSON.getLong("u_dist");
                String id = currentJSON.getString("u_ID");

                user us = new user(name, mail, dist, id);
                list.add(us);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("on compDB:", "could not convert string to JSON: ");
            return null;
        }
        return list;
    }

}