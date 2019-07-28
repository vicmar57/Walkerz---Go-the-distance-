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
import android.util.Log;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

public class AlarmRefresh extends BroadcastReceiver {

    private LocalDB localDB;

    @Override
    public void onReceive(Context context, Intent intent) { //erase gps points local db at given time of day (upon receiving email update of daily top5)

        localDB = LocalDB.getsInstance(context); //our local db
        localDB.status_updateDist(0); //update user's distance for next day to 0.
        localDB.gps_deleteAll(); //delete un-updated gps records.

        Date currentTime = Calendar.getInstance().getTime();
        DateFormat formatter = DateFormat.getInstance(); // Date and time
        Log.d("Alarmrefresh reached", formatter.format(currentTime)); //for debugging
    }

}
//end class AlarmRefresh






