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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.StatusLine;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.protocol.HTTP;

/**
 * Activity to demonstrate basic retrieval of the Google user's ID, email address, and basic
 * profile.
 */
public class SignInActivity extends AppCompatActivity implements
        View.OnClickListener {

    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;
    private PendingIntent pendingIntent;
    private AlarmManager manager;

    private LocalDB db;
    public static final String U_NAME="u_name";
    public static final String MAIL="mail";
    public static final String DIST="distance";

    //status columm
    public static final String START="start";
    public static final String STOP="stop";
    public static final String T_NAME_STATUS="Status";

    //get person's google details
    String personName;
    String personGivenName;
    String personFamilyName;
    String personEmail;
    String personId;
    Uri personPhoto;
    int interval = 3600000; //ONE HOUR

    private GoogleSignInClient mGoogleSignInClient;
    private TextView mStatusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        //init our local db for usage
        db = LocalDB.getsInstance(getApplicationContext());

        // Retrieve a PendingIntent that will perform a broadcast (to update remote db every hour)
        Intent alarmIntent = new Intent(this, AlarmUpdate.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0);

        // Views
        mStatusTextView = findViewById(R.id.status);

        // Button listeners
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.disconnect_button).setOnClickListener(this);

        // [START configure_signin]
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        // [END configure_signin]

        // [START build_client]
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        // [END build_client]

        // [START customize_button]
        // Set the dimensions of the sign-in button.
        SignInButton signInButton = findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setColorScheme(SignInButton.COLOR_LIGHT);
        // [END customize_button]


        manager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        //int interval = 10000; //todo set to INTERVAL_HOUR (minimum is 1 minute).

        //manager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent);
        manager.setInexactRepeating(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+interval, AlarmManager.INTERVAL_HOUR, pendingIntent);
    }

    @Override
    public void onStart() {
        super.onStart();

        // [START on_start_sign_in]
        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        updateUI(account);
        // [END on_start_sign_in]
    }

    @Override
    protected void onResume() {
        super.onResume();
        //set repeating gps update every hour
        manager.setInexactRepeating(AlarmManager.RTC_WAKEUP,System.currentTimeMillis() + interval , interval, pendingIntent);//todo change times
    }

    // [START onActivityResult]
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }
    // [END onActivityResult]

    // [START handleSignInResult]
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount acct = completedTask.getResult(ApiException.class);

            //get person's details from google account
                personName = acct.getDisplayName();
                personGivenName = acct.getGivenName();
                personFamilyName = acct.getFamilyName();
                personEmail = acct.getEmail();
                personId = acct.getId();
                personPhoto = acct.getPhotoUrl();

            // Signed in successfully, show authenticated UI.
            updateUI(acct);

            //sign in to remote server
                    new Thread(){
                        @Override
                        public void run() {
                            String urlString = getString(R.string.ipAddr) + "newU";
                            try {
                                // Build the JSON object to pass parameters to remote db
                                JSONObject jsonObj = new JSONObject();
                                jsonObj.put("username", personName);
                                jsonObj.put("e_mail", personEmail);
                                jsonObj.put("person_id", personId);
                                jsonObj.put("dist", 0);
                                jsonObj.put("last_update", Calendar.getInstance().getTime().toString());

                                // Create the POST object and add the parameters
                                HttpPost httpPost = new HttpPost(urlString);
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

            // write user's details to local db
            user us = new user(personName, personEmail, 0,personId);
            db.status_insertLine(us);
            Intent in=new Intent(this,MainActivity.class);
            //start main activity
            startActivity(in);
            finish(); //don't enable the user to get back to this activity after moving to main activity

        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            updateUI(null);
        }
    }
    // [END handleSignInResult]


    // [START signIn]
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    // [END signIn]

    // [START signOut]
    private void signOut() {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // [START_EXCLUDE]
                        updateUI(null);
                        // [END_EXCLUDE]
                    }
                });
    }
    // [END signOut]

    // [START revokeAccess]
    private void revokeAccess() {
        mGoogleSignInClient.revokeAccess()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // [START_EXCLUDE]
                        updateUI(null);
                        // [END_EXCLUDE]
                    }
                });
    }
    // [END revokeAccess]

    private void updateUI(@Nullable GoogleSignInAccount account) {
        if (account != null) {
            mStatusTextView.setText(getString(R.string.signed_in_fmt, account.getDisplayName()));

            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.VISIBLE);
        } else {
            mStatusTextView.setText(R.string.signed_out);

            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        //when clicking icons in menu
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
            case R.id.sign_out_button:
                signOut();
                break;
            case R.id.disconnect_button:
                revokeAccess();
                break;
        }
    }

}


