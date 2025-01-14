package gr.auth.ee.mug.datacollectionapp.sensorcapture;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileSharingManager {

    private static final String TAG = "FileSharingManager";
    private static final String FILE_PATH = "/sensor_file";
    private static final String FILE_KEY = "file";
    private static final String FILE_NAME_KEY = "file_name";
    ParcelFileDescriptor parcelFileDescriptor;
    File toShareFile;
    private Context context;

    public FileSharingManager (Context context) {
        this.context = context;
    }

    public void shareFile(File file) {
        try {
            this.toShareFile = file;
            Asset asset = createAssetFromFile(file);
            sendData(FILE_PATH, FILE_KEY, FILE_NAME_KEY, asset);
        } catch (IOException e) {
            Log.e(TAG, "Error creating asset from file", e);
        }
    }

    @SuppressLint("VisibleForTests")
    private Asset createAssetFromFile(File file) throws FileNotFoundException {
        this.parcelFileDescriptor =
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        return Asset.createFromFd(parcelFileDescriptor);
    }

    @SuppressLint("VisibleForTests")
    private void sendData(String path, String key, String FILE_NAME_KEY,Asset asset) {
        PutDataMapRequest dataMapRequest = PutDataMapRequest.create(path);
        dataMapRequest.getDataMap().putAsset(key, asset);
        dataMapRequest.getDataMap().putString(FILE_NAME_KEY, toShareFile.getName());
        PutDataRequest request = dataMapRequest.asPutDataRequest();

        Wearable.getDataClient(context).putDataItem(request)
                .addOnSuccessListener(new OnSuccessListener() {
                    @Override
                    public void onSuccess(Object o) {
                        try {
                            parcelFileDescriptor.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Error closing file descriptor", e);
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Failed to send data item: " + request.getUri(), e);
                    }
                });

    }
}