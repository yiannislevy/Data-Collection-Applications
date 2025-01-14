package gr.auth.ee.mug.datacollectionapp.wear;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import gr.auth.ee.mug.datacollectionapp.sensorcapture.FileSenderWorker;
import gr.auth.ee.mug.datacollectionapp.sensorcapture.SensorCaptureManager;

public class SensorCaptureService extends Service {

    private SensorCaptureManager _sensorManager;
    private Boolean isRunning = false;
    private ExecutorService executorService;
    private SharedPreferences.Editor editor;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this._sensorManager = SensorCaptureManager.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();
        isRunning = false;
        SharedPreferences sharedPreferences = getSharedPreferences("ServicePrefs", Context.MODE_PRIVATE);
        this.editor = sharedPreferences.edit();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isRunning) {
            executorService.execute(() -> {
                try {
                    _sensorManager.start();
                    startFileWorker();
                    vibrateDevice("start");
                } catch (Exception e) {
                    Log.d("SensorCaptureService", "onStartCommand: " + e.getMessage());
                }
            });
            createNotificationChannel();
            Notification notification = getNotification();
            isRunning = true;
            startForeground(1, notification);
            editor.putBoolean("isServiceStarted", isRunning).apply();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (isRunning) {
            _sensorManager.stop();
            stopFileWorker();
            vibrateDevice("stop");
            isRunning = false;
            editor.putBoolean("isServiceStarted", isRunning).apply();
        }
        executorService.shutdown();
        super.onDestroy();
    }


    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                "SENSOR_CAPTURE_CHANNEL",
                "Sensor Capture Service Channel",
                NotificationManager.IMPORTANCE_LOW
        );

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification getNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "SENSOR_CAPTURE_CHANNEL")
                .setContentTitle("Sensor Capture Service")
                .setContentText("The sensor capture service is running.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        return builder.build();
    }

    public void startFileWorker(){
        //first cancel all previous workers  and then start the new
        WorkManager.getInstance(this).cancelUniqueWork("fileSenderWork");

        PeriodicWorkRequest fileSenderWorkRequest =
                new PeriodicWorkRequest.Builder(FileSenderWorker.class, 30, TimeUnit.MINUTES)
                        .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("fileSenderWork", ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, fileSenderWorkRequest);
    }

    public void stopFileWorker() {
        // Enqueue a one-time worker to send remaining files, if any
        OneTimeWorkRequest sendRemainingFilesWorkRequest =
                new OneTimeWorkRequest.Builder(FileSenderWorker.class)
                        .build();
        WorkManager.getInstance(this).enqueue(sendRemainingFilesWorkRequest);

        // Cancel the unique periodic work
        WorkManager.getInstance(this).cancelUniqueWork("fileSenderWork");

    }

    private void vibrateDevice(String type) {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        if (vibrator.hasVibrator()) {
            VibrationEffect vibrationEffect;

            if (type.equals("start")) {
                // Vibration pattern for 'start': wait 0ms, vibrate 500ms
                vibrationEffect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE);
            } else { // 'stop'
                // Vibration pattern for 'stop': wait 0ms, vibrate 200ms, wait 200ms, vibrate 200ms
                long[] pattern = {0, 200, 100, 200, 100, 200};
                vibrationEffect = VibrationEffect.createWaveform(pattern, -1);
            }
            vibrator.vibrate(vibrationEffect);
        }
    }
}