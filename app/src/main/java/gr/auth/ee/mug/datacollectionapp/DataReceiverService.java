package gr.auth.ee.mug.datacollectionapp;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import com.google.android.gms.tasks.Tasks;

import gr.auth.ee.mug.datacollectionapp.afts.upload.uploadfile.AftsFileType;
import gr.auth.ee.mug.datacollectionapp.database.AppExecutors;
import gr.auth.ee.mug.datacollectionapp.database.Record;
import gr.auth.ee.mug.datacollectionapp.database.UploadLog;


public class DataReceiverService extends WearableListenerService {

    private static final String TAG = "DataReceiverService";
    private static final String FILE_PATH = "/sensor_file";
    private static final String ASSET_KEY = "file";
    private static final String FILE_NAME_KEY = "file_name";
    private static final String UPLOAD_APPROVED_PATH = "/upload";
    private static final String SENSOR_CAPTURING_RUNNING_PATH = "/running";
    private static final String WATCH_BATTERY_PATH = "/updated_battery";
    private String fullFilePath;

    @SuppressLint("VisibleForTests")
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                Uri uri = event.getDataItem().getUri();
                String receiving_path = uri.getPath();
                DataMapItem dataMapItem;
                switch (receiving_path) {
                    case FILE_PATH:
                        dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                        @SuppressLint("VisibleForTests")
                        Asset fileAsset = dataMapItem.getDataMap().getAsset(ASSET_KEY);
                        String fileName = dataMapItem.getDataMap().getString(FILE_NAME_KEY);

                        if (fileAsset != null) {
                            //save file to internal storage
                            if(!fileAlreadyExists(fileName)) {
                                File receivedFile = saveAssetToFile(getApplicationContext(), fileAsset, fileName);
                                if (receivedFile != null) {
                                    addToDB(fullFilePath, AftsFileType.SENSOR_DATA);
                                    sendAckToWatch(receivedFile.getName());
                                } else {
                                    Log.e(TAG, "receivedFile is NULL, failed to process it.");
                                }
                            }
                            else{
                                Log.e(TAG, "File already exists.");
                                sendAckToWatch(fileName);
                            }
                        }

                        break;
                    case UPLOAD_APPROVED_PATH:
                        dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                        String message_upload = dataMapItem.getDataMap().getString("upload_approved");
                        Intent intent_upload = new Intent("upload-approved-broadcast");
                        intent_upload.putExtra("upload_approved", message_upload);
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent_upload);
                        break;
                    case SENSOR_CAPTURING_RUNNING_PATH:
                        dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                        String message_running = dataMapItem.getDataMap().getString("running");
                        Intent intent_running = new Intent("running-broadcast");
                        intent_running.putExtra("running", message_running);
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent_running);
                        break;
                    case WATCH_BATTERY_PATH:
                        dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                        String updated_battery = dataMapItem.getDataMap().getString("updated_battery");
                        Intent intent_updated_battery = new Intent("updated-battery-broadcast");
                        intent_updated_battery.putExtra("battery_message", updated_battery);
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent_updated_battery);
                        break;
                }
            }
        }
    }


    private File saveAssetToFile(Context context, Asset asset, String fileName ) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }

        InputStream assetInputStream;
        try {
            assetInputStream = Tasks.await(Wearable.getDataClient(context).getFdForAsset(asset)).getInputStream();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ApiException) {
                ApiException apiException = (ApiException) e.getCause();
                if (apiException.getStatusCode() == 4005) {
                    Log.e(TAG, "Asset unavailable. It might be deleted or corrupted.", e);
                } else {
                    Log.e(TAG, "Error getting input stream for asset", e);
                }
            } else {
                Log.e(TAG, "Error getting input stream for asset", e);
            }
            return null;
        } catch (InterruptedException e) {
            Log.e(TAG, "Error getting input stream for asset", e);
            return null;
        }

        if (assetInputStream == null) {
            Log.w(TAG, "Requested an unknown Asset");
            return null;
        }

        String directoryPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Recordings/Data_collection_app";
        File directory = new File(directoryPath);

        File outputFile = new File(directory, fileName);
        this.fullFilePath = directoryPath + "/" + fileName;

        try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = assetInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
            return outputFile;
        } catch (IOException e) {
            Log.e(TAG, "Error saving asset to file", e);
            return null;
        }
    }

    public void addToDB(String fileName, AftsFileType fileType) {
        UploadLog uploadLog = UploadLog.getInstance(getApplicationContext());
        AppExecutors.getInstance().diskIO().execute(
                () -> {
                    Record record = new Record(uploadLog.uploadDao().getAll().size() + 1, fileName, false, fileType);
                    uploadLog.uploadDao().addRecord(record);
                }
        );
    }

    private boolean fileAlreadyExists(String fileName) {
        String directoryPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Recordings/Data_collection_app";
        File directory = new File(directoryPath);
        File fileToCheck = new File(directory, fileName);
        return fileToCheck.exists();
    }

    @SuppressLint("VisibleForTests")
    private void sendAckToWatch(String fileName) {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/file_ack_service");
        putDataMapRequest.getDataMap().putString("file_ack", fileName);
        PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();
        Task<DataItem> putDataTask = Wearable.getDataClient(this).putDataItem(putDataRequest);
        putDataTask.addOnSuccessListener(dataItem -> Log.d(TAG, "File ack message sent successfully")).addOnFailureListener(e -> Log.d(TAG, "File ack message FAILED to send"));
    }
}