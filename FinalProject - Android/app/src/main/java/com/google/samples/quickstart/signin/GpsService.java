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
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

//this service is the connection between the application and the gps
//nice explanations abouts working with gps on youtube
public class GpsService extends Service {

    private static final int LOCATION_INTERVAL = 5000; //1000 is 1 sec
    private static final float LOCATION_DISTANCE = 0;
    private static final double MAX_VELOCITY = 5.55; //m/s = 20km/h
    private LocationManager locationManager=null;
    private LocationListener locationListener;
    private Location last=null;
    private double delta=0;
    private LocalDB localDB;
    private String TAG="Service :";



    @Override
    public void onCreate()
    {
        Log.e(TAG, "onCreate");

        localDB = LocalDB.getsInstance(getApplicationContext());
        if(locationManager==null)
            locationManager= (LocationManager) (getApplicationContext().getSystemService(LOCATION_SERVICE));
        locationListener = new LocationListener() {//define the location listener
            @Override
            public void onLocationChanged(Location location) {
                if(last==null){
                    last=new Location("");
                }
                else{
                    delta=last.distanceTo(location);
                    localDB.status_updateLine(delta);
                }
                last.setLongitude(location.getLongitude());
                last.setLatitude(location.getLatitude());
                localDB.gps_insertLine(location);
            }
            @Override public void onStatusChanged(String s, int i, Bundle bundle) {   }
            @Override public void onProviderEnabled(String s) {  }
            @Override
            public void onProviderDisabled(String s) {
                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(i);
            }
        };
        getGpsLocation();
    }

    public void getGpsLocation(){
        try {
            locationManager.requestLocationUpdates("gps", LOCATION_INTERVAL, LOCATION_DISTANCE, locationListener);
            Log.e(TAG, "request location updates");
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
    }
    @Override
    public void onDestroy()
    {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (Exception ex) {
                Log.i(TAG, "fail to remove location listners, ignore", ex);
            }
        }
    }

    @Override
    // TODO: Return the communication channel to the service.
    public IBinder onBind(Intent intent) {
        return null;
    }
}

