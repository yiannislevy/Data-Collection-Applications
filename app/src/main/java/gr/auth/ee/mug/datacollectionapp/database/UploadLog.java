package gr.auth.ee.mug.datacollectionapp.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Database that tracks the files created by the app.
 * Tracks which files have been uploaded
 * Files no longer existing are deleted on every start up of the app(see MainActivity.refreshDB())
 */
@Database(entities = Record.class, exportSchema = false, version = 1)
public abstract class UploadLog extends RoomDatabase {
    private static final String DB_NAME = "Upload log";
    private static UploadLog instance;

    public static synchronized UploadLog getInstance(Context context) {
        if (instance == null){
            instance = Room.databaseBuilder(context.getApplicationContext(), UploadLog.class, DB_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

    public abstract UploadDao uploadDao();
}
