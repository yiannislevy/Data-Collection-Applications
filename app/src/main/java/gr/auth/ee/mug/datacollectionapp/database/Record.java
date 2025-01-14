package gr.auth.ee.mug.datacollectionapp.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import gr.auth.ee.mug.datacollectionapp.afts.upload.uploadfile.AftsFileType;

@Entity
public class Record {
    @PrimaryKey
    private int id;

    @ColumnInfo(name = "FullFileName")
    private String fullFileName;

    @ColumnInfo(name = "Uploaded")
    private boolean uploaded;

    @ColumnInfo(name = "FileType")
    private AftsFileType fileType;

    public Record(int id, String fullFileName, boolean uploaded, AftsFileType fileType){
        this.id = id;
        this.fullFileName = fullFileName;
        this.uploaded = uploaded;
        this.fileType = fileType;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setFullFileName(String fullFileName) {
        this.fullFileName = fullFileName;
    }

    public String getFullFileName() {return this.fullFileName;}


    public boolean isUploaded() {
        return uploaded;
    }

    public void setUploaded(boolean uploaded) {
        this.uploaded = uploaded;
    }

    public AftsFileType getFileType() {
        return fileType;
    }

    public void setFileType(AftsFileType fileType) {
        this.fileType = fileType;
    }
}
