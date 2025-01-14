package gr.auth.ee.mug.datacollectionapp.afts.upload.uploadfile;

import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


public class UploadDataResponse {

    @SerializedName("md5sum")
    @Expose
    private String md5sum;

    @NonNull
    public String getMd5sum() {
        return md5sum;
    }

    public void setMd5sum(@NonNull String md5sum) {
        this.md5sum = md5sum;
    }
}
