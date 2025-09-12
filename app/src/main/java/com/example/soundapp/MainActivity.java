package com.example.soundapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.soundapp.Services.SoundService;

public class MainActivity extends AppCompatActivity {
    private Equalizer equalizer;
    private SeekBar[] seekBars;
    private TextView[] txtBands;
    private int numberOfBands = 5;
    SharedPreferences prefs;
    private static final int NOTIFICATION_PERMISSION_CODE = 1;  // Hoặc bất kỳ số nguyên dương nào, ví dụ 100
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Kiểm tra và yêu cầu quyền nếu cần
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }


        prefs = getApplicationContext()
                .getSharedPreferences("EQ_PREFS", MODE_PRIVATE);
        Intent intent = new Intent(this, SoundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent); // Android 8+ cần startForegroundService
        }
        seekBars = new SeekBar[numberOfBands];
        txtBands = new TextView[numberOfBands];
        for (int i = 0; i < numberOfBands; i++) {
            // giả sử id theo tên seekBarBand1, seekBarBand2,... và txtBand1, txtBand2,...
            int seekId = getResources().getIdentifier("seekBarBand" + (i + 1), "id", getPackageName());
            int txtId = getResources().getIdentifier("txtBand" + (i + 1), "id", getPackageName());

            seekBars[i] = findViewById(seekId);
            txtBands[i] = findViewById(txtId);
        }
        setupEqualizer();
    }
    /** Thiết lập Equalizer system-wide */
    private void setupEqualizer() {
        // audioSession = 0 → tác động toàn bộ audio session
        equalizer = new Equalizer(0, 0);
        equalizer.setEnabled(true);

        short minLevel = equalizer.getBandLevelRange()[0];
        short maxLevel = equalizer.getBandLevelRange()[1];
        short totalBands = equalizer.getNumberOfBands();

        int bandsToUse = Math.min(numberOfBands, totalBands);

        for (short i = 0; i < bandsToUse; i++) {
            final short band = i;
            int centerFreqHz = equalizer.getCenterFreq(band) / 1000;

            txtBands[i].setText(centerFreqHz + " Hz");

            seekBars[i].setMax(maxLevel - minLevel);
            int savedProgress = prefs.getInt("band" + band, (maxLevel - minLevel) / 2);
//            if (savedProgress < 0 || savedProgress > (maxLevel - minLevel)) {
//                savedProgress = (maxLevel - minLevel) / 2; // Đặt lại nếu giá trị không hợp lệ
//            }
            seekBars[i].setProgress(savedProgress);
            short restoredLevel = (short) (savedProgress + minLevel);
            equalizer.setBandLevel(band, restoredLevel);
            seekBars[i].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    short level = (short) (progress + minLevel);
                    if (!fromUser) return;
                    prefs.edit()
                            .putInt("band" + band, progress)
                            .apply();
                    try {
                        equalizer.setBandLevel(band, level);
                    } catch (UnsupportedOperationException e) {
                        Log.e("Equalizer", "Thiết bị không hỗ trợ setBandLevel", e);
                    }
                }

                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }
    }

}
