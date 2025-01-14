package gr.auth.ee.mug.datacollectionapp.afts.signup;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


public class SignUpResponse {

    @SerializedName("jwt")
    @Expose
    private String jwt;

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }
}
