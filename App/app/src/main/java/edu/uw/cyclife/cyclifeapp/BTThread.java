package edu.uw.cyclife.cyclifeapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Observable;
import java.util.Observer;
import java.util.UUID;


public class BTThread extends Thread implements Observer {
    private final int NUM_BYTES = 900;
    private final String TAG = getClass().getSimpleName();
    private final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket socket = null;
    private InputStream inStream;
    private OutputStream outStream;
    private byte[] inbuf;
    private FileOutputStream outputStreamWriter;
    private KSWrapper ksWatcher;

    // Alarm packet
    public final byte ALARM = 0b01101111;


    private boolean isCrash = false;

    public BTThread(BluetoothAdapter adapter, BluetoothDevice device, KSWrapper ks) {
        BluetoothSocket temp = null;
        try {
            temp = device.createInsecureRfcommSocketToServiceRecord(BT_UUID);
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating input stream", e);
        }
        if (temp == null) {
            return;
        }
        socket = temp;  // Creation succeeded
        inbuf = new byte[NUM_BYTES];   // Create the data buffer
        adapter.cancelDiscovery();
        openFile();
        ksWatcher = ks;
    }

    @Override
    public void update(Observable o, Object arg) {
        KSWrapper k = (KSWrapper)o;
        if (k.getAlarm()) {
            try {
                socket.close();
                outputStreamWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
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

        int byteCount = 0;
        int currData = 0;
        int numData = 6;

        float data[] = new float[numData];
        byte bytes[] = new byte[4];
        for (int i = 0; i < numData; i++) {
            data[i] = 0;
        }
        boolean connected = false;
        boolean done = false;
        while (!done) {
            try {
                if (!connected) {
                    outStream.write(new byte[]{(byte) 0b11001010});
                    connected = true;
                } else if (inStream.available() != 0) {  // If we have anything to read
                    numBytes = inStream.read(inbuf);
                    for (int i = 0; i < numBytes && !done; i++) {
                        if (inbuf[i] == ALARM) {
                            ksWatcher.alarmOn();
                            while (ksWatcher.getAlarm())  {
                                // Wait...
                            }
                            outStream.write(0b10010011);
                            done = true;
                            break;
                        }
//                        bytes[byteCount] = inbuf[i];
//                        byteCount++;
//                        if (byteCount == 4) {
//                            data[currData] = ByteBuffer.wrap(bytes)
//                                    .order(ByteOrder.LITTLE_ENDIAN).getFloat();
//                            //                            data  = ByteBuffer.wrap(bytes)
//                            //                                    .order(ByteOrder.LITTLE_ENDIAN).getFloat();
//                            byteCount = 0;
//                            //                            writeToFile(data);
//                            if (currData == numData - 1) {
//                                writeToFile(data);
//                            }
//                            currData = (currData + 1) % numData;
//                        }
                    }
                }
            } catch (IOException e) {
                try {
                    outputStreamWriter.close();
                } catch (IOException ef) {
                    Log.e("Exception", "File close failed: " + ef.toString());
                }
                Log.d(TAG, "Input stream was disconnected", e);
                return;
            }
        }
        try {
            outputStreamWriter.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openFile() {
        String state = Environment.getExternalStorageState();
        while (!Environment.MEDIA_MOUNTED.equals(state)) {
            // Wait...
        }

        File dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "data");
        if (!dir.exists() && !dir.mkdirs()) {
            Log.e("Exception", "File directory creation failed");
        }  // This will work everytime since I made the directory already (previous run)

        int runNum = 0;
        if (dir != null) {
            File[] f = dir.listFiles();
            if (f != null) {
                runNum = f.length;
            }
        }
        File f = new File(dir, "run" + runNum + ".txt");
        try {
            outputStreamWriter = new FileOutputStream(f, true);
        } catch (IOException e) {
            Log.e("Exception", "File not found: " + e.toString());
        }
    }

    private void writeToFile(float data[]) {
        String state = Environment.getExternalStorageState();
        while (!Environment.MEDIA_MOUNTED.equals(state)) {
            // Wait...
        }

        File dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "data");
        if (!dir.exists() && !dir.mkdirs()) {
            Log.e("Exception", "File directory creation failed");
        }  // This will work everytime since I made the directory already (previous run)

        StringBuilder str = new StringBuilder();
        str.append(data[0]);
        for (int i = 1; i < data.length; i++) {
            str.append(", " + data[i]);
        }
        str.append("\n");
        Log.e("Exception", str.toString());

        File f = new File(dir, "run.txt");
        try {
            outputStreamWriter.write(str.toString().getBytes());
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }
}
