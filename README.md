# TrafficSense-Android

The second Android client to access the [TrafficSense](https://github.com/aalto-trafficsense) [server](https://github.com/aalto-trafficsense/regular-routes-server) (the first one is "Learning Regular Routes"). This client is targeting towards an end-user frontend, but also incorporates debugging features that can be hidden.

More documentation on the client setup coming later, at the moment most of the relevant documentation is still in the [Learning Regular Routes client repository](https://github.com/aalto-trafficsense/regular-routes-client).

The main configuration file to put all your Google service id:s (allocated in the Google developer console) and server addresses (the client supports two, "test" and "production") is google_service_keys.xml. Placeholder files can be found in two paths:

* [app/src/debug/res/values/google_service_keys.xml](https://github.com/aalto-trafficsense/trafficsense-android/blob/master/app/src/debug/res/values/google_service_keys.xml)
* [app/src/release/res/values/google_service_keys.xml](https://github.com/aalto-trafficsense/trafficsense-android/blob/master/app/src/release/res/values/google_service_keys.xml)

Gradle picks up the first one when compiling a debug version and the second one for release builds, which allows to play with different configurations.
Whether to use the test server or production server is selected in ts_configuration.xml. There are also separate copies in:

* [app/src/main/res/values/ts_configuration.xml](https://github.com/aalto-trafficsense/trafficsense-android/blob/master/app/src/main/res/values/ts_configuration.xml)
* [app/src/release/res/values/ts_configuration.xml](https://github.com/aalto-trafficsense/trafficsense-android/blob/master/app/src/release/res/values/ts_configuration.xml)

This supports different settings for debug and release builds. The current default in the files is to use the test server for debug builds and production server for release builds.

