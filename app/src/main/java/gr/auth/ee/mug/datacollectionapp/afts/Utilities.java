package gr.auth.ee.mug.datacollectionapp.afts;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Response;


public class Utilities {

    private Utilities() {
    }

    @NonNull
    public static JSONObject createDummyMetadata() throws JSONException {
        final JSONObject json = new JSONObject();

        json.put("timestamp", "pointless");

        return json;
    }

    @NonNull
    public static String formatError(@NonNull Response response) {
        String s = "[" + response.code() + "] ";

        final ResponseBody errorBody = response.errorBody();
        if (errorBody != null) {
            try {
                s += errorBody.string();
            } catch (IOException e) {
                s = getMessageSafe(e);
            }
        }

        return s;
    }

    @NonNull
    public static String getMessageSafe(@NonNull Throwable t) {
        String message = t.getMessage();
        if (message == null) {
            message = "";
        }

        return message;
    }
}
