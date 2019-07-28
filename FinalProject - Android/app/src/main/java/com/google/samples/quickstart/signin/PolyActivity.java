package com.google.samples.quickstart.signin;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.StatusLine;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.protocol.HTTP;

import static com.google.samples.quickstart.signin.R.id.map;


/**
 * An activity that displays a Google map with polylines to represent paths or routes,
 * and polygons to represent areas.
 */
public class PolyActivity extends AppCompatActivity
        implements
        OnMapReadyCallback,
        GoogleMap.OnPolylineClickListener,
        GoogleMap.OnPolygonClickListener {

    private static final int COLOR_BLACK_ARGB = 0xff000000;
    private static final int COLOR_WHITE_ARGB = 0xffffffff;
    private static final int COLOR_GREEN_ARGB = 0xff388E3C;
    private static final int COLOR_PURPLE_ARGB = 0xff81C784;
    private static final int COLOR_ORANGE_ARGB = 0xffF57F17;
    private static final int COLOR_BLUE_ARGB = 0xffF9A825;

    private static final int POLYLINE_STROKE_WIDTH_PX = 12;
    private static final int POLYGON_STROKE_WIDTH_PX = 8;
    private static final int PATTERN_DASH_LENGTH_PX = 20;
    private static final int PATTERN_GAP_LENGTH_PX = 20;
    private static final PatternItem DOT = new Dot();
    private static final PatternItem DASH = new Dash(PATTERN_DASH_LENGTH_PX);
    private static final PatternItem GAP = new Gap(PATTERN_GAP_LENGTH_PX);

    LocalDB localDB = null;
    private static final String TAG = "maps";
    ArrayList<LatLng> coordList;

    // Create a stroke pattern of a gap followed by a dot.
    private static final List<PatternItem> PATTERN_POLYLINE_DOTTED = Arrays.asList(GAP, DOT);

    // Create a stroke pattern of a gap followed by a dash.
    private static final List<PatternItem> PATTERN_POLYGON_ALPHA = Arrays.asList(GAP, DASH);

    // Create a stroke pattern of a dot followed by a gap, a dash, and another gap.
    private static final List<PatternItem> PATTERN_POLYGON_BETA =
            Arrays.asList(DOT, GAP, DASH, GAP);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve the content view that renders the map.
        setContentView(R.layout.act_poly);

        // Get the SupportMapFragment and request notification when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);

        localDB = LocalDB.getsInstance(getApplicationContext());

    }

    /**
     * Manipulates the map when it's available.
     * The API invokes this callback when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * In this tutorial, we add polylines and polygons to represent routes and areas on the map.
     */
    @Override
    public void onMapReady(final GoogleMap googleMap) {

        final String urlString = getString(R.string.ipAddr) + "users/GPScoordinatesForUser";


        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Build the JSON object to pass parameters- (u_name, u_pass, u_email, u_dist, u_last_up)
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("userID", localDB.getUID());

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

                    try {//put a jason in a list

                        coordList = new ArrayList<LatLng>();

                        // Adding points to ArrayList
                        JSONArray jsonArray = new JSONArray(responseString);
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject currentJSON = jsonArray.getJSONObject(i);
                            double latitude = currentJSON.getDouble("latitude");
                            double longtitude = currentJSON.getDouble("longtitude");
                            coordList.add(new LatLng(latitude, longtitude));
                        }

                    int i =0;
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                        Log.e("on compDB:", "could not convert string to JSON: ");
                    }
                } catch (Exception e) {
                    Log.e("log_tag", "Error in http connection " + e.toString());
                    e.printStackTrace();
                }
            }
        });
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            //suc = false;
            Log.e(TAG, "coud not join: " + e.getMessage());
        }


        PolylineOptions polylineOptions = new PolylineOptions();
        if(coordList.size()!= 0)
            polylineOptions.addAll(coordList);
        polylineOptions.color(getApplicationContext().getResources().getColor(R.color.blue));
        googleMap.addPolyline(polylineOptions);

        // Position the map's camera near Alice Springs in the center of Australia,
        // and set the zoom factor so most of Australia shows on the screen.
        if(coordList.size()!= 0)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordList.get(0),18));
        else
        {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-23.684, 133.903), 4));
            Toast.makeText(getApplicationContext(), "no route to display", Toast.LENGTH_SHORT).show();
        }

        // Set listeners for click events.
        googleMap.setOnPolylineClickListener(this);
        googleMap.setOnPolygonClickListener(this);

    }

    /**
     * Styles the polyline, based on type.
     *
     * @param polyline The polyline object that needs styling.
     */
    private void stylePolyline(Polyline polyline) {
        String type = "";
        // Get the data object stored with the polyline.
        if (polyline.getTag() != null) {
            type = polyline.getTag().toString();
        }

        switch (type) {
            // If no type is given, allow the API to use the default.
/*            case "A":
                // Use a custom bitmap as the cap at the start of the line.
                polyline.setStartCap(
                        new CustomCap(
                                BitmapDescriptorFactory.fromResource(R.drawable.ic_arrow), 10));
                break;*/
            case "B":
                // Use a round cap at the start of the line.
                polyline.setStartCap(new RoundCap());
                break;
        }

        polyline.setEndCap(new RoundCap());
        polyline.setWidth(POLYLINE_STROKE_WIDTH_PX);
        polyline.setColor(COLOR_BLACK_ARGB);
        polyline.setJointType(JointType.ROUND);
    }

    /**
     * Styles the polygon, based on type.
     *
     * @param polygon The polygon object that needs styling.
     */
    private void stylePolygon(Polygon polygon) {
        String type = "";
        // Get the data object stored with the polygon.
        if (polygon.getTag() != null) {
            type = polygon.getTag().toString();
        }

        List<PatternItem> pattern = null;
        int strokeColor = COLOR_BLACK_ARGB;
        int fillColor = COLOR_WHITE_ARGB;

        switch (type) {
            // If no type is given, allow the API to use the default.
            case "alpha":
                // Apply a stroke pattern to render a dashed line, and define colors.
                pattern = PATTERN_POLYGON_ALPHA;
                strokeColor = COLOR_GREEN_ARGB;
                fillColor = COLOR_PURPLE_ARGB;
                break;
            case "beta":
                // Apply a stroke pattern to render a line of dots and dashes, and define colors.
                pattern = PATTERN_POLYGON_BETA;
                strokeColor = COLOR_ORANGE_ARGB;
                fillColor = COLOR_BLUE_ARGB;
                break;
        }

        polygon.setStrokePattern(pattern);
        polygon.setStrokeWidth(POLYGON_STROKE_WIDTH_PX);
        polygon.setStrokeColor(strokeColor);
        polygon.setFillColor(fillColor);
    }

    /**
     * Listens for clicks on a polyline.
     *
     * @param polyline The polyline object that the user has clicked.
     */
    @Override
    public void onPolylineClick(Polyline polyline) {
        // Flip from solid stroke to dotted stroke pattern.
        if ((polyline.getPattern() == null) || (!polyline.getPattern().contains(DOT))) {
            polyline.setPattern(PATTERN_POLYLINE_DOTTED);
        } else {
            // The default pattern is a solid stroke.
            polyline.setPattern(null);
        }

        Toast.makeText(this, "Route type " + polyline.getTag().toString(),
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Listens for clicks on a polygon.
     *
     * @param polygon The polygon object that the user has clicked.
     */
    @Override
    public void onPolygonClick(Polygon polygon) {
        // Flip the values of the red, green, and blue components of the polygon's color.
        int color = polygon.getStrokeColor() ^ 0x00ffffff;
        polygon.setStrokeColor(color);
        color = polygon.getFillColor() ^ 0x00ffffff;
        polygon.setFillColor(color);

        Toast.makeText(this, "Area type " + polygon.getTag().toString(), Toast.LENGTH_SHORT).show();
    }
}




/*
*                     HttpClient httpclient = new DefaultHttpClient();
                    HttpGet httpGet = new HttpGet(uri);
                    httpGet.setHeader(HTTP.CONTENT_ENCODING, HTTP.UTF_8);
                    HttpResponse response = httpclient.execute(httpGet);
                    Log.e(TAG, "Get Received: " + response.toString());

                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    String responseString = out.toString();

                    //update competition table.
                    //comp_insertJson(compUsersList, responseString);

                    Log.e(TAG, "Get Received: " + responseString);
                } catch (Throwable e) {
                    suc = false;
                    Log.e(TAG, "Get Received Error: " + e.getMessage());

                    */