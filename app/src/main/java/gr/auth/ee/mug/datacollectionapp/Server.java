package gr.auth.ee.mug.datacollectionapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

import gr.auth.ee.mug.datacollectionapp.afts.signup.SignUpRequest;
import gr.auth.ee.mug.datacollectionapp.afts.signup.SignUpResponse;
import gr.auth.ee.mug.datacollectionapp.afts.upload.uploadfile.AftsFileType;
import gr.auth.ee.mug.datacollectionapp.afts.upload.uploadfile.UploadFileRequest;



public class Server {

    private Server() {
    }

    /**
     * Uploads provided files to server
     * @param context Application's context
     * @param filename Full file name(with directory)
     * @param filetype Should be one of: AftsFileType.AUDIO, AftsFileType.WEIGHT, AftsFileType.SENSOR_DATA
     * @return A listenable future that will be tied(as a callback) to a function
     */
    public static ListenableFuture<Void> upload(@NonNull Context context, String filename, AftsFileType filetype) {
        final UploadFileRequest request = new UploadFileRequest(context);

        final File db = new File(filename);
        final JSONObject metadata = new JSONObject();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault());
            metadata.put("timestamp", sdf.format(new Date()));
        } catch (JSONException e) {
            final SettableFuture<Void> dummyResult = SettableFuture.create();
            dummyResult.setException(e);
            return dummyResult;
        }

        filename = filename.substring(filename.length() - 19);
        return request.send(filetype, filename, metadata, db);
    }

    /**
     * Signs up a new user, informs the user if something went wrong(e.g. Username already exists)
     * @param passcode The folder/directory where the data are uploaded
     * @param username Unique to each user
     * @param context Application's context
     */
    public static void requestSignUp(String passcode, String username, Context context) {
        SignUpRequest request = new SignUpRequest();
        Futures.addCallback(request.send(passcode, username), new FutureCallback<SignUpResponse>() {
            @Override
            public void onSuccess(SignUpResponse result) {
                String jwt = result.getJwt();
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("Username", username);
                editor.putString("JWT", jwt);
                editor.apply();
            }

            @Override
            public void onFailure(Throwable t) {
                ContextCompat.getMainExecutor(context).execute(() -> Toast.makeText(context, t.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }, Executors.newSingleThreadExecutor());
    }
}
