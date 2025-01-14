package gr.auth.ee.mug.datacollectionapp.afts;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.SettableFuture;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static gr.auth.ee.mug.datacollectionapp.afts.Utilities.formatError;


public class SimpleCallback<T> implements Callback<T> {

    @NonNull private final SettableFuture<Void> future;

    @Deprecated
    public SimpleCallback(@NonNull SettableFuture<Void> future) {
        this.future = future;
    }

    @Override
    public void onResponse(@NonNull Call<T> call, @NonNull Response<T> response) {
        if (response.isSuccessful()) {
            future.set(null);
        } else {
            future.setException(new RuntimeException(formatError(response)));
        }
    }

    @Override
    public void onFailure(@NonNull Call<T> call, @NonNull Throwable t) {
        future.setException(t);
    }
}
