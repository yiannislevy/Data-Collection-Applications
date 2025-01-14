package gr.auth.ee.mug.datacollectionapp.wear;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import gr.auth.ee.mug.datacollectionapp.wear.databinding.ActivityMainBinding;

public class MainActivity extends Activity{

    private LowBatteryReceiver lowBatteryReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //    private TextView mTextView;
        gr.auth.ee.mug.datacollectionapp.wear.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Log.d("MainActivity", "onCreate");
        updateStatusFromPrefs();
        LocalBroadcastManager.getInstance(this).registerReceiver(statusUpdateReceiver, new IntentFilter("sensor-capture-status-update"));
        // comment/uncomment the following lines to enable low battery notifications & functionality
        lowBatteryReceiver = new LowBatteryReceiver();
        registerReceiver(lowBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatusFromPrefs();
        LocalBroadcastManager.getInstance(this).registerReceiver(statusUpdateReceiver, new IntentFilter("sensor-capture-status-update"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(statusUpdateReceiver);
        unregisterReceiver(lowBatteryReceiver);
    }

    private void updateStatusFromPrefs() {
        SharedPreferences sharedPreferences = getSharedPreferences("SensorCapturePrefs", Context.MODE_PRIVATE);
        String status = sharedPreferences.getString("status", null);
        if (status != null) {
            TextView statusTextView = findViewById(R.id.statusTextView);
            statusTextView.setText(status);
        }
    }

    private BroadcastReceiver statusUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = intent.getStringExtra("status");
            if (status != null) {
                TextView statusTextView = findViewById(R.id.statusTextView);
                statusTextView.setText(status);
            }
        }
    };
}



