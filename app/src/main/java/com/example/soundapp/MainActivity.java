package com.example.soundapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.soundapp.Services.EqualizerService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private SeekBar[] seekBars;
    private TextView[] txtBands;
    private Spinner spnPreset;
    private int numberOfBands = 5;
    SharedPreferences prefs;
    private boolean isSpinnerInitialized = false;
    private boolean isPresetSetup = false;
    private static final int NOTIFICATION_PERMISSION_CODE = 100;
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Kiểm tra và yêu cầu quyền nếu cần
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            } else {
                ContextCompat.startForegroundService(this,
                        new Intent(this, EqualizerService.class));
            }
        } else {
            ContextCompat.startForegroundService(this,
                    new Intent(this, EqualizerService.class));
        }
        prefs = getSharedPreferences("EQ_PREFS", MODE_PRIVATE);

        // Ánh xạ view
        seekBars = new SeekBar[numberOfBands];
        txtBands = new TextView[numberOfBands];
        spnPreset = findViewById(R.id.spinnerPreset);

        for (int i = 0; i < numberOfBands; i++) {
            // giả sử id theo tên seekBarBand1, seekBarBand2,... và txtBand1, txtBand2,...
            int seekId = getResources().getIdentifier("seekBarBand" + (i + 1), "id", getPackageName());
            int txtId = getResources().getIdentifier("txtBand" + (i + 1), "id", getPackageName());

            seekBars[i] = findViewById(seekId);
            txtBands[i] = findViewById(txtId);
        }
        IntentFilter filter = new IntentFilter(EqualizerService.ACTION_READY);
        registerReceiver(eqReadyReceiver, filter);
    }
    private final BroadcastReceiver eqReadyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setupSeekBars();
            setupPresent();
        }
    };
    private void setupPresent(){
        if (isPresetSetup) {
            Log.d("MainActivity", "Spinner đã được khởi tạo, bỏ qua setupPresent");
            return;
        }
        prefs = getSharedPreferences("EQ_PREFS", MODE_PRIVATE);
        int numberOfPresets = prefs.getInt("numberOfPresets", 1);
        List<String> presetNamesRestored = new ArrayList<>();
        for(int i = 0; i < numberOfPresets; i++){
            presetNamesRestored.add(prefs.getString("preset" + i, ""));
            Log.d("preset", prefs.getString("preset" + i, ""));
        }
        // gắn adapter cho mỗi spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                presetNamesRestored
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnPreset.setAdapter(adapter);
        isPresetSetup = true;
        // Xử lý sự kiện chọn item (nếu cần)
        spnPreset.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @SuppressLint("UnspecifiedRegisterReceiverFlag")
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true;
                    return; // Bỏ qua lần gọi đầu tiên
                }
                // Lưu preset vào SharedPreferences
                SharedPreferences prefs = getSharedPreferences("EQ_PREFS", MODE_PRIVATE);
                prefs.edit().putInt("selectedPreset", position).apply();

                // Gửi Intent
                Intent intent = new Intent(MainActivity.this, EqualizerService.class);
                intent.setAction(EqualizerService.ACTION_USE_PRESET);
                intent.putExtra(EqualizerService.EXTRA_PRESET_POS, position);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }
    private void setupSeekBars() {
        for (short i = 0; i < numberOfBands; i++) {
            final short band = i;
            // Lấy level đã lưu trong SharedPreferences để set progress
            int savedProgress = prefs.getInt("band" + band, 50); // default 50%
            seekBars[band].setProgress(savedProgress);

            seekBars[band].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (!fromUser) return;

                    prefs.edit().putInt("band" + band, progress).apply();

                    int savedValue = prefs.getInt("band" + band, -1);
                    Log.d("BANDLOG", "onProgressChanged: " + savedValue);
                    // Gửi intent tới Service để set band level
                    int level = seekBar.getProgress(); // level có thể scale theo min/max nếu cần

                    Intent intent = new Intent(MainActivity.this, EqualizerService.class);
                    intent.setAction(EqualizerService.ACTION_SET_BAND);
                    intent.putExtra(EqualizerService.EXTRA_BAND, (int) band);
                    intent.putExtra(EqualizerService.EXTRA_LEVEL, level);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent);
                    } else {
                        startService(intent);
                    }
                }

                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }
    }
    @Override protected void onStop() { super.onStop(); unregisterReceiver(eqReadyReceiver); }
}
