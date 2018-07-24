package com.example.dolf.waarisek;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class MainActivity extends Activity {

    public static final int REQUEST_LOCATION_PERMISSIONS = 1;
    public static final int REQUEST_CHECK_SETTINGS = 2;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private Location lastLocation;

    private TextView latitudeText;
    private TextView longitudeText;
    private TextView timeText;
    private TextView accuracyText;
    private TextView statusText;

    private static final Locale locale = Locale.getDefault();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", locale);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latitudeText = findViewById(R.id.latitude);
        longitudeText = findViewById(R.id.longitude);
        timeText = findViewById(R.id.time);
        accuracyText = findViewById(R.id.accuracy);
        statusText = findViewById(R.id.status);

        statusUpdate("Creating activity…");

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestLastLocation();

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                statusUpdate("Got location results.");
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    lastLocation = location;
                    updateUI();
                }
            }
        };
        createLocationRequest();

    }

    private void requestLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            statusUpdate("Insufficient permissions to get last location.");
            requestLocationPermission();
            return;
        }

        statusUpdate("Requesting last location…");
        Task<Location> task = mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            lastLocation = location;
                            updateUI();
                        }
                    }
                });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                statusUpdate("Failed to get last location: " + e.getLocalizedMessage());
            }
        });

        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                statusUpdate("Got the last location successfully.");
            }
        });

    }

    private void requestLocationPermission() {
        toast("Requesting permission...");
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION
        }, REQUEST_LOCATION_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        statusUpdate("Got permissions:");
        for (int i = 0; i < permissions.length; i++) {
            statusUpdate(String.format("%s %s", permissions[i], (grantResults[i] == PackageManager.PERMISSION_GRANTED) ? "granted" : "denied"));
        }
    }

    private void createLocationRequest() {

        statusUpdate("Creating location request…");

        // Create the location request and set the parameters
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // add the location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);

        // check whether the current location settings are satisfied
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
                // location requests here.
                // ...
                statusUpdate("Location settings are satisfied.");
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    statusUpdate("Location settings are not satisfied.");
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this,
                                REQUEST_CHECK_SETTINGS);
                        statusUpdate("Location settings resolved.");
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                        statusUpdate("Could not resolve problem with location settings.");
                    }
                }
            }
        });

    }

    private void toast(CharSequence text) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    private void statusUpdate(CharSequence text) {
        statusText.append(text + "\n");
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            statusUpdate("Insufficient permissions to start location updates.");
            requestLocationPermission();
            return;
        }

        statusUpdate("Starting location updates.");
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                null /* Looper */);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates(); // Stop location updates when paused!
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    private void updateUI() {
        latitudeText.setText(String.valueOf(lastLocation.getLatitude()));
        longitudeText.setText(String.valueOf(lastLocation.getLongitude()));
        timeText.setText(sdf.format(lastLocation.getTime()));
        accuracyText.setText(String.format(locale, "Accuracy [m] = %.2f", lastLocation.getAccuracy()));
    }

}
