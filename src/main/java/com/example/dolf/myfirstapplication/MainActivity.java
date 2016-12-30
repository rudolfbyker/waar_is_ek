package com.example.dolf.myfirstapplication;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Geocoder;
import android.location.Location;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import android.content.pm.PackageManager;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class MainActivity
        extends AppCompatActivity
        implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<LocationSettingsResult>,
        LocationListener {

    private TextView latitudeText;
    private TextView longitudeText;
    private TextView timeText;
    private TextView accuracyText;
    private TextView addressText;
    private TextView statusText;
    private GoogleApiClient mGoogleApiClient;
    private Location location;
    private LocationRequest mLocationRequest;
    private AddressResultReceiver mResultReceiver;

    private static final Locale locale = Locale.getDefault();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", locale);

    public static final int REQUEST_CHECK_SETTINGS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latitudeText = (TextView) findViewById(R.id.latitude);
        longitudeText = (TextView) findViewById(R.id.longitude);
        timeText = (TextView) findViewById(R.id.time);
        accuracyText = (TextView) findViewById(R.id.accuracy);
        addressText = (TextView) findViewById(R.id.address);
        statusText = (TextView) findViewById(R.id.status);

        statusUpdate("Creating activity…");

        mResultReceiver = new AddressResultReceiver(new Handler());

        requestLocationPermissions();

        // Set up GAPI client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(
                        this /* FragmentActivity */,
                        this /* OnConnectionFailedListener */
                )
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();

        createLocationRequest();

    }

    private void requestLocationPermissions() {
        statusUpdate("Requesting location permissions…");
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
        }, 0);
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
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(this);

    }


    protected void onStart() {
        statusUpdate("Activity started.");
        statusUpdate("Connecting to GAPI client…");
        mGoogleApiClient.connect();
        super.onStart();
    }


    protected void onStop() {
        statusUpdate("Activity stopped.");
        statusUpdate("Disconnecting from GAPI client…");
        mGoogleApiClient.disconnect();
        super.onStop();
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        statusUpdate("Connected to GAPI client.");
        requestLastKnownLocation();
        startLocationUpdates();
    }


    private void requestLastKnownLocation() {
        statusUpdate("Getting last known location…");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions();
            return;
        }
        location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {
            statusUpdate("Location not available.");
            return;
        }
        statusUpdate("Got last known location:\n" + location.toString());
        updateUI();
    }


    private void updateUI() {
        latitudeText.setText(String.valueOf(location.getLatitude()));
        longitudeText.setText(String.valueOf(location.getLongitude()));
        timeText.setText(sdf.format(location.getTime()));
        accuracyText.setText(String.format(locale, "Accuracy [m] = %.2f", location.getAccuracy()));
    }


    protected void startLocationUpdates() {
        statusUpdate("Starting location updates…");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions();
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }


    @Override
    public void onConnectionSuspended(int i) {
        statusUpdate("Connection to GAPI client suspended.");
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        statusUpdate("Connection to GAPI client failed:\n" + connectionResult.toString());
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        statusUpdate("Got permissions:");
        for (int i=0; i<permissions.length; i++) {
            statusUpdate(String.format("%s %s", permissions[i], (grantResults[i] == PackageManager.PERMISSION_GRANTED) ? "granted" : "denied"));
        }
    }


    protected void statusUpdate(String msg) {
        statusText.append(msg+"\n");
    }


    @Override
    public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
        statusUpdate("Got location settings result.");
        final Status status = locationSettingsResult.getStatus();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                statusUpdate("All location settings are satisfied.");
                // TODO initialize location requests here??
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                statusUpdate("User is required to change location settings.");
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    status.startResolutionForResult(this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException e) {
                    statusUpdate(e.toString());
                }
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                statusUpdate("Cannot change location settings.");
                break;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        statusUpdate(String.format(locale, "Got activity result: %d, %d", requestCode, resultCode));
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                if (resultCode == RESULT_OK) {
                    statusUpdate("Location settings have changed. Trying again.");
                    createLocationRequest();
                } else {
                    statusUpdate("Failed to change location settings.");
                    // TODO: complain that the app won't work without location settings
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onLocationChanged(Location location) {
        statusUpdate("Got new location:\n" + location.toString());
        this.location = location;
        updateUI();

        // Determine whether a Geocoder is available.
        if (!Geocoder.isPresent()) {
            statusUpdate(getString(R.string.no_geocoder_available));
            return;
        }

        startGeocoderIntentService(this.location, this.mResultReceiver);

    }


    protected void startGeocoderIntentService(Location location, ResultReceiver receiver) {
        statusUpdate("Starting geocoder intent service…");
        Intent intent = new Intent(this, GeocoderIntentService.class);
        intent.setAction(GeocoderIntentService.ACTION_FETCH_ADDRESS);
        intent.putExtra(GeocoderIntentService.EXTRA_RECEIVER, receiver);
        intent.putExtra(GeocoderIntentService.EXTRA_LOCATION, location);
        startService(intent);
    }


    class AddressResultReceiver extends ResultReceiver {

        AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            String mAddressOutput = resultData.getString(GeocoderIntentService.RESULT_ADDRESS);
            statusUpdate("Received result from geocoder intent service:\n" + mAddressOutput);

            if (resultCode == GeocoderIntentService.RESULT_SUCCESS) {
                statusUpdate(getString(R.string.address_found));
                addressText.setText(mAddressOutput);
            } else {
                statusUpdate(getString(R.string.no_address_found));
                addressText.setText(getString(R.string.no_address_found));
            }

        }

    }


}
