package gr.auth.ee.mug.datacollectionapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import gr.auth.ee.mug.datacollectionapp.MandoUI.PairMandometerDialogFragment;
import gr.auth.ee.mug.datacollectionapp.database.AppExecutors;
import gr.auth.ee.mug.datacollectionapp.database.Record;
import gr.auth.ee.mug.datacollectionapp.database.UploadLog;
import gr.auth.ee.mug.datacollectionapp.mandocapture.BluetoothController;

import static android.Manifest.permission.BLUETOOTH;
import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BROADCAST_STICKY;
import static android.Manifest.permission.MODIFY_AUDIO_SETTINGS;
import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.ContentValues.TAG;
import static android.os.Build.VERSION.SDK_INT;


public class MainActivity extends AppCompatActivity {
    AudioManager audioManager;
    BluetoothController bluetoothController;
    BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    Button recordButton, stopButton;
    FloatingActionButton uploadButton, signUpButton;
    public Button pairMandometerButton;
    public UploadLog uploadLog;
    int filesToBeUploaded, filesOnQueue;
    private AtomicLong ackTimestamp = new AtomicLong(0);
    private AtomicBoolean first_iteration = new AtomicBoolean(true);
    private AtomicBoolean recording = new AtomicBoolean(false);
    private AtomicBoolean watchCrashed = new AtomicBoolean(false);
    private AtomicBoolean watchStopped = new AtomicBoolean(false);
    private ScheduledExecutorService scheduledExecutorService;

    AtomicBoolean allSensorFilesReceived = new AtomicBoolean(true);
    Handler handler = new Handler();

    /**
     * Used for checking if the storage permission has been granted
     */
    Runnable checkSettingOn = new Runnable() {

        @Override
        public void run() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    //You have the permission, re-launch MainActivity
                    Intent i = new Intent(MainActivity.this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    return;
                }
                handler.postDelayed(this, 200);
            }
        }
    };

    /**
     * Handles return from storage permission request(non-deprecated way for onActivityResult)
     */
    ActivityResultLauncher<Intent> storagePermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (SDK_INT >= Build.VERSION_CODES.R) {
                    if (!Environment.isExternalStorageManager()) {
                        Toast.makeText(MainActivity.this, "Storage access permission is required!", Toast.LENGTH_SHORT)
                                .show();
                    }
                }
                requestPermissions();
            });

    /**
     * Handles return from permissions request(non-deprecated way for onActivityResult)
     */
    ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            isGranted -> {
                if (isGranted.containsValue(false)) {
                    Toast.makeText(
                            this,
                            "Permission/s is required for the app to function, please provide manually",
                            Toast.LENGTH_SHORT).show();
                }
            });

    /**
     * Handles return from bluetooth enabling request(non-deprecated way for onActivityResult)
     */
    ActivityResultLauncher<Intent> bluetoothEnableLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK) {recordButton.setEnabled(true);} else {
                        finishAffinity();
                        System.exit(0);
                    }
                }
            });


    @SuppressLint("VisibleForTests")
    private void orderSensorCapturing(String order) {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            String path = "/sensor_capturing";
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(path);
            putDataMapRequest.getDataMap().putString("order", order);
            PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();
            Task<DataItem> putDataTask = Wearable.getDataClient(this).putDataItem(putDataRequest);
            putDataTask.addOnSuccessListener(dataItem -> Log.i(TAG, "Sensor capturing order sent successfully.")).addOnFailureListener(e -> Log.i(TAG, "Sensor capturing order failed to send."));
        });
    }

    boolean upload_approved_already = false;
    String uploadMessage = null;

    private final BroadcastReceiver uploadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("upload_approved");
            if (message != null) {
                if (recording.get()) {
                    upload_approved_already = true;
                    uploadMessage = message;
                } else {
                    processUploadMessage(message);
                }
            }
        }
    };

    private void processUploadMessage(String message) {
        if (message.equals("true") && !recording.get()) {
            watchStatusNotification("new_files");
            allSensorFilesReceived.set(true);
            Toast.makeText(getApplicationContext(), "Ready to upload!", Toast.LENGTH_LONG).show();
            Log.d(TAG, "Upload approved: " + allSensorFilesReceived.get());
            uploadButton.setEnabled(true);
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(uploadReceiver);
        } else if (message.equals("false") && !recording.get()) {
            if (watchCrashed.get())
                watchStatusNotification("new_files_failed");
            allSensorFilesReceived.set(true);
            Toast.makeText(getApplicationContext(), "Upload ready despite files missing! Contact developers!", Toast.LENGTH_LONG).show();
            uploadButton.setEnabled(true);
            watchStatusNotification("files_failed");
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(uploadReceiver);
        }
    }

    private final BroadcastReceiver ackReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received watch ack");
            ackTimestamp.set(System.currentTimeMillis());
            if (unreachableWatchNotificationCounter == 4) {
                watchStatusNotification("watch_back");
            }
            watchCrashed.set(false);
            unreachableWatchNotificationCounter = 0;
            watch_charging.set(false);
            if(first_iteration.compareAndSet(true, false)) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Watch started capturing!", Toast.LENGTH_SHORT).show());                        Log.d(TAG, "Watch has started");
            }
        }
    };

    AtomicBoolean watch_charging = new AtomicBoolean(false);
    AtomicBoolean watch_critical_battery = new AtomicBoolean(false);

    private final BroadcastReceiver updatedBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received updates on watch's battery: " + intent.getStringExtra("battery_message"));
            String message = intent.getStringExtra("battery_message");
            if (message != null) {
                switch (message) {
                    case "low_battery":
                        watchStatusNotification("low_battery");
                        break;
                    case "critical_battery":
                        if (recording.get() && !watchStopped.get() && !mandoStarted.get()) {
                            watch_critical_battery.set(true);
                            stopWatchCapturing();
                            watchStatusNotification("critical_battery");
                            Log.d("updatedBatteryReceiver", "Watch has stopped due to critical battery");
                        }
                        break;
                    case "battery_charged":
                        watchStatusNotification("battery_charged");
                        break;
                    case "battery_charging":
                        if(!watch_charging.get()) {
                            watchStatusNotification("battery_charging");
                            watch_charging.set(true);
                            watch_critical_battery.set(false);
                            if (watchStopped.get())
                                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Watch is charging!", Toast.LENGTH_LONG).show());
                            else {
                                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Watch is charging! Stopping watch sensor capturing...", Toast.LENGTH_LONG).show());
                                stopWatchCapturing();
                            }
                        }
                        break;
                    case "battery_discharging_low":
                        Log.d("updatedBatteryReceiver", "Watch has stopped due to critical battery");
                        watchStatusNotification("wait_to_charge");
                        runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Watch cannot resume capturing yet, please charge more!", Toast.LENGTH_LONG).show());
                        break;
                    case "battery_discharging_okay":
                        if(watch_charging.get()) {
                            Log.d("updatedBatteryReceiver", "Watch has restarted capturing due to battery_discharging");
                            watch_critical_battery.set(false);
                            watch_charging.set(false);
                            watchStatusNotification("battery_discharging");
                            startWatchCapturing();
                            runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Watch stopped charging! Resuming capturing", Toast.LENGTH_LONG).show());
                        }
                        break;
                }
            } else {
                Log.d(TAG, "Received battery update with no message");
            }
        }
    };

    int unreachableWatchNotificationCounter = 0;
    long firstAckTimestamp = 0;
    private Runnable createWatchRunningAckRunnable() {
        return () -> {
            unreachableWatchNotificationCounter = 0;
            while(recording.get() && !watchStopped.get()) {
                if(!first_iteration.get() && !watchCrashed.get() && !watch_charging.get()) {
                    if (System.currentTimeMillis() - ackTimestamp.get() > 90000) {
                        orderSensorCapturing("stop");
                        try {
                            Thread.sleep(20000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        orderSensorCapturing("start");
                        try {
                            Thread.sleep(20000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (System.currentTimeMillis() - ackTimestamp.get() > 90000){
                        runOnUiThread(() -> {
                                Toast.makeText(getApplicationContext(), "Watch has stopped capturing!", Toast.LENGTH_LONG).show();
                                watchStatusNotification("stopped");
                                unreachableWatchNotificationCounter++;
                            });
                            Log.d(TAG, "Watch has stopped unexpectedly");
                        }
                    }
                } else if (first_iteration.get()) {
                    if(System.currentTimeMillis() - firstAckTimestamp > 10000) {
                        orderSensorCapturing("start");
                        if (System.currentTimeMillis() - firstAckTimestamp > 10000) {
                            runOnUiThread(() -> {
                                Toast.makeText(getApplicationContext(), "Watch has not started capturing! Restart capturing or charge it!", Toast.LENGTH_LONG).show();
                                watchStatusNotification("not_started");
                            });
                            Log.d(TAG, "Watch has not started capturing!");
                        }
                    }
                }
                try {
                    Thread.sleep(90000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (unreachableWatchNotificationCounter == 3) {
                    watchCrashed.set(true);
                    watchStatusNotification("unreachable");
                    unreachableWatchNotificationCounter = 4;
                    //create an executable single thread and execute it
                    Executors.newSingleThreadExecutor().execute(retryStartingWatch());
                }
            }
        };
    }

    private Runnable retryStartingWatch() {
        return () -> {
            while(recording.get() && watchCrashed.get()) {
                //send stop, wait 20 seconds, send start and wait 5 minutes
                orderSensorCapturing("stop");
                try {
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                orderSensorCapturing("start");
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private void watchStatusNotification(String issue) {
        String channelId = "watch_status_channel";
        Uri alarmSoundUri = Settings.System.DEFAULT_NOTIFICATION_URI;
        Uri veryLowBatterySoundUri = Settings.System.DEFAULT_ALARM_ALERT_URI;

        boolean important = issue.equals("stopped") || issue.equals("low_battery") || issue.equals("critical_battery") || issue.equals("unreachable");
        boolean batteryRelated = issue.equals("low_battery") || issue.equals("critical_battery"); //to have them have the same notification ID

        Map<String, String> issueChannelNameMap = new HashMap<>();
        //file related notifications
        issueChannelNameMap.put("new_files", "Sensor files from previous session have arrived, ready to upload!");
        issueChannelNameMap.put("new_files_failed", "Error while retrieving sensor files from previous session, contact developers!");
        issueChannelNameMap.put("files_failed", "Some sensor files failed to arrive, contact developers!");

        //battery & status related notifications
        issueChannelNameMap.put("stopped", "Watch has stopped capturing or is out of range! Ignore if it's charging!");
        issueChannelNameMap.put("low_battery", "Watch battery is low!");
        issueChannelNameMap.put("battery_charging", "Watch is charging! It will resume capturing when when it is sufficiently charged and unplugged!");
        issueChannelNameMap.put("battery_discharging", "Charging has stopped. Resuming capturing!");
        issueChannelNameMap.put("unreachable", "Watch is unreachable! If it has battery and isn't charging, stop capturing and restart both applications.");
        issueChannelNameMap.put("critical_battery", "Watch has stopped capturing, due to critical battery, please recharge!");
        issueChannelNameMap.put("wait_to_charge", "Watch cannot restart capturing yet, please wait for it to charge!");
        issueChannelNameMap.put("not_started", "Watch has not started capturing, please restart capturing or charge it!");

        issueChannelNameMap.put("watch_back", "Watch is back in range!");

        String channelName = issueChannelNameMap.getOrDefault(issue, "Watch Status Notifications");
        int notificationId = (issue.equals("unreachable") || issue.equals("watch_back")) ? 2000 : (batteryRelated ? 1000 : issue.hashCode());

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        int importance = important ? NotificationManager.IMPORTANCE_HIGH : NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
        channel.enableVibration(true);
        channel.setVibrationPattern(!important ? new long[]{100, 200, 100, 200} : new long[]{100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100});

        if (important) {
            channel.setLightColor(Color.RED);
            channel.enableLights(true);
            if (issue.equals("critical_battery"))
                channel.setSound(veryLowBatterySoundUri, new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
            else
                channel.setSound(alarmSoundUri, new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
        }
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationManager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Watch Alert")
                .setContentText(channelName)
                .setContentIntent(createLaunchAppPendingIntent())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setOngoing(important); //if its important, its ongoing, else not
        notificationManager.notify(notificationId, builder.build());
    }

    private PendingIntent createLaunchAppPendingIntent() {
        Intent launchAppIntent = new Intent(this, MainActivity.class);
        launchAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(this, 0, launchAppIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }


    private void startWatchRunningCheckerThread(){
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleWithFixedDelay(createWatchRunningAckRunnable(), 0, 90, TimeUnit.SECONDS);
    }

    private void stopWatchRunningCheckerThread(){
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
        }
    }
    AtomicBoolean mandoStarted = new AtomicBoolean(false);
    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        filesOnQueue = 0;
        uploadLog = UploadLog.getInstance(getApplicationContext());
        refreshDB();

        //Binding the UI buttons
        recordButton = findViewById(R.id.recordButton);
        stopButton = findViewById(R.id.stopButton);
        pairMandometerButton = findViewById(R.id.pairScaleButton);
        signUpButton = findViewById(R.id.signUpWindowBtn);
        uploadButton = findViewById(R.id.uploadBtn);
        recordButton.setEnabled(false);
        stopButton.setEnabled(false);
        pairMandometerButton.setEnabled(false);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.hide();

        //Declaring and binding the UI buttons
        Button recordButton = findViewById(R.id.recordButton);
        Button stopButton = findViewById(R.id.stopButton);
        stopButton.setEnabled(false);

        /* Special handling is required for storage in Android 11.
           So the storage permission is requested first and then
           the others are requested by calling the requestPermissions
           in request storage permission */
        if(SDK_INT >= Build.VERSION_CODES.R)
            requestStoragePermission();
        else
            requestPermissions();

        if (!btAdapter.isEnabled()) { //Create dialog to turn on Bluetooth if not already on
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("Bluetooth is required for the app to function.")
                    .setCancelable(false)
                    .setPositiveButton("Take me", (dialog, which) -> {
                        bluetoothEnableLauncher.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                        dialog.dismiss();
                    })
                    .setNegativeButton("Exit", (dialog, which) -> {
                        finishAffinity();
                        System.exit(0);
                    });

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        } else {recordButton.setEnabled(true);}

        orderSensorCapturing("stop");

        recordButton.setOnClickListener(view -> {
            if (allPermissionsGranted()) {
                if(btAdapter.isEnabled()) {
                    //Informing the user that recording has started
                    Toast.makeText(getApplicationContext(), "Recording started.", Toast.LENGTH_SHORT).show();
                    uploadButton.setEnabled(false);
                    stopButton.setEnabled(true);//Enabling stop button because recording has started
                    recordButton.setEnabled(false); //Disabling recording button because recording has started
                    pairMandometerButton.setEnabled(true); /*Ability to add mandometer only available after
                audio is already recording*/

                    Intent intent = new Intent(getApplicationContext(), CaptureService.class);
                    startForegroundService(intent);

                    allSensorFilesReceived.set(false);
                    recording.set(true);
                    startWatchCapturing();
                    //register receiver for low battery
                    LocalBroadcastManager.getInstance(this).registerReceiver(updatedBatteryReceiver, new IntentFilter("updated-battery-broadcast"));
                } else {
                    Toast.makeText(
                            getApplicationContext(),
                            "Bluetooth is not enabled. Please turn on and try again.",
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(
                        getApplicationContext(),
                        "Missing one or more permissions. Please provide manually and try again.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        stopButton.setOnClickListener(view -> {
            Toast.makeText(getApplicationContext(), "Recording stopped.", Toast.LENGTH_SHORT).show();
            recordButton.setEnabled(true);
            stopButton.setEnabled(false);
            pairMandometerButton.setEnabled(false);
            Intent intent = new Intent(getApplicationContext(), CaptureService.class);
            stopService(intent);
            stopWatchCapturing();
            LocalBroadcastManager.getInstance(this).unregisterReceiver(updatedBatteryReceiver);
            recording.set(false);
            refreshDB();
            enableUploadButton();
        });

        pairMandometerButton.setOnClickListener(view -> {
            if (allPermissionsGranted() && btAdapter.isEnabled()) {
                bluetoothController = new BluetoothController();
                pairMandometerButton.setEnabled(false);
                DialogFragment newFragment = new PairMandometerDialogFragment(this);
                newFragment.setCancelable(false);
                newFragment.show(getSupportFragmentManager(), "Pairing");
                mandoStarted.set(true);
            } else {
                Toast.makeText(
                        getApplicationContext(),
                        "Missing one or more permissions. Please try again.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        signUpButton.setOnClickListener(view -> {
            if (allPermissionsGranted()) {
                if (hasNetworkConnection()) {
                    DialogFragment frag = new SignUpDialogFragment();
                    frag.show(getSupportFragmentManager(), "Sign up");
                }
            } else {
                Toast.makeText(
                        getApplicationContext(),
                        "Missing one or more permissions. Please try again.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        uploadButton.setOnClickListener(view -> {
            if (allPermissionsGranted()) {
                if (filesToBeUploaded > 0) {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    if (preferences.contains("JWT") && preferences.contains("Username")) {
                        if (hasNetworkConnection()) {
                            if (allSensorFilesReceived.get()) {
                                Toast.makeText(this, "Uploading files...", Toast.LENGTH_SHORT).show();
                                uploadButton.setEnabled(false);
                                uploadFiles(getApplicationContext());
                            } else {
                                Toast.makeText(this, "Please wait for all files to be received.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {Toast.makeText(getApplicationContext(), "No signed-in user!", Toast.LENGTH_SHORT).show();}
                } else {Toast.makeText(getApplicationContext(), "No files pending upload.", Toast.LENGTH_SHORT).show();}
            } else {
                Toast.makeText(
                        getApplicationContext(),
                        "Missing one or more permissions. Please try again.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        registerReceiver(mBluetoothScoReceiver, intentFilter);
        audioManager = (AudioManager) getApplicationContext().getSystemService(getApplicationContext().AUDIO_SERVICE);
        // Start Bluetooth SCO.
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setBluetoothScoOn(true);
        audioManager.startBluetoothSco();
        // Stop Speaker.
        audioManager.setSpeakerphoneOn(false);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBluetoothScoReceiver);
        unregisterReceiver(uploadReceiver);
        unregisterReceiver(ackReceiver);
        // Stop Bluetooth SCO.
        audioManager.stopBluetoothSco();
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setBluetoothScoOn(false);
        // Start Speaker.
        audioManager.setSpeakerphoneOn(true);

        orderSensorCapturing("stop");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updatedBatteryReceiver);
        unregisterReceiver(ackReceiver);
        unregisterReceiver(uploadReceiver);
        stopWatchRunningCheckerThread();
    }

    private void startWatchCapturing() {
        upload_approved_already = false;
        firstAckTimestamp = System.currentTimeMillis();
        first_iteration.set(true);
        LocalBroadcastManager.getInstance(this).registerReceiver(ackReceiver, new IntentFilter("running-broadcast"));
        LocalBroadcastManager.getInstance(this).registerReceiver(uploadReceiver, new IntentFilter("upload-approved-broadcast"));
        orderSensorCapturing("start");
        watchStopped.set(false);
        startWatchRunningCheckerThread();
    }

    private void stopWatchCapturing() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(ackReceiver);
        orderSensorCapturing("stop");
        watchStopped.set(true);
        stopWatchRunningCheckerThread();
    }

    private void enableUploadButton() {
        if (upload_approved_already) {
            processUploadMessage(uploadMessage);
        } else if (unreachableWatchNotificationCounter > 0) {
            unreachableWatchNotificationCounter = 0;
            processUploadMessage("failed");
        } else {
            Toast.makeText(this, "Transferring files, it might take a few minutes.", Toast.LENGTH_LONG).show();
            Executor executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                if(ackTimestamp.get() == 0) {
                    runOnUiThread(() -> processUploadMessage("false"));
                }
                try {
                    Thread.sleep(180000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!allSensorFilesReceived.get()) {
                    runOnUiThread(() -> processUploadMessage("false"));
                }
            });
        }
    }

    /**
     * Requests permissions that need to be provided by the user(runtime permissions), except the storage permission
     */
    public void requestPermissions() {
        if(SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            requestPermissionLauncher.launch(new String[]{RECORD_AUDIO, BLUETOOTH_CONNECT, POST_NOTIFICATIONS});
        else if (SDK_INT >= Build.VERSION_CODES.S)
            requestPermissionLauncher.launch(new String[]{RECORD_AUDIO, BLUETOOTH_CONNECT});
        else
            requestPermissionLauncher.launch(new String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO});
    }

    /**
     * Requests storage runtime permission, which requires special handling after Android 11
     */
    @RequiresApi(Build.VERSION_CODES.R)
    public void requestStoragePermission() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent storageIntentTry = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    storageIntentTry.addCategory("android.intent.category.DEFAULT");
                    storageIntentTry.setData(Uri.parse(String.format("package:%s",
                            getApplicationContext().getPackageName())));
                    storagePermissionLauncher.launch(storageIntentTry);
                    handler.postDelayed(checkSettingOn, 1000);
                } catch (Exception e) {
                    Intent storageIntentCatch = new Intent();
                    storageIntentCatch.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    storagePermissionLauncher.launch(storageIntentCatch);
                    handler.postDelayed(checkSettingOn, 1000);
                }
            }
        }
    }

    /**
     * Checks that all permissions required for functioning have been granted prior to recording start
     *
     * @return false if one is missing, true otherwise
     */
    public boolean allPermissionsGranted() {
        boolean storage, bluetooth, notifications;
        if (SDK_INT >= Build.VERSION_CODES.R) //special condition for storage after android 11
            storage = Environment.isExternalStorageManager();
        else
            storage = ContextCompat.checkSelfPermission(MainActivity.this, READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(MainActivity.this, WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;

        if (SDK_INT >= Build.VERSION_CODES.S)
            bluetooth = ContextCompat.checkSelfPermission(MainActivity.this, BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
        else
            bluetooth = ContextCompat.checkSelfPermission(MainActivity.this, BLUETOOTH)
                    == PackageManager.PERMISSION_GRANTED;

        if(SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            notifications = ContextCompat.checkSelfPermission(MainActivity.this, POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        else
            notifications = true;

        return ContextCompat.checkSelfPermission(MainActivity.this.getApplicationContext(), RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(MainActivity.this.getApplicationContext(), MODIFY_AUDIO_SETTINGS)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(MainActivity.this.getApplicationContext(), BROADCAST_STICKY)
                == PackageManager.PERMISSION_GRANTED
                && bluetooth
                && storage
                && notifications;
    }

    /**
     * Checks if device has network connection enabled(mobile data or wifi)
     *
     * @return True if a connection is established, false otherwise
     */
    public boolean hasNetworkConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
        if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
            return true;
        } else {
            Toast.makeText(
                    getApplicationContext(),
                    "Please establish network connection and try again",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * Scans for the files of whom there are records in the database and updates the database if they are deleted.
     * Also updates the filesToBeUploaded variable by checking their upload status(uploaded variable)
     */
    public void refreshDB() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<Record> allRecords = uploadLog.uploadDao().getAll();
            uploadLog.uploadDao().deleteRecords(allRecords);
            File temp;
            for (int i = 0; i < allRecords.size(); i++) {
                temp = new File(allRecords.get(i).getFullFileName());
                if (!temp.exists()) {allRecords.remove(allRecords.get(i));}
            }
            for (int i = 0; i < allRecords.size(); i++) {
                if (!allRecords.get(i).isUploaded()) {filesToBeUploaded++;}
                allRecords.get(i).setId(i + 1);
            }
            uploadLog.uploadDao().addMultipleRecords(allRecords);
        });
    }

    /**
     * Uploads files that haven't already been uploaded to the database
     *
     * @param context Application context
     */
    public void uploadFiles(Context context) {
        recordButton.setEnabled(false);
        filesOnQueue = filesToBeUploaded;
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<Record> allRecords = uploadLog.uploadDao().getAll();
            for (int i = 0; i < allRecords.size(); i++) {
                if (!allRecords.get(i).isUploaded()) {
                    int j = i;
                    Futures.addCallback(Server.upload(
                            context,
                            allRecords.get(i).getFullFileName(),
                            allRecords.get(i).getFileType()), new FutureCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            filesToBeUploaded--;
                            Record temp = allRecords.get(j);
                            temp.setUploaded(true);
                            AppExecutors.getInstance().diskIO().execute(() -> uploadLog.uploadDao().updateRecord(temp));
                            isUploadingDone();
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            Toast.makeText(
                                    getApplicationContext(),
                                    "A file has failed to upload. Please retry once uploading is completed",
                                    Toast.LENGTH_SHORT).show();
                            isUploadingDone();
                        }
                    }, getApplicationContext().getMainExecutor());
                }
            }
        });
    }

    /**
     * Called when uploading is finished to inform the user and re-enable uploading button
     */
    public void isUploadingDone() {
        filesOnQueue--;
        if (filesOnQueue == 0) {
            Toast.makeText(getApplicationContext(), "Uploading complete", Toast.LENGTH_SHORT).show();
            uploadButton.setEnabled(true);
            recordButton.setEnabled(true);
        }
    }

    /**
     * Establishes connection with the bluetooth headset
     */
    private final BroadcastReceiver mBluetoothScoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
            System.out.println("ANDROID Audio SCO state: " + state);
            if (AudioManager.SCO_AUDIO_STATE_CONNECTED == state) {
                /*
                 * Now the connection has been established to the bluetooth device.
                 * Record audio or whatever (on another thread).With AudioRecord you can record with an object
                 * created like this:
                 * new AudioRecord(MediaRecorder.AudioSource.MIC, 8000, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                 * AudioFormat.ENCODING_PCM_16BIT, audioBufferSize);
                 *
                 * After finishing, don't forget to unregister this receiver and
                 * to stop the bluetooth connection with am.stopBluetoothSco();
                 */
            }
        }
    };
}

