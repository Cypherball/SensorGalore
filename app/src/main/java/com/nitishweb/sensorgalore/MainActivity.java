package com.nitishweb.sensorgalore;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.material.snackbar.Snackbar;
import com.nitishweb.sensorgalore.databinding.ActivityMainBinding;

import static java.lang.Math.abs;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private ActivityMainBinding binding;
    private SensorManager mSensorManager;
    private Sensor mSensorProximity;
    private Sensor mSensorLight;
    private Sensor accelerometer;
    private long lastUpdate;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initViews();
    }

    private void initViews() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mSensorProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mSensorLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        resetColors();

        setListeners();
    }

    private void setListeners() {
        binding.bResetColor.setOnClickListener(v -> {
            resetColors();
        });
    }

    private void resetColors() {
        ColorDrawable bg = (ColorDrawable) binding.getRoot().getBackground();
        int alpha = bg.getAlpha();
        binding.getRoot().setBackgroundColor(Color.argb(alpha, 0, 0, 0));
        updateColorString();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();
        switch (sensorType) {
            // Event came from the light sensor.
            case Sensor.TYPE_LINEAR_ACCELERATION:
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                binding.accTV.setText(getResources().getString(R.string.accString, x, y, z));

                // Update Background Color every 15ms
                long curTime = System.currentTimeMillis();
                if ((curTime - lastUpdate) > 15) {
                    lastUpdate = curTime;
                    updateBackgroundRGB(x, y, z);
                }
                break;
            case Sensor.TYPE_LIGHT:
                float currentLightVal = event.values[0];
                binding.lightTV.setText(getResources().getString(R.string.lightString, currentLightVal));
                updateBackgroundAlpha(currentLightVal);
                break;
            case Sensor.TYPE_PROXIMITY:
                float currentProximityVal = event.values[0];
                binding.proximityTV.setText(getResources().getString(R.string.proximityString, currentProximityVal));
                break;
            default:
                // do nothing
        }
    }

    private void updateBackgroundAlpha(float currentLightVal) {
        //Linear Transformation (Mapping) from 0-100 to 0-255
        if (currentLightVal > 100) currentLightVal = 100;
        int alpha = (int) ((currentLightVal - 0) * (255 - 0)/(100 - 0) + 0);
        // Get current bg color
        ColorDrawable bg = (ColorDrawable) binding.getRoot().getBackground();
        int bgColor = bg.getColor();
        binding.getRoot().setBackgroundColor(Color.argb(alpha, Color.red(bgColor),Color.green(bgColor),Color.blue(bgColor)));

        updateColorString();
    }

    private void updateBackgroundRGB(float x, float y, float z) {
        //Linear Transformation (Mapping) from -1.5f - 1.5f to -15.0f - 15.0f
        if (x > 1.5f) x = 1.5f;
        if (y > 1.5f) y = 1.5f;
        if (z > 1.5f) z = 1.5f;

        if (x < -1.5f) x = -1.5f;
        if (y < -1.5f) y = -1.5f;
        if (z < -1.5f) z = -1.5f;

        // Ignore micro movements
        if (abs(x) < 0.5 && abs(y) < 0.5 && abs(z) < 0.5)
            return;

        int r = 0; int g = 0; int b = 0;
        // Only update the where the axis has had the maximum displacement
        if (abs(x) > abs(y) && abs(x) > abs(z))
            r = (int) ((x + 1.5f) * (15f + 15f)/(1.5f + 1.5f) - 15f);
        else if (abs(y) > abs(x) && abs(y) > abs(z))
            g = (int) ((y + 1.5f) * (15f + 15f)/(1.5f + 1.5f) - 15f);
        else if (abs(z) > abs(x) && abs(z) > abs(y))
            b = (int) ((z + 1.5f) * (15f + 15f)/(1.5f + 1.5f) - 15f);

        Log.d(TAG, "updateBackgroundRGB: R:" + r + " G:" + g + " B:" + b);

        // Get current background color and opacity
        ColorDrawable bg = (ColorDrawable) binding.getRoot().getBackground();
        int alpha = bg.getAlpha();
        int bgColor = bg.getColor();

        // Add new values to existing color and ensure it is in range of 0 to 255
        int newR = Color.red(bgColor) + r;
        if (newR > 255) newR = 255;
        if (newR < 0) newR = 0;

        int newG = Color.green(bgColor) + g;
        if (newG > 255) newG = 255;
        if (newG < 0) newG = 0;

        int newB = Color.blue(bgColor) + b;
        if (newB > 255) newB = 255;
        if (newB < 0) newB = 0;

        binding.getRoot().setBackgroundColor(Color.argb(alpha, newR, newG, newB));
        updateColorString();
    }

    // Set current ARGB Color values to textview
    private void updateColorString() {
        ColorDrawable bg = (ColorDrawable) binding.getRoot().getBackground();
        int a = bg.getAlpha();
        int bgColor = bg.getColor();
        int r = Color.red(bgColor);
        int g = Color.green(bgColor);
        int b = Color.blue(bgColor);
        binding.currentBGTV.setText(getResources().getString(R.string.currentColorString, a, r, g, b));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    protected void onStop() {
        super.onStop();
        mSensorManager.unregisterListener(this);
    }

    protected void onStart() {
        super.onStart();

        if (accelerometer!= null)
            mSensorManager.registerListener(this, accelerometer , SensorManager.SENSOR_DELAY_FASTEST);
        else
            Snackbar.make(binding.getRoot(), "Error: Accelerometer Sensor not found!", Snackbar.LENGTH_SHORT).show();

        if (mSensorProximity != null)
            mSensorManager.registerListener(this, mSensorProximity, SensorManager.SENSOR_DELAY_NORMAL);
        else
            Snackbar.make(binding.getRoot(), "Error: Proximity Sensor not found!", Snackbar.LENGTH_SHORT).show();

        if (mSensorLight != null)
            mSensorManager.registerListener(this, mSensorLight, SensorManager.SENSOR_DELAY_FASTEST);
        else
            Snackbar.make(binding.getRoot(), "Error: Light Sensor not found!", Snackbar.LENGTH_SHORT).show();
    }
}