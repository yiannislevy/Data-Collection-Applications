package gr.auth.ee.mug.datacollectionapp.afts.upload.uploadjson;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


public class UploadJsonResponse {

    @SerializedName("nof_fields")
    @Expose
    private int nofFields;

    @SerializedName("nof_json_chars")
    @Expose
    private int nofJsonChars;

    public int getNofJsonChars() {
        return nofJsonChars;
    }

    public void setNofJsonChars(int nofJsonChars) {
        this.nofJsonChars = nofJsonChars;
    }

    public int getNofFields() {
        return nofFields;
    }

    public void setNofFields(int nofFields) {
        this.nofFields = nofFields;
    }
}
