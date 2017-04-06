package edu.uw.cyclife.cyclifeapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.media.audiofx.BassBoost;
import android.net.Uri;
import android.os.Bundle;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.content.pm.PackageManager;

import java.util.HashSet;
import java.util.Set;

import static java.security.AccessController.getContext;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static final int REQUEST_ENABLE_BT = 1;  // For notify intent
    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private ArrayAdapter<String> deviceList;
    private ListView deviceListView;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found an object
                BluetoothDevice foundDevice =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (!pairedDevices.contains(foundDevice)) {
                    pairedDevices.add(foundDevice);
                    String deviceName = foundDevice.getName();
                    String deviceHWAddr = foundDevice.getAddress();
                    CharSequence currText = ((TextView) findViewById(R.id.main_text)).getText();
                    deviceList.add(currText.toString() + "\nDevice: " + deviceName +
                                    ", Addr: " + deviceHWAddr);
                    deviceList.notifyDataSetChanged();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                ((TextView) findViewById(R.id.main_text))
                        .setText("Discovering...");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                ((TextView) findViewById(R.id.main_text))
                        .setText("Done Searching!");
            }
        }
    };

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
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        deviceListView = (ListView) findViewById(R.id.bt_list);
        deviceList = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        deviceListView.setAdapter(deviceList);

        // Bluetooth discovery
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CALL_PHONE}, 1);
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);


//        // Enable bluetooth discovery
//        Intent discoverableIntent =
//                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
//        startActivity(discoverableIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the ACTION_FOUND receiver
        unregisterReceiver(mReceiver);
    }

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
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                // Device does not support Bluetooth
                ((TextView)findViewById(R.id.main_text)).setText("No Bluetooth Support :(");
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                    // Bluetooth found, check if enabled and prompt
                    if (!bluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    }
                    // Check if location enabled, and prompt
                    if (!bluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    }
                    if (!((LocationManager) getSystemService(Context.LOCATION_SERVICE))
                            .isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        ((TextView)findViewById(R.id.main_text)).setText("Location Not Turned On :(");
                    } else {

                        // Start discovery, first ask for location permissions
                        // (for hosts of android OS >= 6.0)
                        if (bluetoothAdapter.isDiscovering()) {
                            bluetoothAdapter.cancelDiscovery();
                        }
                        deviceList.clear();
                        pairedDevices = new HashSet<>();
                        bluetoothAdapter.startDiscovery();
                    }
                } else {
                    ((TextView) findViewById(R.id.main_text))
                            .setText("Location Permission Not Enabled!");
                }
            }

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
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
