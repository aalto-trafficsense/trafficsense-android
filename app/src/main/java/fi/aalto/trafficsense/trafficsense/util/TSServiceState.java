package fi.aalto.trafficsense.trafficsense.util;

public enum TSServiceState {
    STARTING, RUNNING, SLEEPING, STOPPING, STOPPED;

    public static String getServiceStateString(TSServiceState state) {
        switch (state) {
            case STARTING:
                return "Starting";
            case RUNNING:
                return "Running";
            case SLEEPING:
                return "Sleeping";
            case STOPPING:
                return "Stopping";
            case STOPPED:
                return "Stopped";
            default:
                return "Unknown state";
        }
    }

}