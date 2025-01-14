package gr.auth.ee.mug.datacollectionapp.wear;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.concurrent.atomic.AtomicBoolean;


public class ListenerService extends WearableListenerService {
    private static final String TAG = "ListenerService";
    private static final String SENSOR_CAPTURING_PATH = "/sensor_capturing";
    private static final String FILE_ACK_SERVICE_PATH = "/file_ack_service";
    private final String SENSOR_CAPTURING_ORDER_KEY = "sensor-capture-message-broadcast";
    private final String FILE_ACKNOWLEDGEMENT_KEY = "file-receiver-service-ack-broadcast";
    private final AtomicBoolean running = new AtomicBoolean(false);

    public void onCreate() {
        super.onCreate();
    }

    @SuppressLint("VisibleForTests")
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem dataItem = event.getDataItem();
                DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
                String path = dataItem.getUri().getPath();
                String message;
                switch (path) {
                    case SENSOR_CAPTURING_PATH:
                        message = dataMap.getString("order");
                        Log.d(TAG, message + " sensor capturing");
                        handleMessage(SENSOR_CAPTURING_ORDER_KEY, "order", message);
                        break;
                    case FILE_ACK_SERVICE_PATH:
                        message = dataMap.getString("file_ack");
                        Log.d(TAG, message + " file ack");
                        handleMessage(FILE_ACKNOWLEDGEMENT_KEY, "ack", message);
                        break;
                }
            }
        }
    }

    private void handleMessage(String action, String key, String message) {
        if (action.equals(SENSOR_CAPTURING_ORDER_KEY)) {
            Intent serviceIntent = new Intent(this, SensorCaptureService.class);
            if (message.equals("start") && !running.get()) {
                running.set(true);
                startForegroundService(serviceIntent);
            } else if (message.equals("stop")) {
                running.set(false);
                stopService(serviceIntent);
            }
        } else if (action.equals(FILE_ACKNOWLEDGEMENT_KEY)){
            Log.d(TAG+" in sendBroadcast", "file ack broadcast order received");
            Intent intent = new Intent(action);
            intent.putExtra(key, message);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }
}