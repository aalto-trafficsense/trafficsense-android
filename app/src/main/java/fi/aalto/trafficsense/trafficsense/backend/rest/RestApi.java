package fi.aalto.trafficsense.trafficsense.backend.rest;

import fi.aalto.trafficsense.trafficsense.backend.rest.types.*;
import org.json.JSONObject;
import retrofit.Callback;
import retrofit.http.*;

public interface RestApi {
    @POST("/register")
    void register(@Body RegisterRequest request, Callback<RegisterResponse> callback);

    @POST("/authenticate")
    void authenticate(@Body AuthenticateRequest request, Callback<AuthenticateResponse> callback);

    @POST("/data")
    void data(@Query("sessionToken") String sessionToken, @Body DataBody body, Callback<JSONObject> callback);

    @GET("/device/{sessionToken}")
    void device(@Path("sessionToken") String sessionToken, Callback<DeviceResponse> callback);
}
