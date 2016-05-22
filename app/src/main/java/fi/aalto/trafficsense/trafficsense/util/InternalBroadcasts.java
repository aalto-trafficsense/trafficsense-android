package fi.aalto.trafficsense.trafficsense.util;

/**
 * Internal broadcast types for asynchronous communications
 */
public class InternalBroadcasts {
    public static final String KEY_USER_ID_CLEARED = "USER_ID_CLEARED";
    public static final String KEY_USER_ID_SET = "USER_ID_SET";
    public static final String KEY_ONE_TIME_TOKEN_CLEARED = "ONE_TIME_TOKEN_CLEARED";
    public static final String KEY_ONE_TIME_TOKEN_SET = "ONE_TIME_TOKEN_SET";
    public static final String KEY_SESSION_TOKEN_CLEARED = "SESSION_TOKEN_CLEARED";
    public static final String KEY_CLIENT_NUMBER_FETCH_COMPLETED = "DEVICE_ID_FETCH_COMPLETED";


    public static final String KEY_SERVER_CONNECTION_FAILURE = "SERVER_CONNECTION_FAILURE";
    public static final String KEY_SERVER_CONNECTION_SUCCEEDED = "SERVER_CONNECTION_SUCCEEDED";

    public static final String KEY_REGISTRATION_SUCCEEDED = "REGISTRATION_SUCCEEDED";

    public static final String KEY_REQUEST_SIGN_IN = "REQUEST_SIGN_IN";

    public static final String KEY_REQUEST_AUTHENTICATION = "REQUEST_AUTHENTICATION";
    public static final String KEY_RETURNED_AUTHENTICATION_RESULT = "RETURNED_AUTHENTICATION_RESULT";
    public static final String KEY_RETURNED_AUTHENTICATION_RESULT_ERR_MSG = "RETURNED_AUTHENTICATION_RESULT_MSG";
    public static final String KEY_AUTHENTICATION_SUCCEEDED = "AUTHENTICATION_SUCCEEDED";
    public static final String KEY_AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
    public static final String KEY_SIGNED_OUT = "SIGNED_OUT";

    public static final String KEY_UPLOAD_REQUEST = "UPLOAD_REQUEST";
    public static final String KEY_UPLOAD_STARTED = "UPLOAD_STARTED";
    public static final String KEY_UPLOAD_SUCCEEDED = "UPLOAD_SUCCEEDED";
    public static final String KEY_UPLOAD_FAILED = "UPLOAD_FAILED";
    public static final String KEY_UPLOAD_TIME = "UPLOAD_TIME";

    public static final String KEY_SERVICE_STATE_UPDATE = "SERVICE_STATE_UPDATE";
    public static final String KEY_UPLOAD_STATE_UPDATE = "UPLOAD_STATE_UPDATE";
    public static final String LABEL_STATE_INDEX = "STATE_IDX";
    public static final String LABEL_CLIENT_NUMBER = "CLIENT_NUMBER";

    public static final String KEY_QUEUE_LENGTH_UPDATE = "QUEUE_LENGTH_UPDATE";
    public static final String LABEL_QUEUE_LENGTH = "QUEUE_LENGTH";
    public static final String LABEL_QUEUE_THRESHOLD = "QUEUE_THRESHOLD";

    public static final String KEY_SERVICE_START = "SERVICE_START";
    public static final String KEY_SERVICE_STOP = "SERVICE_STOP";

    public static final String KEY_LOCATION_UPDATE = "LOCATION_UPDATE";
    public static final String KEY_ACTIVITY_UPDATE = "ACTIVITY_UPDATE";
    public static final String KEY_SENSORS_UPDATE = "SENSORS_UPDATE";

    public static final String KEY_DEBUG_SETTINGS_REQ = "DEBUG_SETTINGS_REQ";
    public static final String KEY_DEBUG_SETTINGS = "DEBUG_SETTINGS";
    public static final String KEY_DEBUG_SHOW_REQ = "DEBUG_SHOW_REQ";
    public static final String KEY_DEBUG_SHOW = "DEBUG_SHOW";
    public static final String KEY_MAIN_ACTIVITY_REQ = "MAIN_ACTIVITY_REQ";

    public static final String KEY_VIEW_PAUSED = "VIEW_PAUSED";
    public static final String KEY_VIEW_RESUMED = "VIEW_RESUMED";

    public static final String KEY_SETTINGS_ACTIVITY_INTERVAL = "SETTINGS_ACTIVITY_INTERVAL_CHG";
    public static final String KEY_SETTINGS_LOCATION_INTERVAL = "SETTINGS_LOCATION_INTERVAL_CHG";


}
