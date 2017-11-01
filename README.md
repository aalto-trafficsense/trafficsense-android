# TrafficSense-Android

The second Android client to access the [TrafficSense](https://github.com/aalto-trafficsense) [server](https://github.com/aalto-trafficsense/regular-routes-server) (the first one is [Learning Regular Routes](https://github.com/aalto-trafficsense/regular-routes-client)). This client is targeting towards an end-user frontend, but also incorporates debugging features, which are hidden by default.

# 1. Setting up a development machine

1. Install [Android Studio](https://developer.android.com/sdk/installing/studio.html)<br>
   *or*<br>
   install [IntelliJ IDEA](http://www.jetbrains.com/idea/download/) and [Android SDK](https://developer.android.com/sdk/index.html)
2. Use Android SDK manager (installed by Android Studio or Android SDK) to install at least the following packages:

    + Tools
      + Android SDK Tools
      + Android SDK Platform-tools
      + Android SDK Build-tools (Many versions ok, currently building with 26.0.2)
    + Android 6.0 (API 23, "Marshmallow")
      + Google APIs
      + Android SDK Platform 23
    + Extras
      + Android Support Repository
      + Android Support Library
      + Google Play services
      + Google Repository
      
   If running in an emulator, install also:
   
   + Tools
     + Android Emulator
     + Intel x86 Emulator Accelerator (HAXM installer)
   + Android 6.0 (API 23, "Marshmallow")
     + Google APIs Intel x86 Atom_64 System Image
   
# 2. Documentation
      
## 2.1 Authentication

The server-client authentication is done using Google OAuth2 service provided through Google+ API. User doesn't have to have a Google+ account; a normal Google account is enough.
  
The registration flow follows Google OAuth2 guidelines. First Android client requests single use token from Google+ API that provides access to users Google profile. This triggers Android to prompt consent from the user before providing client with working google id + token. After receiving token, Android client sends it to the server that verifies its validity from Google API. Both sides calculates 'client id' hash value from users Google id that guarantees same result for any future registrations. That id is used every time when client authenticates later sessions to get a valid session token. That session token is then included in every transaction as a token of valid transaction from client. 
  
When server gets response from Google authentication service, it registers client (stores it's information + received tokens from Google) or responds with "Forbidden" status in case the access token was invalid. Successful registration is responded with HTTP code 200 (OK) + valid session token. Separate authentication API call is not then needed after successful registration.

When client gets response of successful registration, it stores the session token and the client id it calculated previously. With these values stored, client doesn't need to re-register after each time. 

The whole process is described with the following picture that derives the basic elements from Google's OAuth2 description: 
    ![Registration](http://i.imgur.com/A5BpdXA.png)
  
[Source: Google OAuth2 documentation](https://developers.google.com/accounts/docs/OAuth2)

The label circulated with red color shows the order of steps. Google recommends that the authorization token is sent by using secure HTTPS connection with POST request. That implementation requires certification for server. If authorization token is leaked (e.g. via "man in the middle" attack), it still requires server's client id issued by Google to be valid for use. That is not considered to justify the need for proper HTTPS encryption. 

## 2.2 Concept of users and devices
User is identified by Google account. Same user may have several devices that all are linked to the same user. When user registers with different device, same hash value (user id) is used and therefore server will only add a new device for the same user. Otherwise registration does not differ from new user + new device -combination.

Note: When user uninstalls / reinstalls the client application that is considered as a new device (identified by new installation id). 

# 3. Building the client

## 3.1 Define the client configuration

The main configuration file to put all your Google service id:s (allocated in the Google developer console) and server addresses (the client supports two, "test" and "production") is google_service_keys.xml. Placeholder template files can be found in two paths:

* [app/src/debug/res/values/google_service_keys.xml](https://github.com/aalto-trafficsense/trafficsense-android/blob/master/app/src/debug/res/values/google_service_keys.xml)
* [app/src/release/res/values/google_service_keys.xml](https://github.com/aalto-trafficsense/trafficsense-android/blob/master/app/src/release/res/values/google_service_keys.xml)

Gradle picks up the first one when compiling a debug version and the second one for release builds, which allows to play with different configurations. Generation of the credentials is normally during server installation and explained in the [regular-routes-devops Readme.markdown](https://github.com/aalto-trafficsense/regular-routes-devops/blob/master/README.markdown). The keys are configured in the [Google developer console](https://console.developers.google.com/). The preparation of some credentials requires that the keystore is configured, please check instructions for that in the next section, if needed.

* `google_app_id`: Also called `project number`, can be found under the Google developer console "information". Separate projects can be used for the debug and release configurations.
* `google_maps_key`: The client is using Google Maps and therefore requires maps API to be enabled. On the [Google developer console](https://console.developers.google.com/) for your project under [Menu] "APIs and services" / "Dashboard" select "ENABLE APIS AND SERVICES". Find "Google Maps Android API" and "Enable API". Then, under "APIs and services" / "Credentials" / "Credentials": "Create credentials" create an "API Key", select "Android" and enter the following information:
    * Signing-certificate fingerprint: Paste the SHA1 (instructions for keystore configuration and extraction below 3.2-3.3)
    * Package name: From the "AndroidManifest.xml" file in the client: "fi.aalto.trafficsense.trafficsense" (or "...debug")
    * Press "Create". Copy the generated key (starting with "AIza...") to `google_maps_key`.
    * Separate keys can be allocated for debug and release configurations (note the different package names).
    * It is practical to do the other key allocation instructed in 3.2-3.3 at this time.
* `server_address_test` and `server_address_production`: Insert the http(s)-address of the server (terminate with `/api`) the client is being built for. The first address is for a test server, the second for a production server. If there is no separate test server, the addresses can be the same.
* `web_client_id_test` and `web_client_id_production`: Instructed to be generated during server setup in [devops instructions](https://github.com/aalto-trafficsense/regular-routes-devops/blob/master/README.markdown). Can be found from under "APIs & services" / "Credentials" / "OAuth 2.0 client IDs" / "Web client 1" / "Client ID".

Current versions of the system also support [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging/) for pushing traffic disruption notices to clients. The [Firebase console](https://console.firebase.google.com/) generates new credentials and configuration files named `google-services.json`, when the project is imported to Firebase (or generated on Firebase from scratch). The debug and production build copies of these files should be located in:

* [app/src/debug](https://github.com/aalto-trafficsense/trafficsense-android/blob/master/app/src/debug/google-services.json)
* [app/src/release](https://github.com/aalto-trafficsense/trafficsense-android/blob/master/app/src/release/google-services.json)

If there are no separate configurations for test and production server, a single `google-services.json` file can be placed directly into the `app` directory.

Whether to use the test server or production server is selected in `ts_configuration.xml`, again separately for debug and production builds. There are also two separate template copies in:

* [app/src/main/res/values/ts_configuration.xml](https://github.com/aalto-trafficsense/trafficsense-android/blob/master/app/src/main/res/values/ts_configuration.xml)
* [app/src/release/res/values/ts_configuration.xml](https://github.com/aalto-trafficsense/trafficsense-android/blob/master/app/src/release/res/values/ts_configuration.xml)

The current default in the files is to use the test server for debug builds and production server for release builds.

## 3.2 Configure your keystore (if needed)

Please check [further instructions on signing apps from Google](https://developer.android.com/tools/publishing/app-signing.html). Both debug- and release keys are ok. If using the debug-key, it is normally located in `~/.android/debug.keystore`. A keystore for a release key is generated with:

    $ keytool -genkey -v -keystore my-release-key.keystore -alias alias_name -keyalg RSA -keysize 2048 -validity 10000

Remember to record both the keystore password and key password! 
    
## 3.3 Paste your SHA1 fingerprint on the [Google developer console](https://console.developers.google.com/) 

SHA1 from the debug.keystore is extracted like this:

    $ keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android

The SHA1 of a release keystore is extracted with:

    $ keytool -list -v -keystore my-release-key.keystore -alias alias-name

On the console under "APIs & services" / "Credentials" / "Credentials": "Create credentials" create an "OAuth client ID" with the following information:
* Application type: Android
* Signing-certificate fingerprint: Paste the SHA1 as extracted above
* Package name: From the "AndroidManifest.xml" file in the client: "fi.aalto.trafficsense.trafficsense" (...".debug" for a debug client)
* Google+ deep linking is not used.
* Press "Create"

This key does not need to be entered anywhere, but it has to exist on the Google console for the project for the sign-in to work.

## 3.4 Build

With a debug key: Connect a phone via USB and run from the IDE. The configuration should be ready, but if not, it is "regularroutes" as an "Android Application". Module is "regularroutes", package "Deploy default APK".

With a release key: Select "Build" / "Generate signed APK". Select the proper keystore. Add the proper usernames and passwords. The key alias needs to be updated. For TrafficSense project sample environment the files (separate for test and production servers) are on the project drive. After the .apk-file is generated, copy to phone, install and run.

## 3.5 Build problems & solutions

### 3.5.1 IDE complains about non-Gradle & Gradle modules in the same project

Problem: Opening the client with Intellij IDEA after a new pull from repo, the following error is printed:

    Unsupported Modules Detected
    Compilation is not supported for following modules: regularroutes. Unfortunately you can't have non-Gradle Java modules and Android-Gradle modules in one project.

Solution: Make an arbitrary modification to "settings.gradle" (e.g. add an empty line) and respond "sync now" to the message that appears. The problem should disappear.

### 3.5.2 Gradle doesn't "see" the app

Problem: Gradle seems to be ignoring the application (in Android Studio under "Project" / "Android" there is only a "Gradle scripts" folder, no "app" folder).

Solution: Check that there is a top-level file "settings.gradle" with the content "include ':app'".

