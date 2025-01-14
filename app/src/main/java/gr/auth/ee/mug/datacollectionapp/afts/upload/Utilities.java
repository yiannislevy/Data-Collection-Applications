package gr.auth.ee.mug.datacollectionapp.afts.upload;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;
import org.json.JSONObject;

import java.io.File;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;


public class Utilities {

    private Utilities() {
    }

    @NonNull
    @Contract("_, _ -> new")
    public static MultipartBody.Part createPart(@NonNull String type, @NonNull String s) {
        return MultipartBody.Part.createFormData(type, s);
    }

    @NonNull
    @Contract("_, _ -> new")
    public static MultipartBody.Part createPart(@NonNull String type, @NonNull JSONObject json) {
        return MultipartBody.Part.createFormData(type, json.toString());
    }

    @NonNull
    @Contract("_, _ -> new")
    public static MultipartBody.Part createPart(@NonNull String type, @NonNull File dataFile) {
        final RequestBody requestBody = RequestBody.create(null, dataFile);

        return MultipartBody.Part.createFormData(type, dataFile.getName(), requestBody);
    }
}
