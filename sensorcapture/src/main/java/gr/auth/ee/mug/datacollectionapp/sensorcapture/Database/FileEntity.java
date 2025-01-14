package gr.auth.ee.mug.datacollectionapp.sensorcapture.Database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "file_table")
public class FileEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "FileName")
    private String fileName;

    @ColumnInfo(name = "Status")
    private String status;

    @ColumnInfo(name = "TimeStamp")
    private long timeStamp;

    public FileEntity(String fileName, String status, long timeStamp) {
        this.fileName = fileName;
        this.status = status;
        this.timeStamp = timeStamp; //last time accessed
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

}
