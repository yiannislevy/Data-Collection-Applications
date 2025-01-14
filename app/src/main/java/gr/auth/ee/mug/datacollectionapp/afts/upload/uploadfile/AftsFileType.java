package gr.auth.ee.mug.datacollectionapp.afts.upload.uploadfile;

import androidx.annotation.NonNull;


public enum AftsFileType {
    PHOTO("photo"),
    LOCATION("gps"),
    SYNC_DATA("sync-data-file"),
    LOGGED_DATA("logged-data-file"),
    DIAGNOSTICS("diagnostics"),
    AUDIO("audio"),
    WEIGHT("weight"),
    SENSOR_DATA("sensor-data");

    @NonNull public final String label;

    AftsFileType(@NonNull String label) {
        this.label = label;
    }
}
