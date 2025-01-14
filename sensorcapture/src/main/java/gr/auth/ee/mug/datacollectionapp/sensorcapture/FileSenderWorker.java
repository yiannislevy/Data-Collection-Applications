package gr.auth.ee.mug.datacollectionapp.sensorcapture;

import static android.content.ContentValues.TAG;
import static gr.auth.ee.mug.datacollectionapp.sensorcapture.Database.AppDatabase.updateFileStatusDB;
import static gr.auth.ee.mug.datacollectionapp.sensorcapture.SensorCaptureManager.FILE_WAIT_TIME;
import static gr.auth.ee.mug.datacollectionapp.sensorcapture.SensorCaptureManager.dbLock;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import gr.auth.ee.mug.datacollectionapp.sensorcapture.Database.AppDatabase;
import gr.auth.ee.mug.datacollectionapp.sensorcapture.Database.FileEntity;

public class FileSenderWorker extends Worker {

    private final AppDatabase appDatabase;
    private final Context context;
    FileSharingManager _fileSharingManager;
    private AtomicInteger fileRetryCounter = new AtomicInteger(0);
    long startTime = 0;
    private final Semaphore semaphore = new Semaphore(0);


    public FileSenderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        appDatabase = AppDatabase.getInstance(context);
        _fileSharingManager = new FileSharingManager(context);
        LocalBroadcastManager.getInstance(context).registerReceiver(fileAckReceiver, new IntentFilter("file-receiver-service-ack-broadcast"));
    }

    @NonNull
    @Override
    public Result doWork() {
        setUploadMessageSent(false);
        Log.d("FileSenderWorker", "Work initialized");
        updateClosedPendingFiles();
        List<FileEntity> completedFiles = appDatabase.fileDao().getFilesByStatus("completed");
        Log.d("fileSenderWorker", "completedFilesSize: " + completedFiles.size());
        fileRetryCounter.set(0);
        try {
            while (completedFiles.size() > 0) {

                if (fileRetryCounter.get() == 1) {
                    startTime = System.currentTimeMillis();
                }

                for (FileEntity fileEntity : completedFiles) {
                    File file = new File(getApplicationContext().getExternalFilesDir(null), fileEntity.getFileName());
                    if (file.exists()) {
                        semaphore.drainPermits();
                        System.out.println("Sending file: " + fileEntity.getFileName() + "...");
                        _fileSharingManager.shareFile(file);
                        try {
                            boolean acquired = semaphore.tryAcquire(FILE_WAIT_TIME, TimeUnit.MILLISECONDS);
                            if (!acquired) {
                                // Handle the timeout case, for example:
                                Log.d("FileSenderWorker", "Timeout while waiting for acknowledgment for file " + fileEntity.getFileName());
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    } else {
                        //delete the entity from the db if it doesn't exist in the directory
                        synchronized (dbLock) {
                            appDatabase.fileDao().delete(fileEntity);
                        }
                        Log.e("fileSender @thread", "'completed' file does not exist: " + fileEntity.getFileName());
                    }
                    updateClosedPendingFiles();
                    completedFiles = appDatabase.fileDao().getFilesByStatus("completed");
                }
                if (!isUploadApproved()) {
                    fileRetryCounter.incrementAndGet();
                    if (fileRetryCounter.get() >= 5) {
                        Log.d("MainActivity @stop()", "File sending failed");
                        sendMessage("false");
                        break;
                    }
                    if(System.currentTimeMillis() - startTime > 180000 && fileRetryCounter.get() >= 3) {
                        Log.d("MainActivity @stop()", "File sending took too long and failed");
                        sendMessage("false");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("fileSender @thread", "Failed to send completed files: " + e.getMessage());
        }
        LocalBroadcastManager.getInstance(context).unregisterReceiver(fileAckReceiver);
        return Result.success();
    }

    public boolean isUploadApproved() {
        boolean isApproved = false;
        if (!isUploadMessageSent()) {
            List<FileEntity> pendingFiles = appDatabase.fileDao().getFilesByStatus("pending");
            List<FileEntity> completedFiles = appDatabase.fileDao().getFilesByStatus("completed");
            if (pendingFiles.size() == 0 && completedFiles.size() == 0) {
                Log.d("FileWorker", "No remaining files to send. Stopping worker...");
                    sendMessage("true");
                    isApproved = true;
                WorkManager.getInstance(getApplicationContext()).cancelAllWorkByTag("fileSenderWork");
            }
        }
        return isApproved || isUploadMessageSent();
    }

    private void updateClosedPendingFiles() {

        List<FileEntity> pendingEntities = appDatabase.fileDao().getFilesByStatus("pending");

        for (FileEntity fileEntity : pendingEntities) {
            File file = new File(getApplicationContext().getExternalFilesDir(null), fileEntity.getFileName());
            long initialFileSize = file.length();
            // Check if the file is closed by trying to read it.
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            long newFileSize = file.length();

            if (newFileSize == initialFileSize) {
                fileEntity.setStatus("completed");
                fileEntity.setTimeStamp(System.currentTimeMillis());
                synchronized (dbLock) {
                    appDatabase.fileDao().update(fileEntity);
                }
            }
        }
    }

    @SuppressLint("VisibleForTests")
    private void sendMessage(String value) {
        DataClient dataClient = Wearable.getDataClient(context);

        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/upload");
        putDataMapRequest.getDataMap().putString("upload_approved", value);

        // To force onDataChanged to be called on the receiving side even if the data is the same,
        // we use a unique timestamp to make the DataItem appear as new.
        putDataMapRequest.getDataMap().putLong("timestamp", System.currentTimeMillis());

        PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();

        Task<DataItem> putDataTask = dataClient.putDataItem(putDataRequest);
        putDataTask.addOnSuccessListener(aVoid -> setUploadMessageSent(true))
                .addOnFailureListener(e -> Log.d(TAG, "upload_approved" + " message failed to send"));
    }

    private final BroadcastReceiver fileAckReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("ack");
            if (message != null) {

                Runnable db_update = () -> {
                    Log.d("FileSenderWorker", "fileAckReceiver: " + message + "\n\n");
                    updateFileStatusDB(message, "sent");
                    semaphore.release();  // Signal the doWork method to continue with the next file
                    isUploadApproved();
                };

                ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
                dbExecutor.execute(db_update);
            }
        }
    };

    private boolean isUploadMessageSent() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("FileSenderPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean("uploadMessageSent", false);
    }

    private void setUploadMessageSent(boolean sent) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("FileSenderPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("uploadMessageSent", sent);
        editor.apply();
    }

    @Override
    public void onStopped() {
        super.onStopped();
        LocalBroadcastManager.getInstance(context).unregisterReceiver(fileAckReceiver);
    }

}