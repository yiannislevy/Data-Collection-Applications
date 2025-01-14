package gr.auth.ee.mug.datacollectionapp.sensorcapture.Database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FileDAO {

    @Insert
    long insert(FileEntity fileEntity);

    @Update
    void update(FileEntity fileEntity);

    @Update
    void updateAll(List<FileEntity> fileEntities);

    @Delete
    void delete(FileEntity fileEntity);

    @Query("SELECT * FROM file_table")
    List<FileEntity> getAllFiles();

    @Query("SELECT * FROM file_table WHERE status = :status")
    List<FileEntity> getFilesByStatus(String status);

    @Query("SELECT * FROM file_table WHERE fileName = :fileName")
    FileEntity getFileByName(String fileName);
}
