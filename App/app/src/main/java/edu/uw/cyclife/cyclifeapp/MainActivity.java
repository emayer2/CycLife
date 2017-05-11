package edu.uw.cyclife.cyclifeapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.vision.text.Text;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final int REQUEST_ENABLE_BT = 1;  // For notify intent
    private static String mLat;
    private static String mLong;
    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private ArrayAdapter<String> deviceList;
    private ListView deviceListView;
    private boolean isMainButtonRed = true;

    private final int MAX_BAT_HEIGHT = 430;
    private int currHeight = 30;

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;


    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * Represents a geographical location.
     */
    protected Location mLastLocation;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    protected LocationRequest mLocationRequest;

    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    protected LocationSettingsRequest mLocationSettingsRequest;

    /**
     * Represents a geographical location.
     */
    protected Location mCurrentLocation;

    protected String mLatitudeLabel;
    protected String mLongitudeLabel;
    protected TextView mLatitudeText;
    protected TextView mLongitudeText;
    protected String mLastUpdateTime;
    protected TextView mLastUpdateTimeTextView;

    // UUID For Bluetooth
    private final String BT_UUID = "00001101-0000-1000-8000-00805F9B34FB";

//    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//
//            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//                // Discovery has found an object
//                BluetoothDevice foundDevice =
//                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                if (!pairedDevices.contains(foundDevice)) {
//                    pairedDevices.add(foundDevice);
//                    String deviceName = foundDevice.getName();
//                    String deviceHWAddr = foundDevice.getAddress();
//                    CharSequence currText = ((TextView) findViewById(R.id.main_text)).getText();
//                    deviceList.add(currText.toString() + "\nDevice: " + deviceName +
//                            ", Addr: " + deviceHWAddr);
//                    deviceList.notifyDataSetChanged();
//                }
//            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
//                ((TextView) findViewById(R.id.main_text))
//                        .setText("Discovering...");
//            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
//                ((TextView) findViewById(R.id.main_text))
//                        .setText("Done Searching!");
//            }
//        }
//    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                currHeight = Math.min(currHeight + 5, MAX_BAT_HEIGHT);
                setBatteryHeight(currHeight);
            }
        });

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        mLatitudeLabel = getResources().getString(R.string.latitude_label);
        mLongitudeLabel = getResources().getString(R.string.longitude_label);
        mLatitudeText = (TextView) findViewById((R.id.latitude_text));
        mLongitudeText = (TextView) findViewById((R.id.longitude_text));

        buildGoogleApiClient();
//        createLocationRequest();
//        buildLocationSettingsRequest();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        final Button button = (Button) findViewById(R.id.main_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isMainButtonRed) {
                    // Switch to green
                    findViewById(R.id.main_button).setBackgroundResource(R.drawable.power_button_green);
                    ((TextView) findViewById(R.id.main_text))
                            .setText("Deactivate");
                } else {
                    findViewById(R.id.main_button).setBackgroundResource(R.drawable.power_button_red);
                    ((TextView) findViewById(R.id.main_text))
                            .setText("Activate");
                }
                isMainButtonRed = !isMainButtonRed;

                // TODO: Start bluetooth, start connection, run
            }
        });

        // Create an instance of GoogleAPIClient.
//        if (mGoogleApiClient == null) {
//            mGoogleApiClient = new GoogleApiClient.Builder(this)
//                    .addConnectionCallbacks(this)
//                    .addOnConnectionFailedListener(this)
//                    .addApi(LocationServices.API)
//                    .build();
//        }

//        deviceListView = (ListView) findViewById(R.id.bt_list);
//        deviceList = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
//        deviceListView.setAdapter(deviceList);
//        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
//        {
//            @Override
//            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3)
//            {
//                String addr = (String)deviceListView.getItemAtPosition(position);
//                addr = addr.split(", ")[1];
//                ((TextView)findViewById(R.id.main_text)).setText(addr);
////                BTThread sock = new BTThread(bluetoothAdapter, addr);
////                sock.start();
//            }
//        });

        // Setup battery
        setBatteryHeight(currHeight);

//        // Bluetooth discovery
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
//        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
//        filter.addAction(BluetoothDevice.ACTION_FOUND);
//        registerReceiver(mReceiver, filter);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CALL_PHONE}, 1);
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

//        deviceList.add("1, 123456");
//        deviceList.add("2, 142536");
//        deviceList.add("3, 165432");
//        deviceList.add("4, 198656");
//        deviceList.notifyDataSetChanged();


//        // Enable bluetooth discovery
//        Intent discoverableIntent =
//                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
//        startActivity(discoverableIntent);
    }

    /**
     * Builds a GoogleApiClient. Uses the addApi() method to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    private void setBatteryHeight(int h) {
        int colg;
        int colr;
        if (h >= MAX_BAT_HEIGHT / 2) {
            colg = 0xf;
            colr = 15 - (int) (15 * ((double) (h - MAX_BAT_HEIGHT / 2)) / (MAX_BAT_HEIGHT / 2));
        } else {
            colr = 0xf;
            colg = (int) (15 * ((double) h)) / (MAX_BAT_HEIGHT / 2);
        }
        String color = "#ff";
        String colrstr = Integer.toHexString(colr);
        String colgstr = Integer.toHexString(colg);
        color += colrstr.charAt(colrstr.length() - 1);
        color += colrstr.charAt(colrstr.length() - 1);
        color += colgstr.charAt(colgstr.length() - 1);
        color += colgstr.charAt(colgstr.length() - 1);
        color += "00";
        View v = findViewById(R.id.main_battery_level);
        v.setBackgroundColor(Color.parseColor(color));
        ConstraintLayout.LayoutParams p = (ConstraintLayout.LayoutParams) v.getLayoutParams();
        p.height = h;
        v.setLayoutParams(p);
    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        // Unregister the ACTION_FOUND receiver
//        unregisterReceiver(mReceiver);
//    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.on_settings) {
            Intent settIntent = new Intent(this, SettingsActivity.class);
            startActivityForResult(settIntent, 1);
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            // Do nothing since we're in the home already
        } else if (id == R.id.nav_bluetooth) {
            Intent si = new Intent(this, BluetoothActivity.class);
            startActivity(si);
        } else if (id == R.id.nav_call) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                    == PackageManager.PERMISSION_GRANTED) {
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:1234567890"));
                startActivity(callIntent);
            } else {
                ((TextView) findViewById(R.id.main_text))
                        .setText("Phone Permission Not Enabled!");
            }
        } else if (id == R.id.nav_settings) {
            Intent settIntent = new Intent(this, SettingsActivity.class);
            startActivityForResult(settIntent, 1);
        } else if (id == R.id.nav_killswitch) {
            // TODO: Remove this testing
            Intent si = new Intent(this, KillswitchActivity.class);
            startActivity(si);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        // Provides a simple way of getting a device's location and is well suited for
        // applications that do not require a fine-grained location and that do not need location
        // updates. Gets the best and most recent location currently available, which may be null
        // in rare cases when a location is not available.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            mLatitudeText.setText(String.format("%s: %f", mLatitudeLabel,
                    mLastLocation.getLatitude()));
            mLat = (String.format("%s: %f", mLatitudeLabel,
                    mLastLocation.getLatitude()));
            mLong = (String.format("%s: %f", mLongitudeLabel,
                    mLastLocation.getLongitude()));
            mLongitudeText.setText(String.format("%s: %f", mLongitudeLabel,
                    mLastLocation.getLongitude()));
        } else {
            Toast.makeText(this, R.string.no_location_detected, Toast.LENGTH_LONG).show();
        }


//        // Updating Location Request
//        mLocationRequest = new LocationRequest();
//        mLocationRequest.setInterval(10000);
//        mLocationRequest.setFastestInterval(5000);
//        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//
//        // Build it
//        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
//                .addLocationRequest(mLocationRequest);
//
//        // Start it
//        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i("MainActivity", "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i("MainActivity", "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//        stopLocationUpdates();
//    }

//    protected void startLocationUpdates() {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
//        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
//    }

//    @Override
//    public void onLocationChanged(Location location) {
//        mCurrentLocation = location;
//        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
//        updateUI();
//    }

//    private void updateUI() {
//        mLatitudeText.setText(String.valueOf(mCurrentLocation.getLatitude()));
//        mLongitudeText.setText(String.valueOf(mCurrentLocation.getLongitude()));
//        mLastUpdateTimeTextView.setText(mLastUpdateTime);
//    }

//    protected void stopLocationUpdates() {
//        LocationServices.FusedLocationApi.removeLocationUpdates(
//                mGoogleApiClient, this);
//    }

//    @Override
//    public void onResume() {
//        super.onResume();
//        if (mGoogleApiClient.isConnected()) {
//            startLocationUpdates();
//        }
//    }

//    protected void createLocationRequest() {
//        mLocationRequest = new LocationRequest();
//
//        // Sets the desired interval for active location updates. This interval is
//        // inexact. You may not receive updates at all if no location sources are available, or
//        // you may receive them slower than requested. You may also receive updates faster than
//        // requested if other applications are requesting location at a faster interval.
//        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
//
//        // Sets the fastest rate for active location updates. This interval is exact, and your
//        // application will never receive updates faster than this value.
//        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
//
//        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//    }
//
//    /**
//     * Uses a {@link com.google.android.gms.location.LocationSettingsRequest.Builder} to build
//     * a {@link com.google.android.gms.location.LocationSettingsRequest} that is used for checking
//     * if a device has the needed location settings.
//     */
//    protected void buildLocationSettingsRequest() {
//        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
//        builder.addLocationRequest(mLocationRequest);
//        mLocationSettingsRequest = builder.build();
//    }

//    public static String getLat() {
//        return mLat;
//    }
//
//    public static String getLong() {
//        return mLong;
//    }
}
