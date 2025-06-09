package com.example.ecobot;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {
    private Handler handler = new Handler();
    private Runnable sendCommandRunnable;
    private static final int COMMAND_INTERVAL_MS = 100;

    private SeekBar speedSeekBar, lifterSeekBar;
    private TextView speedLabel, lifterAngleLabel;
    private int currentSpeed = 128;

    private static final int REQUEST_BLUETOOTH_CONNECT = 1;

    private TextView connectionStatus;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;

    private final UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private Button connectBtn, forwardBtn, backBtn, leftBtn, rightBtn, stopBtn, grabBtn;

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);

        speedSeekBar = findViewById(R.id.speedSeekBar);
        speedLabel = findViewById(R.id.speedLabel);
        lifterSeekBar = findViewById(R.id.lifterSeekBar);
        lifterAngleLabel = findViewById(R.id.lifterAngleLabel);

        connectionStatus = findViewById(R.id.connectionStatus);
        connectBtn = findViewById(R.id.connectBtn);
        forwardBtn = findViewById(R.id.forwardBtn);
        backBtn = findViewById(R.id.backBtn);
        leftBtn = findViewById(R.id.leftBtn);
        rightBtn = findViewById(R.id.rightBtn);
        stopBtn = findViewById(R.id.stopBtn);
        grabBtn = findViewById(R.id.grabBtn);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show();
            finish();
        }

        speedSeekBar.setMax(255);
        speedSeekBar.setProgress(currentSpeed);
        speedLabel.setText("Speed: " + currentSpeed);
        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentSpeed = progress;
                speedLabel.setText("Speed: " + currentSpeed);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        lifterSeekBar.setMax(180);
        lifterSeekBar.setProgress(90);
        lifterAngleLabel.setText("Lifter Angle: 90°");
        lifterSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int angle, boolean fromUser) {
                lifterAngleLabel.setText("Lifter Angle: " + angle + "°");
                sendCommand("LFT:" + angle);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        connectBtn.setOnClickListener(v -> checkAndShowDevices());

        forwardBtn.setOnTouchListener(createHoldListener("F"));
        backBtn.setOnTouchListener(createHoldListener("B"));
        leftBtn.setOnTouchListener(createHoldListener("L"));
        rightBtn.setOnTouchListener(createHoldListener("R"));

        stopBtn.setOnClickListener(v -> sendCommand("S"));
        grabBtn.setOnClickListener(v -> sendCommand("G"));
    }

    @SuppressLint("ClickableViewAccessibility")
    private View.OnTouchListener createHoldListener(String direction) {
        return (v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    sendCommandRunnable = new Runnable() {
                        public void run() {
                            sendCommand(direction);
                            handler.postDelayed(this, COMMAND_INTERVAL_MS);
                        }
                    };
                    handler.post(sendCommandRunnable);
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (sendCommandRunnable != null) handler.removeCallbacks(sendCommandRunnable);
                    sendCommand("S");
                    return true;
            }
            return false;
        };
    }

    private void checkAndShowDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_CONNECT);
        } else {
            showDeviceList();
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void showDeviceList() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        ArrayAdapter<String> deviceNames = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        BluetoothDevice[] deviceArray = new BluetoothDevice[pairedDevices.size()];

        int i = 0;
        for (BluetoothDevice device : pairedDevices) {
            deviceNames.add(device.getName() + "\n" + device.getAddress());
            deviceArray[i++] = device;
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Device")
                .setAdapter(deviceNames, (dialog, which) -> connectToDevice(deviceArray[which]))
                .show();
    }

    private void connectToDevice(BluetoothDevice device) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                return;
            }

            bluetoothSocket = device.createRfcommSocketToServiceRecord(DEFAULT_UUID);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();

            connectionStatus.setText("Connected");
            connectionStatus.setTextColor(Color.parseColor("#4CAF50"));
            Toast.makeText(this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            connectionStatus.setText("Not Connected");
            connectionStatus.setTextColor(Color.parseColor("#FF5252"));
            Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void sendCommand(String command) {
        if (outputStream != null) {
            try {
                String fullCommand = command.contains(":") ? command + "\n" : command + ":" + currentSpeed + "\n";
                outputStream.write(fullCommand.getBytes());
            } catch (IOException e) {
                Toast.makeText(this, "Failed to send command", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (bluetoothSocket != null) bluetoothSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_CONNECT &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showDeviceList();
        } else {
            Toast.makeText(this, "Bluetooth permission is required", Toast.LENGTH_SHORT).show();
        }
    }
}
