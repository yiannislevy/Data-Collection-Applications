package gr.auth.ee.mug.datacollectionapp.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * DAO for accessing UploadLog ROOM database
 * Contains/implements only methods useful for the app
 */
@Dao
public interface UploadDao {
    @Query("SELECT * FROM record")
    List<Record> getAll();

    @Insert
    void addRecord(Record record);

    @Insert
    void addMultipleRecords(List<Record> records);

    @Update
    void updateRecord(Record record);

    @Delete
    void deleteRecords(List<Record> records);
}