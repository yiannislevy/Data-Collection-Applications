package gr.auth.ee.mug.datacollectionapp;

import static android.os.Build.VERSION.SDK_INT;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.util.List;

import gr.auth.ee.mug.datacollectionapp.afts.upload.uploadfile.AftsFileType;
import gr.auth.ee.mug.datacollectionapp.audiocapture.AudioCaptureManager;
import gr.auth.ee.mug.datacollectionapp.database.AppExecutors;
import gr.auth.ee.mug.datacollectionapp.database.Record;
import gr.auth.ee.mug.datacollectionapp.database.UploadLog;
import gr.auth.ee.mug.datacollectionapp.mandocapture.MandoCaptureManager;

public class CaptureService extends Service {
    private final IBinder iBinder = new LocalBinder();

    //Binder used so any activity/fragment can bind to the service and use its functions
    public class LocalBinder extends Binder {
        public CaptureService getService() {
            return CaptureService.this;
        }
    }

    private boolean running = false;
    private boolean mandoRunning = false;

    private AudioCaptureManager audioManager;
    private MandoCaptureManager mandoManager;
    private NotificationManager notificationManager;
    private String externalFilesDir;

    /**
     * Sets the mandoManager parameter to an already existing one passed as a parameter
     * @param mandoManager the already existing mandometer manager
     */
    public void setMandoManager(MandoCaptureManager mandoManager) {
        this.mandoManager = mandoManager;
    }

    @Override
    public void onCreate() {
        audioManager = new AudioCaptureManager();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(SDK_INT >= Build.VERSION_CODES.R)
            this.externalFilesDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        else
            this.externalFilesDir = Environment.getExternalStorageDirectory().toString();
        this.externalFilesDir += "/Recordings/Data_collection_app";
        File dir = new File(this.externalFilesDir);
        if(!dir.exists()) {
            try {
                if(!dir.mkdirs()) {
                    Toast.makeText(getApplicationContext(), "Problem with folder creation. Please kill app and restart!", Toast.LENGTH_SHORT).show();
                    System.exit(0);
                }
            } catch (Exception e) {
                Log.d("Directory making", e.getMessage());
            }
        }
    }


    /**
     * Creates the notification channel that will hold the notification to the user
     * @param notificationManager NotificationManager object created on onCreate that manages notifications for the service
     * @return the channels id that can be used to manage it
     */
    private String createNotificationChannel(NotificationManager notificationManager) {
        String channelId = "Capture service channel"; //Custom notification channel id
        String channelName = "Capture service"; //Custom notification channel name
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW); //Importance low so notification can appear on shed and taskbar but not make a sound
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC); //Visibility is public because no sensitive information is displayed and user can be constantly informed about recording
        notificationManager.createNotificationChannel(channel);
        return channelId;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!running) {
            String channelId = createNotificationChannel(notificationManager);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);

            Intent i = new Intent(getApplicationContext(), MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, i, PendingIntent.FLAG_IMMUTABLE);
            //Creating the foreground service notification
            Notification notification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentText("The app is recording.")
                    .setContentIntent(pendingIntent)
                    .build();

            // starting the recording process
            addToDB(audioManager.start(externalFilesDir), AftsFileType.AUDIO);
            running = true;
            startForeground(1, notification);
        } else {
            mandoManager.start(externalFilesDir);
            mandoRunning = true;
        }
        // Returns start_not_sticky because if the app is killed and then the OS tries to restart, problems occur
        return START_NOT_STICKY;
    }

    /**
     * Adds new file record to database
     * @param fileName The full path of the file to be added to record
     */
    public void addToDB(String fileName, AftsFileType fileType) {
        UploadLog uploadLog = UploadLog.getInstance(getApplicationContext());
        AppExecutors.getInstance().diskIO().execute(
                () -> {
                    List<Record> allRecords = uploadLog.uploadDao().getAll();
                    Record record = new Record(allRecords.size() + 1, fileName, false, fileType);
                    uploadLog.uploadDao().addRecord(record);
                }
        );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Stopping the recording process
        if(running) {
            audioManager.stop();
            if (mandoRunning) {
                addToDB(mandoManager.stop(), AftsFileType.WEIGHT);
                mandoRunning = false;
            }
            running = false;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }
}
