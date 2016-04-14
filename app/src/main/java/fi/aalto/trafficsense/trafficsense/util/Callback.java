package fi.aalto.trafficsense.trafficsense.util;

public interface Callback<T> {
    void run(T result, RuntimeException error);
}
