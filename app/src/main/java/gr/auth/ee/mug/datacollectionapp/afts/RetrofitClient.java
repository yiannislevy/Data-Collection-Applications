package gr.auth.ee.mug.datacollectionapp.afts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


class RetrofitClient {

    @NonNull private static final String SERVER_URL = "https://andromeda.ee.auth.gr/afts-rebecca-v0/";

    private RetrofitClient() {
    }

    @NonNull
    static Retrofit getInstance(@Nullable String jwt) {

        final OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        clientBuilder.addInterceptor(chain -> {
            final Request.Builder builder = chain.request().newBuilder();
            if (jwt != null) {
                builder.addHeader("Authorization", "Bearer " + jwt);
            }

            return chain.proceed(builder.build());
        });
        clientBuilder.connectTimeout(2, TimeUnit.HOURS);
        clientBuilder.readTimeout(2, TimeUnit.HOURS);
        clientBuilder.writeTimeout(2, TimeUnit.HOURS);

        return new Retrofit.Builder().client(clientBuilder.build())
                .baseUrl(SERVER_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();
    }
}
