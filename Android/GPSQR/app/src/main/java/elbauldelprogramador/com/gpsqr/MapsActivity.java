package elbauldelprogramador.com.gpsqr;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import com.software.shell.fab.ActionButton;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {


    protected static final String TAG = MapsActivity.class.getSimpleName();

    // Keys for storing activity state in the Bundle.
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";

    /**
     * Regex to extract Lat, Lng from strings like this: LATITUD_37.19735641547103_LONGITUD_-3.623774830675075
     */
    private static final Pattern pat = Pattern.compile("[A-Z]+_(-?\\d+\\.\\d+)");

    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    protected Boolean mRequestingLocationUpdates;

    /**
     * Represents a geographical location.
     */
    protected Location mCurrentLocation;

    /**
     * Last known location, used for drawing path between two Locations
     */
    protected Location mPreviousLocation;
    /**
     * UI things
     */
    private Activity mAct;
    private GoogleMap mMap;
    /**
     * The coordinates readed from the QR code
     */
    private double[] mCoord = new double[2]; // 0:lat,1:lng
    /**
     * Broadcast Receiver to receive location updates
     */
    private BroadcastReceiver mLocationReceiver;

    /**
     * Intent for launch the Service
     */
    private Intent mRequestLoctionIntent;

    public void updateMap() {
        if (mPreviousLocation != null) {
            PolylineOptions polylineOptions = new PolylineOptions()
                    .add(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()))
                    .add(new LatLng(mPreviousLocation.getLatitude(), mPreviousLocation.getLongitude()))
                    .color(Color.RED)
                    .width(5);
            mMap.addPolyline(polylineOptions);
        }
        LatLng sydney = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        mMap.addMarker(new MarkerOptions().position(sydney).title("TITLE"));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(sydney, 21f));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mAct = this;
        mRequestingLocationUpdates = true;

        final ActionButton ab = (ActionButton) findViewById(R.id.action_button);

        ab.setImageResource(R.drawable.fab_plus_icon);
        ab.setShowAnimation(ActionButton.Animations.JUMP_FROM_DOWN);
        ab.setHideAnimation(ActionButton.Animations.JUMP_TO_DOWN);
        ab.setButtonColor(getResources().getColor(R.color.fab_material_amber_500));
        ab.setButtonColorPressed(getResources().getColor(R.color.fab_material_amber_900));

        ab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ab.hide();
                new IntentIntegrator(mAct).initiateScan();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ab.show();
                    }
                }, 1000);
            }
        });

        // TODO:
        mLocationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mPreviousLocation = mCurrentLocation;
                mCurrentLocation = intent.getParcelableExtra(LocationUpdaterService.COPA_MESSAGE);
                updateMap();
            }
        };

        mRequestLoctionIntent = new Intent(this, LocationUpdaterService.class);
        startService(mRequestLoctionIntent);

        // Update values using data stored in the Bundle.
//        updateValuesFromBundle(savedInstanceState);

    }

//    /**
//     * Updates fields based on data stored in the bundle.
//     *
//     * @param savedInstanceState The activity state saved in the Bundle.
//     */
//    private void updateValuesFromBundle(Bundle savedInstanceState) {
//        Log.i(TAG, "Updating values from bundle");
//        if (savedInstanceState != null) {
//            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
//            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
//            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
//                mRequestingLocationUpdates = savedInstanceState.getBoolean(
//                        REQUESTING_LOCATION_UPDATES_KEY);
////          TODO:      setButtonsEnabledState();
//            }
//
//            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
//            // correct latitude and longitude.
//            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
//                // Since LOCATION_KEY was found in the Bundle, we can be sure that mCurrentLocation
//                // is not null.
//                mPreviousLocation = mCurrentLocation;
//                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
//            }
//
//            // Update the value of mLastUpdateTime from the Bundle and update the UI.
//            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
//                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
//            }
//            updateMap();
//        }
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Log.d("MainActivity", "Cancelled scan");
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                Log.d("MainActivity", "Scanned");
                Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
                Matcher m = pat.matcher(result.getContents());
                int i = 0;
                while (m.find()) {
                    mCoord[i++] = Double.parseDouble(m.group(1));
                }
                // Add a marker in Sydney and move the camera
                LatLng sydney = new LatLng(mCoord[0], mCoord[1]);
                mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(sydney, 21.0f));

//                GoogleDirection.withServerKey(getString(R.string.google_maps_server_key))
//                        .from(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()))
//                        .to(new LatLng(mCoord[0], mCoord[1]))
//                        .transportMode(TransportMode.WALKING)
//                        .execute(new DirectionCallback() {
//                            @Override
//                            public void onDirectionSuccess(Direction direction) {
//                                if (direction.isOK()) {
//                                    Toast.makeText(getApplicationContext(), "DIRECTION KOK", Toast.LENGTH_LONG).show();
//                                    ArrayList<LatLng> directionPositionList = direction.getRouteList().get(0).getLegList().get(0).getDirectionPoint();
//                                    PolylineOptions polylineOptions = DirectionConverter.createPolyline(getApplicationContext(), directionPositionList, 5, Color.BLUE);
//                                    mMap.addPolyline(polylineOptions);
//                                } else {
//                                    Toast.makeText(getApplicationContext(), "NOT OK" + direction.getStatus(), Toast.LENGTH_LONG).show();
//                                }
//                            }
//
//                            @Override
//                            public void onDirectionFailure(Throwable t) {
//                                Toast.makeText(getApplicationContext(), "Failure", Toast.LENGTH_LONG).show();
//                            }
//                        });

            }
        } else {
            // This is important, otherwise the result will not be passed to the fragment
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-1, -1), 12.0f));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Within {@code onPause()}, we pause location updates, but leave the
        // connection to GoogleApiClient intact.  Here, we resume receiving
        // location updates if the user has requested them.

        LocalBroadcastManager.getInstance(this).registerReceiver(mLocationReceiver, new IntentFilter(LocationUpdaterService.COPA_RESULT));

//        if (!mIsReceiverRegistered) {
//            if (mLocationReceiver == null)
//                mLocationReceiver = new LocationReceiver();
//            IntentFilter filter = new IntentFilter(LocationReceiver.ACTION);
//            this.registerReceiver(mLocationReceiver, filter);
//            mIsReceiverRegistered = true;
//        }

        // TODO: STop serice here or in ondestroy
//        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
//            startLocationUpdates();
//        }
    }

    @Override
    protected void onDestroy() {
        stopService(mRequestLoctionIntent);

        super.onDestroy();
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocationReceiver);
        super.onStop();
    }

//    /**
//     * Stores activity data in the Bundle.
//     */
//    public void onSaveInstanceState(Bundle savedInstanceState) {
//        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
//        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
//        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
//        super.onSaveInstanceState(savedInstanceState);
//    }

}