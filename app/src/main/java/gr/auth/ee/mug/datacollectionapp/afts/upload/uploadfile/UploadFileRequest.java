package gr.auth.ee.mug.datacollectionapp.afts.upload.uploadfile;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import gr.auth.ee.mug.datacollectionapp.afts.Afts;
import gr.auth.ee.mug.datacollectionapp.afts.AftsInterfaces;
import gr.auth.ee.mug.datacollectionapp.afts.SimpleCallback;
import retrofit2.Call;

import static gr.auth.ee.mug.datacollectionapp.afts.upload.Utilities.createPart;


public class UploadFileRequest {

    @NonNull private final AftsInterfaces afts;

    public UploadFileRequest(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String jwt = preferences.getString("JWT", "");
        afts = Afts.getInstance(jwt);
    }

    @NonNull
    static Call<UploadDataResponse> createCall(
            @NonNull AftsInterfaces afts,
            @NonNull JSONObject params,
            @NonNull JSONObject metadata,
            @NonNull File dataFile) {
        // Order matters, don't change (should be: params, metadata, datafile)
        return afts.uploadData(createPart("params", params),
                createPart("metadata", metadata),
                createPart("datafile", dataFile));
    }

    @NonNull
    static JSONObject createParams(@NonNull AftsFileType dataType, @NonNull String filename) throws JSONException {
        final JSONObject json = new JSONObject();

        json.put("filename", filename);
        json.put("dataType", dataType.label);

        return json;
    }

    public ListenableFuture<Void> send(
            @NonNull AftsFileType fileType,
            @NonNull String filename,
            @NonNull JSONObject metadata,
            @NonNull File dataFile) {
        final SettableFuture<Void> future = SettableFuture.create();

        final JSONObject params;
        try {
            params = createParams(fileType, filename);
        } catch (JSONException e) {
            future.setException(e);
            return future;
        }

        createCall(afts, params, metadata, dataFile).enqueue(new SimpleCallback<>(future));

        return future;
    }
}
