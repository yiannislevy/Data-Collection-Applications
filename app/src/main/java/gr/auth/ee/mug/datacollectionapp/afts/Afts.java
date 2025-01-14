package gr.auth.ee.mug.datacollectionapp.afts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class Afts {

    @Nullable private static AftsInterfaces aftsWithJwt = null;
    @Nullable private static AftsInterfaces aftsWithoutJwt = null;

    @NonNull
    public static AftsInterfaces getInstance() {
        return getInstance(null);
    }

    @NonNull
    public static AftsInterfaces getInstance(@Nullable String jwt) {
        if (jwt == null) {
            return getAftsWithoutJwt();
        } else {
            return getAftsWithJwt(jwt);
        }
    }

    @NonNull
    private static AftsInterfaces getAftsWithJwt(@NonNull String jwt) {
        if (aftsWithJwt == null) {
            aftsWithJwt = RetrofitClient.getInstance(jwt).create(AftsInterfaces.class);
        }

        return aftsWithJwt;
    }

    @NonNull
    private static AftsInterfaces getAftsWithoutJwt() {
        if (aftsWithoutJwt == null) {
            aftsWithoutJwt = RetrofitClient.getInstance(null).create(AftsInterfaces.class);
        }

        return aftsWithoutJwt;
    }
}
