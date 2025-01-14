package gr.auth.ee.mug.datacollectionapp.afts;

import gr.auth.ee.mug.datacollectionapp.afts.hello.HelloResponse;
import gr.auth.ee.mug.datacollectionapp.afts.signup.SignUpResponse;
import gr.auth.ee.mug.datacollectionapp.afts.upload.uploadfile.UploadDataResponse;
import gr.auth.ee.mug.datacollectionapp.afts.upload.uploadjson.UploadJsonResponse;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;


public interface AftsInterfaces {

    @POST("hello")
    Call<HelloResponse> hello();

    @Multipart
    @POST("signUp")
    Call<SignUpResponse> signUp(@Part MultipartBody.Part params);

    @Headers("Transfer-Encoding: chunked")
    @Multipart
    @POST("uploadData")
    Call<UploadDataResponse> uploadData(
            @Part MultipartBody.Part params, @Part MultipartBody.Part metadata, @Part MultipartBody.Part datafile);

    @Headers("Transfer-Encoding: chunked")
    @Multipart
    @POST("uploadJSON2")
    Call<UploadJsonResponse> uploadJson(
            @Part MultipartBody.Part params, @Part MultipartBody.Part content, @Part MultipartBody.Part metadata);
}
