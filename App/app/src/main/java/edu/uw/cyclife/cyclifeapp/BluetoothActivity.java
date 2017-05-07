package edu.uw.cyclife.cyclifeapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class BluetoothActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;  // For notify intent
    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> foundDevices;
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
                if (!foundDevices.contains(foundDevice)) {
                    foundDevices.add(foundDevice);
                    String deviceName = foundDevice.getName();
                    String deviceHWAddr = foundDevice.getAddress();
                    String addstr = "";
                    if (deviceName != null) {
                        addstr += "\nDevice: " + deviceName +", ";
                    }
                    addstr += "Addr: " + deviceHWAddr;
                    deviceList.add(addstr);
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

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        deviceListView = (ListView) findViewById(R.id.bt_list);
        deviceList = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        deviceListView.setAdapter(deviceList);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3)
            {
                showToast("Pairing...");
                String addr = (String)deviceListView.getItemAtPosition(position);
                addr = addr.split("Addr: ")[1];
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(addr);
                try {
                    Method m = device.getClass().getMethod("createBond", (Class[])null);
                    m.invoke(device, (Object[])null);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                    showToast("Error pairing with device");
                    return;
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                    showToast("Error pairing with device");
                    return;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    showToast("Error pairing with device");
                    return;
                }
                BTThread sock = new BTThread(bluetoothAdapter, addr,
                        findViewById(R.id.main_text));
                sock.start();
            }
        });

        // Bluetooth discovery
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

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
                    foundDevices = new HashSet<>();
                    pairedDevices = new HashSet<>();
                    bluetoothAdapter.startDiscovery();
                }
            } else {
                ((TextView) findViewById(R.id.main_text))
                        .setText("Location Permission Not Enabled!");
            }
        }
    }
}
