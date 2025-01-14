package gr.auth.ee.mug.datacollectionapp.wear;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import gr.auth.ee.mug.datacollectionapp.sensorcapture.FileSenderWorker;

public class LowBatteryReceiver extends BroadcastReceiver {

    private boolean isLowBatteryNotified = false;
    private float lastBatteryPercentage = 100;
    private float lastBatteryPercentage_monitor = -1;
    private static final String UPDATED_BATTERY_PATH = "/updated_battery";
    private static final String UPDATED_BATTERY_KEY = "updated_battery";
    private int lowBatteryLevel = 20;
    private int criticalBatteryLevel = 10; //at around 5 it crashes depending on workload
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private static final String TAG = "LowBatteryReceiver";
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        int batteryPercentage = getBatteryPercentage(intent);
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL;
        SharedPreferences sharedPreferences = context.getSharedPreferences("BatteryPrefs", Context.MODE_PRIVATE);
        boolean lastChargingState = sharedPreferences.getBoolean("lastChargingState", false);
        if ((batteryPercentage != lastBatteryPercentage_monitor && batteryPercentage <= 20) || isCharging != lastChargingState) {
            executorService.execute(() -> handleAction(intent, batteryPercentage, isCharging, lastChargingState, context));
            lastBatteryPercentage_monitor = batteryPercentage;
            sharedPreferences.edit().putBoolean("lastChargingState", isCharging).apply();
        }
    }

    private void handleAction(Intent intent, int batteryPercentage, boolean isCharging, boolean lastChargingState, Context context) {
        SharedPreferences serviceSharedPreferences = context.getSharedPreferences("ServicePrefs", Context.MODE_PRIVATE);
        switch (intent.getAction()) {
            case Intent.ACTION_BATTERY_CHANGED:
                if (isCharging != lastChargingState) {
                    handleChargingStateChanged(isCharging, batteryPercentage, context);
                } else {
                    handleBatteryChanged(batteryPercentage, serviceSharedPreferences, context);
                }
                break;
            case Intent.ACTION_BOOT_COMPLETED:
                handleBootCompleted(context);
                break;
        }
    }

    private int getBatteryPercentage(Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        return (int)((level * 100) / (float) scale);
    }
    private void handleBatteryChanged(int batteryPercentage, SharedPreferences serviceSharedPreferences, Context context) {

        boolean isServiceStarted = serviceSharedPreferences.getBoolean("isServiceStarted", false); //default value is false

        //notifies user dropping to low
        if (batteryPercentage <= lowBatteryLevel && batteryPercentage > criticalBatteryLevel && (lastBatteryPercentage > lowBatteryLevel || isServiceStarted) ) {
            rescheduleFileWorker(15, TimeUnit.MINUTES, context);
            Log.d(TAG, "handleBatteryChanged: Low battery, rescheduling worker to 15 minutes.");
            sendMessage(UPDATED_BATTERY_PATH, UPDATED_BATTERY_KEY, "low_battery", context);
            if (isServiceStarted) {
                serviceSharedPreferences.edit().putBoolean("isServiceStarted", false).apply();
            }
        }

        //notifies user dropping to critical
        if (batteryPercentage <= criticalBatteryLevel && (lastBatteryPercentage > criticalBatteryLevel || isServiceStarted) ) {
            Log.d(TAG, "handleBatteryChanged: Critical battery, stopping app.");
            sendMessage(UPDATED_BATTERY_PATH, UPDATED_BATTERY_KEY, "critical_battery", context);
            if (isServiceStarted) {
                serviceSharedPreferences.edit().putBoolean("isServiceStarted", false).apply();
            }
        }
        lastBatteryPercentage = batteryPercentage;
    }
    private void handleBootCompleted(Context context) {
        sendStatusUpdateUI("Stopped", context);
    }
    private void handleChargingStateChanged(Boolean isCharging, int batteryPercentage, Context context) {
        if (isCharging) {
            sendMessage(UPDATED_BATTERY_PATH, UPDATED_BATTERY_KEY, "battery_charging", context);
        } else {
            if(batteryPercentage >= lowBatteryLevel) {
                Log.d(TAG, "handlePowerDisconnected: battery level is restored");
                sendMessage(UPDATED_BATTERY_PATH, UPDATED_BATTERY_KEY, "battery_discharging_okay", context);
                if (isLowBatteryNotified) {
                    isLowBatteryNotified = false;
                }
            } else {
                Log.d(TAG, "handlePowerDisconnected: battery level still low");
                sendMessage(UPDATED_BATTERY_PATH, UPDATED_BATTERY_KEY, "battery_discharging_low", context);
            }
        }
    }
    @SuppressLint("VisibleForTests")
    private void sendMessage(String path, String key, String value, Context context) {
        DataClient dataClient = Wearable.getDataClient(context);

        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(path);
        putDataMapRequest.getDataMap().putString(key, value);

        putDataMapRequest.getDataMap().putLong("timestamp", System.currentTimeMillis());

        PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();

        Task<DataItem> putDataTask = dataClient.putDataItem(putDataRequest);
        putDataTask.addOnSuccessListener(aVoid -> Log.d(TAG, value + " message sent successfully"))
                .addOnFailureListener(e -> Log.d(TAG, value + " message failed to send"));
    }
    public void rescheduleFileWorker(long period, TimeUnit timeUnit, Context context) {
        Log.d(TAG, "rescheduleFileWorker: rescheduling file worker!");

        //cancel all workers named fileSenderWork
        WorkManager.getInstance(context).cancelAllWorkByTag("fileSenderWork");

        // Trigger a one-time work request immediately
        OneTimeWorkRequest immediateWorkRequest =
                new OneTimeWorkRequest.Builder(FileSenderWorker.class)
                        .build();
        WorkManager.getInstance(context).enqueue(immediateWorkRequest);

        // Schedule the periodic work request after #period minutes
        PeriodicWorkRequest fileSenderWorkRequest =
                new PeriodicWorkRequest.Builder(FileSenderWorker.class, period, timeUnit)
                        .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("fileSenderWork", ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, fileSenderWorkRequest);
    }

    private void sendStatusUpdateUI(String status, Context context) {
        SharedPreferences UIsharedPreferences = context.getSharedPreferences("SensorCapturePrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = UIsharedPreferences.edit();
        editor.putString("status", status);
        editor.apply();

        Intent intent = new Intent("sensor-capture-status-update");
        intent.putExtra("status", status);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}