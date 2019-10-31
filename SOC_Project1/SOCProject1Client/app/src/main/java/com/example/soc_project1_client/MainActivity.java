package com.example.soc_project1_client;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;


import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements LocationListener{

    EditText host_field;
    EditText username_field;
    Button start_button;
    Button stop_button;

    String username;
    String timestamp;
    double latitude;
    double longitude;
    String host;

    long update = 5000; // location update in milliseconds

    boolean checkGPS = false;

    boolean canGetLocation = false;

    Location loc;
    protected LocationManager locationManager;

    Context context;

    RequestQueue queue; // this = context
    String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();
        queue = Volley.newRequestQueue(context);

        host_field = findViewById(R.id.host);
        username_field = findViewById(R.id.username);
        start_button = findViewById(R.id.start_button);
        stop_button = findViewById(R.id.stop_button);

        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                username = username_field.getText().toString();
                host = host_field.getText().toString();
                timestamp = Long.toString(System.currentTimeMillis());
                url = "http://" + host + "/locationupdate";

                getLocation();

                if (canGetLocation) {
//                    Toast.makeText(getApplicationContext(), "Longitude:" + Double.toString(longitude) + "\nLatitude:" + Double.toString(latitude), Toast.LENGTH_SHORT).show();
//                    sendLocationInfo();

                } else {

                    showSettingsAlert();
                }

            }
        });

        stop_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (canGetLocation) {
                    stopListener();
                    Toast.makeText(getApplicationContext(), "GPS Stopped", Toast.LENGTH_SHORT).show();

                }

            }
        });
    }

    private Location getLocation() {

        try {
            locationManager = (LocationManager) context
                    .getSystemService(LOCATION_SERVICE);

            // get GPS status
            checkGPS = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            if (!checkGPS) {
                Toast.makeText(context, "No Service Provider is available", Toast.LENGTH_SHORT).show();
            } else {
                this.canGetLocation = true;

                // if GPS Enabled get lat/long using GPS Services
                if (checkGPS) {

                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        //
                        ActivityCompat.requestPermissions((Activity)context,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                1);
                    }

                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            update,
                            0, this);
                    if (locationManager != null) {
                        loc = locationManager
                                .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (loc != null) {
                            latitude = loc.getLatitude();
                            longitude = loc.getLongitude();
                            Log.d("Latitude", Double.toString(latitude));
                            Log.d("Longitude", Double.toString(longitude));

                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return loc;
    }

    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);


        alertDialog.setTitle("GPS is not Enabled!");

        alertDialog.setMessage("Do you want to turn on GPS?");


        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                context.startActivity(intent);
            }
        });


        alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });


        alertDialog.show();
    }


    public void stopListener() {
        if (locationManager != null) {

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.removeUpdates(this);
            Log.d("GPS Status", "GPS Stopped");

        }
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    /**
     * POST method for location info
     */
    public  void sendLocationInfo(){

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        // response
                        Log.d("Server Response", response);
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Log.d("Error.Response", error.toString());
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("username", username);
                params.put("timestamp", timestamp);
                params.put("latitude", Double.toString(latitude));
                params.put("longitude", Double.toString(longitude));

                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> params = new HashMap<String, String>();
//                params.put("Content-Type", "application/json; charset=utf-8");

                return params;
            }

        };
        postRequest.setRetryPolicy(new DefaultRetryPolicy(500000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(postRequest);
    }

    @Override
    public void onLocationChanged(Location location) {
        username = username_field.getText().toString();
        host = host_field.getText().toString();
        timestamp = Long.toString(System.currentTimeMillis());
        url = "http://" + host + "/locationupdate";
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        Log.d("Username", username);
        Log.d("Timestamp", timestamp);
        Log.d("Latitude", Double.toString(latitude));
        Log.d("Longitude", Double.toString(longitude));
        Toast.makeText(context, "Longitude:" + Double.toString(longitude) + "\nLatitude:" + Double.toString(latitude), Toast.LENGTH_SHORT).show();
        sendLocationInfo();

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
