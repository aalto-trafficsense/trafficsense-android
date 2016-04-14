package fi.aalto.trafficsense.trafficsense.backend.rest.types;

import com.google.gson.annotations.SerializedName;

public class AuthenticateResponse {
    @SerializedName("sessionToken")
    public final String mSessionToken;

    public AuthenticateResponse(String sessionToken) {
        this.mSessionToken = sessionToken;
    }
}
