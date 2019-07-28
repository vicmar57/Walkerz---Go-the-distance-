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

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.StatusLine;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.protocol.HTTP;

public class TopFive extends AppCompatActivity {

    public static final String TAG = "top5 act";
    String output;
    ArrayList<user> top5UsersList = new ArrayList<>();
    private Top5Adapter top5adapt = null;
    boolean suc = true;
    int action = 0; //1 for top5, 2 for yesterdays top5

    TextView user = null;
    TextView email = null;
    TextView dista = null;
    ListView UsersView = null;

    int checkTread=0;
    Button getTop5;
    Button get_y_Top5;

    String srv_ip = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_five);

        getTop5 = (Button) findViewById(R.id.top5);
        get_y_Top5 = (Button) findViewById(R.id.y_top5);

        srv_ip = getString(R.string.ipAddr).substring(7, getString(R.string.ipAddr).length() - 6);

        user = findViewById(R.id.username);
        email = findViewById(R.id.email);
        dista = findViewById(R.id.distan);
        UsersView = findViewById(R.id.top5LIST);

        top5adapt = new Top5Adapter();
        UsersView.setAdapter(top5adapt);

        //when clicking "top5" button, pull top 5 from remote db.
        getTop5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action = 1; //get current top5
                //execute proper action on list

                getTop5.setEnabled(false); //block more clicks while checking communication to server
                get_y_Top5.setEnabled(false); //block more clicks while checking communication to server

                Thread t= new Thread(new Runnable() {
                    @Override
                    public void run() {//check server avalable

                        if (!isReachable(srv_ip, 5000, 2000)) {
                            checkTread = 1;
                        }
                    }
                });
                t.start();
                try {
                    t.join();
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }

                getTop5.setEnabled(true); //enable clicks after checking communication to server
                get_y_Top5.setEnabled(true); //enable clicks after checking communication to server

                if(checkTread==1){
                    Toast.makeText(getApplicationContext(), "server is unavailable", Toast.LENGTH_SHORT).show();
                    checkTread=0;
                    return;
                }


                getData(action);
                if (!suc) {
                    suc = true;
                    String text = "could not get data from server";
                    Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
                }
                top5adapt.notifyDataSetChanged();
                Log.e("Note", ":onCreate");
            }
        });

        ////when clicking "top5" button, pull top 5 from remote db.
        get_y_Top5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action = 2; //get yesterdays top5
                //execute proper action on list


                getTop5.setEnabled(false); //block more clicks while checking communication to server
                get_y_Top5.setEnabled(false); //block more clicks while checking communication to server

                Thread t= new Thread(new Runnable() {
                    @Override
                    public void run() {//check server avalable

                        if (!isReachable(srv_ip, 5000, 2000)) {
                            checkTread = 1;
                        }
                    }
                });
                t.start();
                try {
                    t.join();
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }

                getTop5.setEnabled(true); //enable clicks after checking communication to server
                get_y_Top5.setEnabled(true); //enable clicks after checking communication to server

                if(checkTread==1){
                    Toast.makeText(getApplicationContext(), "server is unavailable", Toast.LENGTH_SHORT).show();
                    checkTread=0;
                    return;
                }

                getData(action);
                if (!suc) {
                    suc = true;
                    String text = "could not get data from server";
                    Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
                }
                top5adapt.notifyDataSetChanged();
                Log.e("Note", ":onCreate");
            }
        });

    }
    //end on create

    //get user data from server
    public void getData(int action) { //get the data from server and notify the adapter

        String url = null;
        switch (action) {
            case 1:
                url = getString(R.string.ipAddr) + "top5"; //local host (serv) ip
                break;

            case 2:
                url = getString(R.string.ipAddr) + "y_top5"; //local host ip
                break;
        }

        final String uri = url;

        //get the data
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //get the data - http get to the server
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpGet httpGet = new HttpGet(uri);
                    httpGet.setHeader(HTTP.CONTENT_ENCODING, HTTP.UTF_8);
                    HttpResponse response = httpclient.execute(httpGet);
                    Log.e(TAG, "Get Received: " + response.toString());

                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    final String responseString = out.toString();

                    //update table.
                    Log.e(TAG, "Get Received: " + responseString);
                    top5_insertJson(top5UsersList, responseString);

                } catch (Throwable e) {
                    suc = false;
                    Log.e(TAG, "Get Received Error: " + e.getMessage());
                }
            }
        });
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            suc = false;
            Log.e(TAG, "could not join: " + e.getMessage());
        }
        if (!suc) {
            suc = true;
            Toast.makeText(getApplicationContext(), "could not get data from server", Toast.LENGTH_SHORT).show();
            return;
        }
        top5adapt.notifyDataSetChanged();
    }
    //end getData


    //insert json to users list
    public List<user> top5_insertJson(List<user> list, String responseString) {
        try {//put a jason in a list
            JSONArray jsonArray = new JSONArray(responseString);

            //clear list
            top5UsersList.clear();

            //add all users to refreshed list
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
    //end insertjson


    //user adapter for display - inner class
    class Top5Adapter extends BaseAdapter {

        @Override
        public int getCount() {
            return top5UsersList.size();
        }

        @Override
        public Object getItem(int position) {
            return top5UsersList.get(position);
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
            user us = top5UsersList.get(position);

            TextView u_name = convertView.findViewById(R.id.T_uname);
            TextView u_mail = convertView.findViewById(R.id.T_umail);
            TextView u_dist = convertView.findViewById(R.id.T_udist);

            u_name.setText(us.u_name);
            u_mail.setText(us.mail);
            u_dist.setText(String.valueOf(us.dist));
            return convertView;
        }
    }


    private static boolean isReachable(String addr, int openPort, int timeOutMillis) {
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
// end class TopFive



