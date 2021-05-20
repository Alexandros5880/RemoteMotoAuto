package com.remote.remotemotoauto;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    public Button bluetooth_btn, start_engine_btn, settings_btn, password_btn;
    public Switch electricity_swt, stop_btn, saved_mode_swt;
    public static Intent bluetooth_intent, settings_intent, password_intent;
    public ImageView home_moto_img;
    // Thread Socket
    public ConnectThread conThread;
    public TextView connection_view;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetooth_btn = (Button) findViewById(R.id.bluetooth_connection);
        electricity_swt = (Switch) findViewById(R.id.electricity_swt);
        start_engine_btn = (Button) findViewById(R.id.start_engine_btn);
        stop_btn = (Switch) findViewById(R.id.stop_btn);
        settings_btn = (Button) findViewById(R.id.settings_btn);
        home_moto_img = (ImageView) findViewById(R.id.home_moto_img);
        password_btn = (Button) findViewById(R.id.password_btn);
        saved_mode_swt = (Switch) findViewById(R.id.saved_mode_swt);
        connection_view = (TextView) findViewById(R.id.connection_view);


        // Get Saved device name
        Bluetooth b = new Bluetooth();
        String deviceName = b.readFromFile(getApplicationContext(), "saveData.txt");
        Log.e("GET DEVICE NAME ", deviceName);
        b.autoConnect(deviceName); // HC-06    98:D3:11:F8:1D:91
        //b.autoConnect("HC-06");
        // If Bluetooth is connected show it on text view
        if ( b.isConected ) {
            connection_view.setText("Connected");
            connection_view.setTextColor(Color.GREEN);
        } else {
            connection_view.setText("Desconnected");
            connection_view.setTextColor(Color.RED);
        }

        // Get if Saved Mode is enable
        String savedMode = b.readFromFile(getApplicationContext(), "Saved_Mode_File.txt");
        Log.e("SAVED MODE ", (String)savedMode);
        if (savedMode.contains("saved_on")) {
            saved_mode_swt.setChecked(true);
        } else if (savedMode.contains("saved_off")) {
            saved_mode_swt.setChecked(false);
        }

        // Setup Start engine button layout
        start_engine_btn.setBackgroundColor(Color.LTGRAY);
        start_engine_btn.setWidth(400);
        start_engine_btn.setHeight(100);

        // Get the bluetooth thread instance
        conThread = Bluetooth.conThread;

        // Go to Bluetooth settings activity
        bluetooth_intent = new Intent(this, Bluetooth.class);
        bluetooth_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(bluetooth_intent);
            }
        });
        // Go to Settings activity
        settings_intent = new Intent(this, SettingsActivity.class);
        settings_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(settings_intent);
            }
        });

        // Go to password Activity
        password_intent = new Intent(this, Password.class);
        password_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(password_intent);
            }
        });

        // START Electricity Button
        electricity_swt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String on = "on_power";
                String off = "off_power";
                try {
                    if (isChecked) {
                        if (!saved_mode_swt.isChecked()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (stop_btn.isChecked()) {
                                        showToast("Stop Switch is ON");
                                        stop_btn.setChecked(false);
                                    }
                                }
                            });
                            Thread.sleep(333);
                            conThread.write(on.getBytes());
                        } else {
                            // Switch off electricity
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (electricity_swt.isChecked()) {
                                        electricity_swt.setChecked(false);
                                    }
                                }
                            });
                            showToast("Switch off saved mode.");
                        }
                    } else {
                        conThread.write(off.getBytes());
                    }
                } catch (NullPointerException | InterruptedException ne) {
                    Log.e("Main Activity: ", ne.getMessage());
                }
            }
        });

        // Saved mode switch
        saved_mode_swt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String on = "saved_on";
                String off = "saved_off";
                Bluetooth b = new Bluetooth();
                try {
                    if (isChecked) {
                        // If STOP button is on switch off
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (stop_btn.isChecked()) {
                                    showToast("Electricity Switch is ON");
                                    stop_btn.setChecked(false);
                                }
                            }
                        });
                        Thread.sleep(333);
                        // If Electricity button is on Switch Off
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (electricity_swt.isChecked()) {
                                    electricity_swt.setChecked(false);
                                }
                            }
                        });
                        Thread.sleep(333);
                        conThread.write(on.getBytes());
                        b.writeToFile(on, getApplicationContext(), "Saved_Mode_File.txt");
                    } else {
                        conThread.write(off.getBytes());
                        b.writeToFile(off, getApplicationContext(), "Saved_Mode_File.txt");
                    }
                } catch (NullPointerException | InterruptedException ne) {
                    Log.e("Main Activity: ", ne.getMessage());
                }
            }
        });

        // START Engine Button
        start_engine_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String start_engine = "on_engine";
                try {
                    if (electricity_swt.isChecked()) {
                        conThread.write(start_engine.getBytes());
                    } else {
                        showToast("Switch Electricity ON");
                    }
                } catch (NullPointerException ne) {
                    Log.e("Main Activity: ", ne.getMessage());
                }
            }
        });
        // START Engine Button
        start_engine_btn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    start_engine_btn.setBackgroundColor(Color.LTGRAY);
                } else if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (electricity_swt.isChecked()) {
                        start_engine_btn.setBackgroundColor(Color.GREEN);
                    } else {
                        //showToast("Switch ON Electricity");
                    }
                }
                return false;
            }
        });


        // STOP Button
        stop_btn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                final String on = "Stop";
                String off = "Stop_off";
                try {
                    if (isChecked) { // I can't send to bluetooth and change the switch togaither
                        if (! saved_mode_swt.isChecked()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (electricity_swt.isChecked()) {
                                        showToast("Electricity Switch is ON");
                                        electricity_swt.setChecked(false);
                                    }
                                }
                            });
                            Thread.sleep(333);
                            conThread.write(on.getBytes());
                        } else {
                            // Switch off Stop Button
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (stop_btn.isChecked()) {
                                        stop_btn.setChecked(false);
                                    }
                                }
                            });
                            showToast("Switch off saved mode.");
                        }
                    } else {
                        conThread.write(off.getBytes());
                    }
                } catch (NullPointerException | InterruptedException ne) {
                    Log.e("Main Activity: ", ne.getMessage());
                }
            }
        });
    }


    // Restart Application
    public void restart() {
        Intent i = getBaseContext().getPackageManager().
                getLaunchIntentForPackage(getBaseContext().getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    // Toast message function
    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

}