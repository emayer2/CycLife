package edu.uw.cyclife.cyclifeapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class BTThread extends Thread {
    private final int NUM_BYTES = 900;
    private final String TAG = getClass().getSimpleName();
    private final UUID BT_UUID =  UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket socket = null;
    private InputStream inStream;
    private  OutputStream outStream;
    private byte[] inbuf;

    View text;

    private boolean isCrash = false;

    public BTThread(BluetoothAdapter adapter, String addr, View t) {
        BluetoothDevice device = adapter.getRemoteDevice(addr);
        BluetoothSocket temp = null;
        try {
            temp = device.createRfcommSocketToServiceRecord(BT_UUID);
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating input stream", e);
        }
        if (temp == null) {
            return;
        }
        socket = temp;  // Creation succeeded
        inbuf = new byte[NUM_BYTES];   // Create the data buffer
        text = t;
    }

    @Override
    public void run()  {
        super.run();
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
                if (totBytes < NUM_BYTES) {  // If we still need to keep reading
                    if (inStream.available() != 0) {  // If we have anything to read
                        numBytes = inStream.read(inbuf);
                        ((TextView)text).setText(inbuf[0] + " | " + numBytes);
//                        if (numBytes > 0) {  // If we read anything
//                            //  Store, and keep reading
//                            for (int i = 0; i < numBytes; i++) {
//                                tempBuf[totBytes + i] = inbuf[i];
//                                Log.d(TAG, inbuf[i] + "\n");
//                            }
//                            totBytes += numBytes;
//                        }
                    }
                } else {  // totBytes = NUM_BYTES, nothing left to read
                    // TODO: Analyze data
                    totBytes = 0;
                    // TODO: Send data back??
                    // write(...)
                }
            } catch (IOException e) {
                Log.d(TAG, "Input stream was disconnected", e);
                break;
            }
        }
    }
}
