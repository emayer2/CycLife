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
import android.support.v4.app.ActivityCompat;
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
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        // Paired devices setup
        MainActivity.pdeviceListView = (ListView) findViewById(R.id.btp_list);
        MainActivity.pdeviceList = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        MainActivity.pdeviceListView.setAdapter(MainActivity.pdeviceList);
        MainActivity.pdeviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3)
            {
                showToast("Connecting...");
                String addr = (String)MainActivity.pdeviceListView.getItemAtPosition(position);
                addr = addr.split("Addr: ")[1];
                BluetoothDevice device = MainActivity.bluetoothAdapter.getRemoteDevice(addr);
                MainActivity.sock = new BTThread(MainActivity.bluetoothAdapter, device,
                        MainActivity.ks);
                MainActivity.sock.start();
                finish();
            }
        });

        // Add already paired devices
        for (BluetoothDevice b : MainActivity.pairedDevices) {
            String deviceName = b.getName();
            String deviceHWAddr = b.getAddress();
            String addstr = "";
            if (deviceName != null) {
                addstr += "\nDevice: " + deviceName + ", ";
            }
            addstr += "Addr: " + deviceHWAddr;
            MainActivity.pdeviceList.add(addstr);
        }
        MainActivity.pdeviceList.notifyDataSetChanged();

        // Found devices setup
        MainActivity.fdeviceListView = (ListView) findViewById(R.id.btf_list);
        MainActivity.fdeviceList = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        MainActivity.fdeviceListView.setAdapter(MainActivity.fdeviceList);
        MainActivity.fdeviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3)
            {
                showToast("Connecting...");
                String addr = (String)MainActivity.fdeviceListView.getItemAtPosition(position);
                addr = addr.split("Addr: ")[1];
                BluetoothDevice device = MainActivity.bluetoothAdapter.getRemoteDevice(addr);
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
                MainActivity.sock = new BTThread(MainActivity.bluetoothAdapter, device,
                        MainActivity.ks);
                MainActivity.sock.start();
                finish();
            }
        });

        if (MainActivity.bluetoothAdapter == null) {
            // Device does not support Bluetooth
            showToast("No Bluetooth Support :(");
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                // Bluetooth found, check if enabled and prompt
                if (!MainActivity.bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, MainActivity.REQUEST_ENABLE_BT);
                }
                // Check if location enabled, and prompt
                if (!MainActivity.bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, MainActivity.REQUEST_ENABLE_BT);
                }
                if (!((LocationManager) getSystemService(Context.LOCATION_SERVICE))
                        .isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    showToast("Location Not Turned On :(");
                } else {

                    // Start discovery, first ask for location permissions
                    // (for hosts of android OS >= 6.0)
                    if (MainActivity.bluetoothAdapter.isDiscovering()) {
                        MainActivity.bluetoothAdapter.cancelDiscovery();
                    }
                    MainActivity.fdeviceList.clear();
                    MainActivity.foundDevices = new HashSet<>();
                    MainActivity.bluetoothAdapter.startDiscovery();
                }
            } else {
                showToast("Location Permission Not Enabled!");
            }
        }
    }
}
