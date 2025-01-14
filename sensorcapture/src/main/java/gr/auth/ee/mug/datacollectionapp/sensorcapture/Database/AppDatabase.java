package gr.auth.ee.mug.datacollectionapp.sensorcapture.Database;

import static gr.auth.ee.mug.datacollectionapp.sensorcapture.SensorCaptureManager.dbLock;
import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {FileEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "file_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }

    public static void updateFileStatusDB(String fileName, String status) {
        FileEntity fileEntity = instance.fileDao().getFileByName(fileName);
        fileEntity.setStatus(status);
        fileEntity.setTimeStamp(System.currentTimeMillis());
        synchronized (dbLock) {
            instance.fileDao().update(fileEntity);
        }
    }

    public static void insertFileDB(String fileName) {
        FileEntity fileEntity = new FileEntity(fileName, "pending", System.currentTimeMillis());
        synchronized (dbLock) {
            instance.fileDao().insert(fileEntity);
        }
    }


    public abstract FileDAO fileDao();
}
