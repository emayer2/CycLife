package edu.uw.cyclife.cyclifeapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class BTThread extends Thread {
    private final int NUM_BYTES = 9;
    private final String TAG = getClass().getSimpleName();
    private final UUID BT_UUID =  UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket socket = null;
    private InputStream inStream;
    private  OutputStream outStream;
    private byte[] inbuf;

    public BTThread(BluetoothAdapter adapter, String addr) {
        BluetoothDevice device = adapter.getRemoteDevice(addr);
        BluetoothSocket temp = null;
        try {
            temp = device.createRfcommSocketToServiceRecord(BT_UUID);
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating input stream", e);
        }
        socket = temp;  // Creation succeeded
        inbuf = new byte[NUM_BYTES];   // Create the data buffer
    }

    @Override
    public void run()  {
        // Connect to the device
        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            socket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                socket.close();
            } catch (IOException closeException) {
                Log.d(TAG, "Could not close the client socket", closeException);
            }
            return;
        }

        InputStream tempIn = null;
        OutputStream tempOut = null;
        try {
            tempIn = socket.getInputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating input stream", e);
            return;
        }
        try {
            tempOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating output stream", e);
            return;
        }
        inStream = tempIn;
        outStream = tempOut;

        byte[] tempBuf = new byte[NUM_BYTES];
        int totBytes = 0;  // Total number of bytes read
        int numBytes;  // Current number of bytes from read()
        while (true) {
            try {
                numBytes = inStream.read(inbuf);
                if (totBytes < NUM_BYTES) {
                    //  Store, and keep reading
                    for (int i = 0; i < numBytes; i++) {
                        tempBuf[totBytes + i] = inbuf[i];
                    }
                    totBytes += numBytes;
                } else {  // totBytes = NUM_BYTES
                    // TODO: Analyze data
                    totBytes = 0;
                }

                // TODO: Send data back??
            } catch (IOException e) {
                Log.d(TAG, "Input stream was disconnected", e);
                break;
            }
        }
    }
}
