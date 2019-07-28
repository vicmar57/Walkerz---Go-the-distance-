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


import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.widget.ToggleButton;

import java.util.Calendar;
import java.util.Date;


public class MainActivity extends AppCompatActivity {

    //when to update distance and gps points local db - currently 20:00.
    int HOUR_DEF = 20;
    int MIN__DEF = 00;

    private static final String TAG = "main Act";
    static boolean record = true;    //application pgs: on\off
    static boolean startStop = false; //application pgs: on\off
    LocalDB localDB = null;
    boolean alarmSet = false;
    private PendingIntent pendingIntent;
    private AlarmManager manager;
    private AlarmManager alarmMgr;
    private PendingIntent alarmIn;
    int checkTread = 0, check2 = 1;
    int interval = 3600000; //ONE HOUR
    Button update;
    Button Btop5;
    Button comp;
    Button maps;

    String srv_ip = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        localDB = LocalDB.getsInstance(getApplicationContext());
        ToggleButton t_start = findViewById(R.id.START_BUTTON);
        ToggleButton t_gps = findViewById(R.id.ONOFF);
        update = (Button) findViewById(R.id.B_update);
        Btop5 = (Button) findViewById(R.id.B_TOP5);
        comp = (Button) findViewById(R.id.B_comp);
        maps = (Button) findViewById(R.id.B_comp);

        srv_ip = getString(R.string.ipAddr).substring(7, getString(R.string.ipAddr).length() - 6);

        if (t_start.isActivated())
            startStop = true;
        if (t_gps.isActivated())
            record = false;

        if (!localDB.status_table_empty()) //no signIn to google yet
        {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    //server ip address, port, and timeout
                    if (!isReachable(srv_ip, 5000, 2000)) {
                        check2 = 1;
                    } else
                        check2 = 0;
                }
            });
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (check2 == 1) {
                Toast.makeText(getApplicationContext(), "please make sure server is working and on the same LAN as your device.", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Intent in = new Intent(this, SignInActivity.class);
                startActivity(in);
            }
        } else {//already signed in to google
            Log.e("signedin", "already signed");
            if (!alarmSet) {
                // Retrieve a PendingIntent that will perform a broadcast
                Intent alarmIntent = new Intent(this, AlarmUpdate.class);
                pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0);
                manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + interval, interval, pendingIntent);
                alarmSet = true;
                //int interval = 10000; // 10 seconds todo set to INTERVAL_HOUR, minimum is 1 minute.
                //manager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent);

                alarmMgr = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                Intent intent = new Intent(getApplicationContext(), AlarmRefresh.class);
                alarmIn = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);

                // Set the alarm to start at 21:32 PM
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.set(Calendar.HOUR_OF_DAY, HOUR_DEF);
                calendar.set(Calendar.MINUTE, MIN__DEF);
                alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY, alarmIn);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        localDB.status_updateStop(0);
    }

    public void clickUpdate(View view) {
        disableButtons(); //block more clicks while checking communication to server

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {//check server avalable

                //server ip address, port, and timeout
                if (!isReachable(srv_ip, 5000, 2000)) {
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

        enableButtons(); //enable clicks after checking communication to server

        if (checkTread == 1) {
            Toast.makeText(getApplicationContext(), "server is unavailable", Toast.LENGTH_SHORT).show();
            checkTread = 0;
            return;
        }

        Date currentTime = Calendar.getInstance().getTime();
        DateFormat formatter = DateFormat.getInstance(); // Date and time
        Log.d("reached", formatter.format(currentTime));

        //update remote db with gps points that are in the local db
        //check if there are coord records in the local db
        if (localDB.gps_table_empty()) //pending gps records to update todo back check
        {
            String searchQuery = "SELECT  * FROM GPS_points";
            Cursor cursor = localDB.getSpecifiDdata(searchQuery); //get all lines from gps points table

            //read whole SQL gps_points to Json object.
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

            new Thread() {
                @Override
                public void run() {
                    try {
                        String urlString = getString(R.string.ipAddr) + "update"; //todo get from strings.xml

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

                    String urlString = getString(R.string.ipAddr) + "update_dist";
                    try {
                        // Build the JSON object to pass parameters- (u_name, u_pass, u_email, u_dist, u_last_up)
                        JSONObject jsonObj = new JSONObject();
                        jsonObj.put("dist", localDB.getCurrDistance());
                        String var = localDB.getUID();
                        jsonObj.put("uid", localDB.getUID());

                        // Create the POST object and add the parameters
                        HttpPost httpPost = new HttpPost(urlString);
                        StringEntity entity = new StringEntity(jsonObj.toString(), HTTP.UTF_8);
                        entity.setContentType("application/json");
                        httpPost.setEntity(entity);
                        HttpClient client = new DefaultHttpClient();
                        HttpResponse response = client.execute(httpPost);
                        Log.e(TAG, "POST Received: " + response.toString());

                        StatusLine statusLine = response.getStatusLine();
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
            Toast.makeText(getApplicationContext(), "updating remote db with collected coordinates", Toast.LENGTH_SHORT).show();
        } else //empty gps db
            Toast.makeText(getApplicationContext(), "no gps points to update", Toast.LENGTH_SHORT).show();
    }

    //display user's walk on Gmaps
    public void clickMaps(View view) {

        disableButtons(); //block more clicks while checking communication to server

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {//check server avalable

                //server ip address, port, and timeout
                if (!isReachable(srv_ip, 5000, 2000)) {
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

        enableButtons(); //enable clicks after checking communication to server

        if (checkTread == 1) {
            Toast.makeText(getApplicationContext(), "server is unavailable", Toast.LENGTH_SHORT).show();
            checkTread = 0;
            return;
        }
        Intent in = new Intent(this, PolyActivity.class);
        startActivity(in);
    }

    //get top5 current walkerz
    public void clickTop5(View view) {
        disableButtons(); //block more clicks while checking communication to server

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {//check server avalable

                //server ip address, port, and timeout
                if (!isReachable(srv_ip, 5000, 2000)) {
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

        enableButtons(); //enable clicks after checking communication to server

        if (checkTread == 1) {
            Toast.makeText(getApplicationContext(), "server is unavailable", Toast.LENGTH_SHORT).show();
            checkTread = 0;
            return;
        }
        Intent in = new Intent(this, TopFive.class);
        startActivity(in);

    }

    public void clickCompetition(View view) {
        disableButtons(); //block more clicks while checking communication to server

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {//check server avalable

                //server ip address, port, and timeout
                if (!isReachable(srv_ip, 5000, 2000)) {
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

        enableButtons(); //enable clicks after checking communication to server

        if (checkTread == 1) {
            Toast.makeText(getApplicationContext(), "server is unavailable", Toast.LENGTH_SHORT).show();
            checkTread = 0;
            return;
        }
        Intent in = new Intent(this, Competition.class);
        startActivity(in);

    }

    public void clickOnOff(View view) {

        if (!record) {
            stopService(new Intent(MainActivity.this, GpsService.class));
            record = !record;
            if (startStop) {
                Toast.makeText(this, "stop recording", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (startStop) {
                Toast.makeText(this, "continue recording", Toast.LENGTH_SHORT).show();
            }
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET}
                            , 10);
                }
                return;
            } else {
                record = !record;
            }
            Intent in = new Intent(this, GpsService.class);
            startService(in);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 10:
                record = !record;
                Intent in = new Intent(this, GpsService.class);
                startService(in);
                break;
            default:
                break;
        }
    }

    public void clickStartStop(View view) {//start to count current distance (for running), timer of distance
        startStop = !startStop;
        if (!startStop) {
            localDB.status_updateStop(localDB.getCurrDistance());
            return;
        }
        if (record) {
            Toast.makeText(this, "turn gps on", Toast.LENGTH_SHORT).show();
            return;
        }
        final Handler handler = new Handler();
        if (localDB.getCurrStop() > 0 || localDB.getCurrStart() == 0) {
            localDB.status_updateStart(localDB.getCurrDistance());
            localDB.status_updateStop(0);
            TextView t = findViewById(R.id.T_KM);
            t.setText("0m");
        }
        handler.postDelayed(new Runnable() {
            public void run() {
                if (startStop & !record) {
                    TextView t = findViewById(R.id.T_KM);
                    double a = localDB.getCurrDistance();
                    double b = localDB.getCurrStart();
                    t.setText(String.format("%.2f", localDB.getCurrDistance() - localDB.getCurrStart()) + "m");
                }
                handler.postDelayed(this, 5000);
            }
        }, 5000);
    }


    public void clickTestGPS(View view) {
        if (record) {
            Toast.makeText(this, "turn gps on", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent in = new Intent(this, TestGpsPoints.class);
        startActivity(in);
    }
/*
    private void showPermissionDialog() {
        if (!LocationController.checkPermission(this)) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    10);
        }
    }*/

    //check if server is reachable
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

    private void enableButtons() {
        maps.setEnabled(true); //block more clicks while checking communication to server
        update.setEnabled(true); //block more clicks while checking communication to server
        Btop5.setEnabled(true); //block more clicks while checking communication to server
        comp.setEnabled(true); //block more clicks while checking communication to server
    }

    private void disableButtons() {
        maps.setEnabled(false); //block more clicks while checking communication to server
        update.setEnabled(false); //block more clicks while checking communication to server
        Btop5.setEnabled(false); //block more clicks while checking communication to server
        comp.setEnabled(false); //block more clicks while checking communication to server
    }
}
