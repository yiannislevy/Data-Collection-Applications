package gr.auth.ee.mug.datacollectionapp.afts.signup;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import gr.auth.ee.mug.datacollectionapp.afts.Afts;
import gr.auth.ee.mug.datacollectionapp.afts.AftsInterfaces;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class SignUpRequest {

    @NonNull private final AftsInterfaces afts;

    public SignUpRequest() {
        afts = Afts.getInstance();
    }

    @NonNull
    private static Call<SignUpResponse> createCall(@NonNull AftsInterfaces afts, @NonNull JSONObject params) {
        return afts.signUp(MultipartBody.Part.createFormData("params", params.toString()));
    }

    @NonNull
    private static JSONObject createParams(@NonNull String studyName, @NonNull String userName) throws JSONException {
        final JSONObject json = new JSONObject();

        json.put("passcode", studyName);
        json.put("username", userName);

        return json;
    }

    public ListenableFuture<SignUpResponse> send(@NonNull String studyName, @NonNull String userName) {
        final SettableFuture<SignUpResponse> future = SettableFuture.create();

        final JSONObject params;
        try {
            params = createParams(studyName, userName);
        } catch (JSONException e) {
            Log.e("Sign up request", "request(): error creating params", e);
            future.setException(e);
            return future;
        }

        createCall(afts, params).enqueue(new Callback<SignUpResponse>() {
            @Override
            public void onResponse(
                    @NonNull Call<SignUpResponse> call, @NonNull Response<SignUpResponse> response) {
                SignUpResponse signUpResponse = response.body();
                if (signUpResponse != null) {
                    future.set(response.body());
                } else {
                    try {
                        SignUpErrorResponse signUpErrorResponse = new Gson().fromJson(response.errorBody().string(),
                                SignUpErrorResponse.class);
                        future.setException(new Throwable(signUpErrorResponse.getTitle()
                                + ": "
                                + signUpErrorResponse.getDescription()));
                    } catch (IOException e) {
                        future.setException(new Throwable("Unknown error"));
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<SignUpResponse> call, @NonNull Throwable t) {
                future.setException(t);
            }
        });

        return future;
    }
}
