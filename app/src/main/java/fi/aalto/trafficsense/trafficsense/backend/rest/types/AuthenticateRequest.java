package fi.aalto.trafficsense.trafficsense.backend.rest.types;


import com.google.gson.annotations.SerializedName;

public class AuthenticateRequest {
    @SerializedName("userId")
    public final String UserId;

    @SerializedName("installationId")
    public final String InstallationId;

    /** The following 'device ID' is Android ID (unique per Android installation) */
    @SerializedName("deviceId")
    public final String DeviceId;

    @SerializedName("clientVersion")
    public final String ClientVersion;


    public AuthenticateRequest(String userId, String deviceId, String installationId, String clientVersion) {
        UserId = userId;
        InstallationId = installationId;
        DeviceId = deviceId;
        ClientVersion = clientVersion;
    }
}
