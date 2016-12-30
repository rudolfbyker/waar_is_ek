package com.example.dolf.waar_is_ek;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.google.android.gms.internal.zzs.TAG;


/**
 * An {@link IntentService} subclass for handling asynchronous geocoding task requests in
 * a service on a separate handler thread.
 */
public class GeocoderIntentService extends IntentService {

    protected static final String ACTION_FETCH_ADDRESS = "com.example.dolf.waar_is_ek.action.FOO";
    protected static final String EXTRA_LOCATION = "com.example.dolf.waar_is_ek.extra.LOCATION";
    protected static final String EXTRA_RECEIVER = "com.example.dolf.waar_is_ek.extra.RECEIVER";
    protected static final String RESULT_ADDRESS = "com.example.dolf.waar_is_ek.result.ADDRESS";
    protected static final int RESULT_SUCCESS = 0;
    protected static final int RESULT_FAILURE = 1;

    private static final Locale locale = Locale.getDefault();

    public GeocoderIntentService() {
        super("GeocoderIntentService");
    }


    /**
     * Starts this service. If the service is already performing a task this action will be queued.
     * @see IntentService
     */
//    public static void startActionFetchAddress(Context context, Location location) {
//        Intent intent = new Intent(context, GeocoderIntentService.class);
//        intent.setAction(ACTION_FETCH_ADDRESS);
//        intent.putExtra(EXTRA_LOCATION, location);
//        context.startService(intent);
//    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_FETCH_ADDRESS.equals(action)) {
                final Location location = intent.getParcelableExtra(EXTRA_LOCATION);
                final ResultReceiver receiver = intent.getParcelableExtra(EXTRA_RECEIVER);
                handleActionFetchAddress(location, receiver);
            }
        }
    }


    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFetchAddress(Location location, ResultReceiver receiver) {
        String errorMessage = "";
        List<Address> addresses = null;
        Geocoder geocoder = new Geocoder(this, locale);

        try {
            addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1
            );
        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            errorMessage = getString(R.string.service_not_available);
            Log.e(TAG, errorMessage, ioException);
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            errorMessage = getString(R.string.invalid_lat_long_used);
            Log.e(TAG, errorMessage + ". " +
                    "Latitude = " + location.getLatitude() +
                    ", Longitude = " +
                    location.getLongitude(), illegalArgumentException);
        }

        // Handle case where no address was found.
        if (addresses == null || addresses.size()  == 0) {
            if (errorMessage.isEmpty()) {
                errorMessage = getString(R.string.no_address_found);
                Log.e(TAG, errorMessage);
            }
            deliverResultToReceiver(RESULT_FAILURE, errorMessage, receiver);
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<>();

            // Fetch the address lines using getAddressLine,
            // join them, and send them to the thread.
            for(int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }
            Log.i(TAG, getString(R.string.address_found));
            deliverResultToReceiver(
                    RESULT_SUCCESS,
                    TextUtils.join(System.getProperty("line.separator"), addressFragments),
                    receiver
            );
        }

    }


    private void deliverResultToReceiver(int resultCode, String message, ResultReceiver receiver) {
        Bundle bundle = new Bundle();
        bundle.putString(RESULT_ADDRESS, message);
        receiver.send(resultCode, bundle);
    }

}
