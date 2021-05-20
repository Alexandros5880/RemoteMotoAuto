package com.remote.remotemotoauto;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Bluetooth extends AppCompatActivity {

    private static final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 1;
    private ListView show_BT_devices;
    private TextView textView_UP;
    private Button discoverable_btn, btn_ON, btn_OFF, btn_Paired, select_btn;
    private ImageView mBlueIv;
    private Map<String, BluetoothDevice> devices_map;
    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_DISCOVER_BT = 1;

    private BluetoothAdapter mBlueAdapter;
    private BluetoothAdapter mBlueDiscoverAdapter;

    // Reeceved devices list
    private Set<BluetoothDevice> recevedDevices;
    private BluetoothDevice selectedDevice;

    // Thread Socket
    public static ConnectThread conThread;

    // Hellper variable to show if socke is connected
    protected boolean isConected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        show_BT_devices = (ListView) findViewById(R.id.show_BT_devices);
        textView_UP = (TextView) findViewById(R.id.textView_UP);
        discoverable_btn = (Button) findViewById(R.id.discoverable_btn);
        btn_ON = (Button) findViewById(R.id.btn_ON);
        btn_OFF = (Button) findViewById(R.id.btn_OFF);
        btn_Paired = (Button) findViewById(R.id.btn_Paired);
        select_btn = (Button) findViewById(R.id.select_btn);
        mBlueIv = (ImageView) findViewById(R.id.mBlueIv);

        devices_map = new HashMap<String, BluetoothDevice>();

        // Adapter Bountend
        mBlueAdapter = BluetoothAdapter.getDefaultAdapter();
        // Adapter Receving new Devices
        recevedDevices = mBlueAdapter.getBondedDevices();
        mBlueDiscoverAdapter = mBlueAdapter.getDefaultAdapter();

        // Check if bluetooth is available or not
        if (mBlueAdapter == null) {
            textView_UP.setText("Bluetooth is not available.");
            textView_UP.setTextColor(Color.GREEN);
        } else {
            textView_UP.setText("Bluetooth is available.");
            textView_UP.setTextColor(Color.RED);
        }

        // Set image if bluetooth is on or off
        if (mBlueAdapter.isEnabled()) {
            mBlueIv.setImageResource(R.drawable.ic_bluetooth_on);
            textView_UP.setText("Bluetooth is available.");
            textView_UP.setTextColor(Color.GREEN);
        } else {
            mBlueIv.setImageResource(R.drawable.ic_bluetooth_off);
            textView_UP.setText("Bluetooth is not available.");
            textView_UP.setTextColor(Color.RED);
        }

        // Button ON
        btn_ON.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBlueAdapter.isEnabled()) {
                    showToast("Turning On Bluetooth...");
                    // Intent t on bluetooth
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, REQUEST_ENABLE_BT);
                    //textView_UP.setText("Bluetooth is available.");
                    //textView_UP.setTextColor(Color.GREEN);
                } else {
                    showToast("Bluetooth is already on");
                }
            }
        });

        // Button OFF
        btn_OFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBlueAdapter.isEnabled()) {
                    mBlueAdapter.disable();
                    showToast("Tutning Bluetooth Off");
                    mBlueIv.setImageResource(R.drawable.ic_bluetooth_off);
                    textView_UP.setText("Bluetooth is not available.");
                    textView_UP.setTextColor(Color.RED);
                } else {
                    showToast("Bluetooth is already off");
                }
            }
        });

        // Making device Discoverable
        discoverable_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBlueAdapter.isDiscovering()) {
                    showToast("Making Your Device Discoverable");
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300); // Discoverable for 300 seconds or 5 minutes
                    startActivityForResult(intent, REQUEST_DISCOVER_BT);
                }
            }
        });

        // Paired Button
        btn_Paired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBlueAdapter.isEnabled()) {
                    // Get Baired Devices
                    List<String> list = new ArrayList<String>();
                    Set<BluetoothDevice> devices = mBlueAdapter.getBondedDevices();
                    for (BluetoothDevice device : devices) {
                        devices_map.put(device.getName(), device);
                        list.add(device.getName());
                    }
                    ArrayAdapter array_adapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, list);
                    show_BT_devices.setAdapter(null);
                    show_BT_devices.setAdapter(array_adapter);
                }
            }
        });

        // show_BT_devices On Click
        show_BT_devices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected item text from ListView
                String selectedItem = (String) parent.getItemAtPosition(position);
                // Set the selected device
                selectedDevice = devices_map.get(selectedItem);
                showToast(selectedDevice.getName());
            }
        });

        // Select Button
        select_btn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                if (mBlueAdapter.isEnabled()) {
                    try {
                        // Make a Thread Connection
                        Log.e("ADDRESS:    ", selectedDevice.getAddress());
                        //showToast("ADDRESS:    " + selectedDevice.getAddress().getClass());
                        conThread = new ConnectThread(selectedDevice, mBlueAdapter);
                        conThread.run();
                        // Save the name of device
                        writeToFile(selectedDevice.getName(), getApplicationContext(), "saveData.txt");
                        // Restart Application
                        new MainActivity().restart();
                        /*
                        // Return to Main Activity
                        Intent main_intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(main_intent);
                        */
                    } catch( Exception e ) {
                        showToast("Select device!" );
                    }
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    // Bluetooth is on
                    mBlueIv.setImageResource(R.drawable.ic_bluetooth_on);
                    showToast("Bluetooth is on");
                    textView_UP.setTextColor(Color.GREEN);
                } else {
                    // User denied to turn bluetooth on
                    showToast("Could't on bluetooth");
                    textView_UP.setTextColor(Color.RED);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Toast message function
    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // Get Paired Devices and connect automatically
    public void autoConnect(String deviceName) { // HC-06    98:D3:11:F8:1D:91
        try {
            deviceName = deviceName.trim();
            this.devices_map = new HashMap<String, BluetoothDevice>();
            // Adapter Bountend
            this.mBlueAdapter = BluetoothAdapter.getDefaultAdapter();
            // Adapter Receving new Devices
            this.recevedDevices = mBlueAdapter.getBondedDevices();
            this.mBlueDiscoverAdapter = mBlueAdapter.getDefaultAdapter();
            if (mBlueAdapter.isEnabled()) {
                // Get Baired Devices
                Set<BluetoothDevice> devices = this.mBlueAdapter.getBondedDevices();
                for (BluetoothDevice device : devices) {
                    this.devices_map.put(device.getName(), device);
                }
                BluetoothDevice device = this.devices_map.get(deviceName);
                // Make a Thread Connection
                this.conThread = new ConnectThread(device, this.mBlueAdapter);
                this.conThread.run();
                this.isConected = this.conThread.isConnected;
            }
        } catch (NullPointerException ne) {
            Log.e("AUTO CONNECT LOG ", ne.getMessage());
        }
    }


    // Save Data Locally
    protected void writeToFile(String data,Context context, String fileName) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(fileName, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    // Read Data From File Locally
    protected String readFromFile(Context context, String fileName) {
        String ret = "";
        try {
            InputStream inputStream = context.openFileInput(fileName);
            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append("\n").append(receiveString);
                }
                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }
        return ret;
    }


}











class ConnectThread extends Thread {

    private static final String TAG = "TAG THREAD CLASS:      ";
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothAdapter bluetoothAdapter;

    // For Writing Variables
    private Handler handler; // handler that gets info from Bluetooth service
    private InputStream mmInStream = null;
    private OutputStream mmOutStream = null;
    private byte[] mmBuffer; // mmBuffer store for the stream

    // Helper variable shows if socked is connected
    protected boolean isConnected;

    // Defines several constants used when transmitting messages between the
    // service and the UI.
    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;

        // ... (Add other message types here as needed.)
    }

    public ConnectThread(BluetoothDevice device, BluetoothAdapter bluetoothAdapter) {
        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.
        BluetoothSocket tmp = null;
        mmDevice = device;
        this.bluetoothAdapter = bluetoothAdapter;

        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        mmSocket = tmp;

        // Get the input and output streams; using temp objects because
        // member streams are final.
        try {
            mmInStream = mmSocket.getInputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating input stream", e);
        }
        try {
            mmOutStream = mmSocket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating output stream", e);
        }

        this.isConnected = mmSocket.isConnected();
    }

    public void run() {
        // Cancel discovery because it otherwise slows down the connection.
        this.bluetoothAdapter.cancelDiscovery();

        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mmSocket.connect();
            this.isConnected = mmSocket.isConnected();
            Log.e(TAG, "  My Socket Connection:  " + mmSocket.isConnected());
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                mmSocket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
            return;
        }

        // The connection attempt succeeded. Perform work associated with
        // the connection in a separate thread.
        //manageMyConnectedSocket(mmSocket);
    }

    // Call this from the main activity to send data to the remote device.
    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
            // Share the sent message with the UI activity.
            Message writtenMsg = handler.obtainMessage(
                    MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
            writtenMsg.sendToTarget();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when sending data  IOException");
            /*
            // Send a failure message back to the activity.
            Message writeErrorMsg = handler.obtainMessage(MessageConstants.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString("toast","Couldn't send data to the other device");
            writeErrorMsg.setData(bundle);
            handler.sendMessage(writeErrorMsg);
            */
        }
    }


    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }


}
