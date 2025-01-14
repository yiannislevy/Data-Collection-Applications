package gr.auth.ee.mug.datacollectionapp.sensorcapture;

import static android.content.ContentValues.TAG;
import static android.content.Context.SENSOR_SERVICE;

import static gr.auth.ee.mug.datacollectionapp.sensorcapture.Database.AppDatabase.insertFileDB;
import static gr.auth.ee.mug.datacollectionapp.sensorcapture.Database.AppDatabase.updateFileStatusDB;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.lang.Thread;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import java.util.concurrent.atomic.AtomicBoolean;

import gr.auth.ee.mug.datacollectionapp.sensorcapture.Database.AppDatabase;
import gr.auth.ee.mug.datacollectionapp.sensorcapture.Database.FileEntity;

public class SensorCaptureManager implements SensorEventListener{

    private static SensorCaptureManager instance;
    final private Context context;
    final private SensorManager sensorManager;
    final private Sensor accelerometer, gyroscope;
    private DataOutputStream accOutputStream, gyroOutputStream;
    final private Object lockAcc = new Object();
    final private Object lockGyro = new Object();
    public static final Object dbLock = new Object();
    private static long startTime;
    private File acc_file, gyro_file;
    private String acc_file_name, gyro_file_name;
    private AtomicBoolean threadsRunning;
    public Thread fileSplitter, runningAck;
    static final private int MAX_TIME = 900000;
    public static final int FILE_WAIT_TIME = 30000;
    public FileSharingManager _fileSharingManager;
    final private AppDatabase appDatabase;
    public float runningTimestamp;

    //for wakelock
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;

    private void cleanDBAndDeleteSentFiles() {
        List<FileEntity> allFiles = appDatabase.fileDao().getAllFiles();
        if (allFiles.size() > 0) {
            for (FileEntity fileEntity : allFiles) {
                File file = new File(context.getExternalFilesDir(null), fileEntity.getFileName());
                if (!file.exists()) {
                    appDatabase.fileDao().delete(fileEntity);
                    Log.e("fileSplitter @thread", "file " + fileEntity.getFileName() + " does not exist");
                }
                // uncomment code below to delete sent files
//                else if (fileEntity.getStatus().equals("sent")) {
//                    if(file.delete()) {
//                        appDatabase.fileDao().delete(fileEntity);
//                        Log.e("fileSplitter @thread", "file " + fileEntity.getFileName() + " deleted");
//                    } else {
//                        Log.e("fileSplitter @thread", "file " + fileEntity.getFileName() + " could not be deleted");
//                    }
//                }
                //sleep for 5 seconds between each file
//                try {
//                    Thread.sleep(5000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            }
        }
    }

    @SuppressLint("VisibleForTests")
    private void sendMessage(String path, String key, String value) {
        DataClient dataClient = Wearable.getDataClient(context);

        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(path);
        putDataMapRequest.getDataMap().putString(key, value);

        // To force onDataChanged to be called on the receiving side even if the data is the same,
        // we use a unique timestamp to make the DataItem appear as new.
        putDataMapRequest.getDataMap().putLong("timestamp", System.currentTimeMillis());

        PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();

        Task<DataItem> putDataTask = dataClient.putDataItem(putDataRequest);
        putDataTask.addOnSuccessListener(aVoid -> Log.d(TAG, key + " message sent successfully"))
                .addOnFailureListener(e -> Log.d(TAG, key + " message failed to send"));
    }

    private Runnable createFileSplitterRunnable() {
        return () -> {
            while (threadsRunning.get()) {
                if (System.currentTimeMillis() - startTime > MAX_TIME) {
                    startTime = System.currentTimeMillis();
                    //ACCELEROMETER
                    String prev_acc_file_name = acc_file_name;
                    acc_file_name = startTime + "_accelerometer.bin";
                    try {
                        //----------------- START split file logic-----------------
                        acc_file = new File(context.getExternalFilesDir(null), acc_file_name);
                        DataOutputStream newAccOutputStream = new DataOutputStream(new FileOutputStream(acc_file));
                        DataOutputStream oldAccOutputStream = accOutputStream;
                        synchronized (lockAcc) {
                            accOutputStream = newAccOutputStream;
                        }
                        oldAccOutputStream.close();
                        //----------------- END split file logic -----------------
                        updateFileStatusDB(prev_acc_file_name, "completed");
                        insertFileDB(acc_file.getName());
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("fileSplitter @thread", "Failed to split/update on db the acc file: " + e.getMessage());
                    }
                    // GYROSCOPE
                    String prev_gyro_file_name = gyro_file_name;
                    gyro_file_name = startTime + "_gyroscope.bin";
                    try {
                        //----------------- START split file logic-----------------
                        gyro_file = new File(context.getExternalFilesDir(null), gyro_file_name);
                        DataOutputStream newGyroOutputStream = new DataOutputStream(new FileOutputStream(gyro_file));
                        DataOutputStream oldGyroOutputStream = gyroOutputStream;
                        synchronized (lockGyro) {
                            gyroOutputStream = newGyroOutputStream;
                        }
                        oldGyroOutputStream.close();
                        //----------------- END split file logic -----------------
                        updateFileStatusDB(prev_gyro_file_name, "completed");
                        insertFileDB(gyro_file.getName());
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("fileSplitter @thread", "Failed to split/update on db gyro file: " + e.getMessage());
                    }
                    try {
                        Thread.sleep(MAX_TIME);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.e("fileSplitter @thread", "was interrupted by stop(): " + e.getMessage());
                        threadsRunning.set(false);
                    }
                }
            }
        };
    }

    private Runnable createRunningAckRunnable() {
        return () -> {
            while (threadsRunning.get()) {
                sendMessage("/running", "running", "running");
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.e("stillRunningAck @thread", "was interrupted by stop(): " + e.getMessage());
                }
            }
            this.runningTimestamp = System.currentTimeMillis();
        };
    }

    private SensorCaptureManager (Context context) {
        this.context = context;

        threadsRunning = new AtomicBoolean(false);

        // Initialize the SensorManager and the sensors
        sensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        _fileSharingManager = new FileSharingManager(context);
        appDatabase = AppDatabase.getInstance(context);

        //for wakelock object initialization
        powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SensorCaptureManager::MyWakelockTag");
    }

    public static synchronized SensorCaptureManager getInstance(Context context) {
        if (instance == null) {
            instance = new SensorCaptureManager(context);
        }
        return instance;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                synchronized (lockAcc) {
                    try {
                        accOutputStream.writeFloat(event.values[0]);
                        accOutputStream.writeFloat(event.values[1]);
                        accOutputStream.writeFloat(event.values[2]);
                        accOutputStream.writeLong(event.timestamp);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case Sensor.TYPE_GYROSCOPE:
                synchronized (lockGyro) {
                    try {
                        gyroOutputStream.writeFloat(event.values[0]);
                        gyroOutputStream.writeFloat(event.values[1]);
                        gyroOutputStream.writeFloat(event.values[2]);
                        gyroOutputStream.writeLong(event.timestamp);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // This method is not used
    }

    public void start() {

        if(!threadsRunning.get()) {
//            first_iteration = true;

            startTime = System.currentTimeMillis();
            System.out.println(startTime);

            acc_file_name = startTime + "_accelerometer.bin";
            acc_file = new File(context.getExternalFilesDir(null), acc_file_name);
            try {
                accOutputStream = new DataOutputStream(new FileOutputStream(acc_file));
            } catch (IOException e) {
                e.printStackTrace();
            }
            FileEntity accFileEntity = new FileEntity(acc_file.getName(), "pending", System.currentTimeMillis());

            gyro_file_name = startTime + "_gyroscope.bin";
            gyro_file = new File(context.getExternalFilesDir(null), gyro_file_name);
            try {
                gyroOutputStream = new DataOutputStream(new FileOutputStream(gyro_file));
            } catch (IOException e) {
                e.printStackTrace();
            }
            FileEntity gyroFileEntity = new FileEntity(gyro_file.getName(), "pending", System.currentTimeMillis());

            Executor executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                synchronized (dbLock) {
                    appDatabase.fileDao().insert(accFileEntity);
                    appDatabase.fileDao().insert(gyroFileEntity);
                }
            });

            // Register the sensors with the specified sampling rate and reporting mode
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);

            this.threadsRunning.set(true);

            startThreads();
            Executors.newSingleThreadExecutor().execute(this::cleanDBAndDeleteSentFiles);
            sendStatusUpdateUI("Started");

            //Start wakelock
            try {
                wakeLock.acquire(600*60*1000L /*10 hours*/);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("MainActivity wakelock acquire @start()", "An exception was thrown: " + e.getMessage());
            }
        }
    }
    public void stop() {
        this.threadsRunning.set(false);
        sendStatusUpdateUI("Stopped");

        sensorManager.unregisterListener(this);

        stopThreads();

        stopStream(accOutputStream);
        stopStream(gyroOutputStream);

        try {
            wakeLock.release();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("MainActivity wakelock release @stop()", "An exception was thrown: " + e.getMessage());
        }
    }

    private void sendStatusUpdateUI(String status) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("SensorCapturePrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("status", status);
        editor.apply();

        Intent intent = new Intent("sensor-capture-status-update");
        intent.putExtra("status", status);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void startThreads() {
        this.fileSplitter = new Thread(createFileSplitterRunnable());
        try {
            fileSplitter.start();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("MainActivity fileSplitter @start()", "An exception was thrown: " + e.getMessage());
        }

        this.runningAck = new Thread(createRunningAckRunnable());
        try {
            runningAck.start();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("MainActivity runningAck @start()", "An exception was thrown: " + e.getMessage());
        }
    }

    private void stopThreads() {
        this.fileSplitter.interrupt();
        try {
            this.fileSplitter.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        this.runningAck.interrupt();
        try {
            this.runningAck.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void stopStream(DataOutputStream stream) {
        try {
            stream.flush();
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}