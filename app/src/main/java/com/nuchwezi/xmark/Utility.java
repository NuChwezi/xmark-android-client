package com.nuchwezi.xmark;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.jar.JarFile;

/**
 * Created by NemesisFixx on 25-Jun-16.
 */
public class Utility {

    public static class APK {
        public static final String APK_UPDATE_CONFIG_URI = "https://raw.githubusercontent.com/NuChwezi/xmark-android-client/master/release/xmark-updates.json";
        public static final String APK_UPDATE_URI = "https://raw.githubusercontent.com/NuChwezi/xmark-android-client/master/release/XMark.apk";

        protected static final String APK_DOWNLOAD_PATH = createSDCardDir("XMARK_Updates");

        protected static final String NEW_APK_NAME = "XMark-Update.apk";
        public static final String KEY_VERSIONCODE = "versionCode";
        public static final String KEY_AUTOUPDATE = "autoUpdate";
        public static final boolean DEFAULT_AUTOUPDATE = false;
        public static final String KEY_FORCEUPDATE = "forceUpdate";
        public static final boolean DEFAULT_FORCEUPDATE = false;
        public static final String KEY_APPNAME = "appName";
        public static final String KEY_APPDESCRIPTION = "appDescription";
        public static final String KEY_UPDATEMESSAGE = "message";
        public static final String KEY_VERSIONNAME = "versionName";
        public static final String KEY_APK_URI = "apkUrl";
        public static final String KEY_UPDATEINFO = "updateInfo";
    }

    /*
	 * Will create directory on the External Storage Card with the given dirName
	 * name.
	 *
	 * Throws an exception is dirName is null, and returns the name of the
	 * created directory if successful
	 */
    public static String createSDCardDir(String dirName) {

        Log.d(Tag, "Creating Dir on sdcard...");

        if (dirName == null) {
            Log.e(Tag, "No Directory Name Specified!");
            return null;
        }

        if (Environment.getExternalStorageDirectory() == null) {

            File folder = new File(String.format("%s/%s",
                    Environment.getDataDirectory(), dirName));

            boolean success = false;

            if (!folder.exists()) {
                success = folder.mkdir();
                Log.d(Tag, "Created Dir on sdcard...");
            } else {
                success = true;
                Log.d(Tag, "Dir exists on sdcard...");
            }

            if (success) {
                return folder.getAbsolutePath();
            } else {
                Log.e(Tag, "Failed to create on sdcard...");
                return null;
            }
        } else {

            File folder = new File(String.format("%s/%s",
                    Environment.getExternalStorageDirectory(), dirName));

            boolean success = false;

            if (!folder.exists()) {
                success = folder.mkdir();
                Log.d(Tag, "Created Dir on sdcard...");
            } else {
                success = true;
                Log.d(Tag, "Dir exists on sdcard...");
            }

            if (success) {
                return folder.getAbsolutePath();
            } else {
                Log.e(Tag, "Failed to create on sdcard...");
                return null;
            }
        }
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        // if no network is available networkInfo will be null, otherwise check
        // if we are connected
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }

    public static class HTTP_METHODS {
        public static final String POST = "POST";
        public static final String GET = "GET";
    }

    public static void getHTTP(Context context, String url, final ParametricCallback parametricCallback) {

        Log.d(Tag, String.format("HTTP GET, FETCH URI: %s", url));


        Ion.with(context)
                .load(HTTP_METHODS.GET, url)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {

                        parametricCallback.call(result);
                    }
                });

    }

    private static String Tag = "XMARK";

    public static void showAlert(String title, String message, Context context) {
        showAlert(title, message, R.mipmap.ic_launcher, context);
    }

    public static void showAlert(String title, String message, int iconId,
                                 Context context) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            builder.setIcon(iconId);
            builder.setTitle(title);
            builder.setMessage(message);

            AlertDialog alert = builder.create();
            alert.show();
        } catch (Exception e) {
            Log.e(Tag, "Alert Error : " + e.getMessage());
        }
    }

    public static int getVersionNumber(Context context) {
        PackageInfo pinfo = null;
        try {
            pinfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return pinfo != null ? pinfo.versionCode : 1;
    }

    public static String getVersionName(Context context) {
        PackageInfo pinfo = null;
        try {
            pinfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return pinfo != null ? pinfo.versionName : "DEFAULT";
    }

    public static void autoUpdateAPK(String appName, final String apkUpdateUri, final Activity activity, final Handler handler) {
        final int notificationID = 1;
        final NotificationManager mNotifyManager = (NotificationManager) activity
                .getSystemService(Context.NOTIFICATION_SERVICE);
        final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(activity);
        mBuilder.setContentTitle(String.format("%s Download", appName))
                .setContentText("Update in progress...")
                .setSmallIcon(R.drawable.ic_notification_update);

        Intent notificationIntent = new Intent(activity, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(activity, 0,
                notificationIntent, 0);
        mBuilder.setContentIntent(contentIntent);

        Log.d(Tag, String.format("APK UPDATE FETCH URI: %s", apkUpdateUri));

        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... arg) {

                try {
                    URL url = new URL(apkUpdateUri);
                    HttpURLConnection c = (HttpURLConnection) url
                            .openConnection();
                    c.setRequestMethod("GET");
                    c.setDoOutput(true);
                    c.connect();

                    String PATH = APK.APK_DOWNLOAD_PATH;
                    File file = new File(PATH);
                    file.mkdirs();
                    File outputFile = new File(file, APK.NEW_APK_NAME);
                    if (outputFile.exists()) {
                        outputFile.delete();
                    }

					/*
                     * FileOutputStream fos = new FileOutputStream(outputFile);
					 *
					 * InputStream is = c.getInputStream();
					 *
					 * byte[] buffer = new byte[1024]; int len1 = 0; while
					 * ((len1 = is.read(buffer)) != -1) { fos.write(buffer, 0,
					 * len1); mBuilder.setProgress(0, 0, true);
					 * mNotifyManager.notify(notificationID, mBuilder.build());
					 * } fos.close(); is.close();
					 */

                    mBuilder.setProgress(0, 0, true);
                    mNotifyManager.notify(notificationID, mBuilder.build());

                    org.apache.commons.io.FileUtils.copyURLToFile(url,
                            outputFile);

                    // When the loop is finished, updates the notification
                    mBuilder.setContentText("Download complete")
                            // Removes the progress bar
                            .setProgress(0, 0, false);
                    mNotifyManager.notify(notificationID, mBuilder.build());

                    String filePath = outputFile.getAbsolutePath();

                    Log.d(Tag,
                            String.format("Downloaded APK FILE: %s", filePath));

                    if (!isAPKCorrupted(filePath)) {

                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.fromFile(new File(filePath)),
                                "application/vnd.android.package-archive");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // without
                        // this
                        // flag
                        // android
                        // returned
                        // a
                        // intent
                        // error!
                        activity.startActivity(intent);
                    } else {
                        handler.post(new Runnable() {

                            @Override
                            public void run() {
                                Utility.showAlert(
                                        "Auto-Update",
                                        "Sorry, but the downloaded APK seems to be corrupted!\n\nPlease inform the developers.",
                                        R.mipmap.ic_error, activity);
                            }
                        });
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            Utility.showAlert(
                                    "Auto-Update",
                                    "Sorry, but an error has prevented the app from self-updating...",
                                    R.mipmap.ic_error, activity);
                        }

                    });
                    Log.e(Tag, "Update error! " + e.getMessage());
                }

                return null;
            }

        }.execute(null, null, null);
    }

    public static boolean isAPKCorrupted(String filePath) {
        boolean corruptedApkFile = false;
        try {
            new JarFile(new File(filePath));
        } catch (Exception ex) {
            corruptedApkFile = true;
        }
        return corruptedApkFile;
    }

    public static void getPublishedVersionInfo(Context context, final ParametricCallbackJSONObject callbackJSONObject) {


        Utility
                .getHTTP(context, Utility.APK.APK_UPDATE_CONFIG_URI, new ParametricCallback() {
                    @Override
                    public void call(String jPublishedVersionInfo) {
                        Log.d(Tag, String.format("PUBLISHED APK INFO (REMOTE):\n %s \n",
                                jPublishedVersionInfo));

                        try {
                            callbackJSONObject.call(jPublishedVersionInfo != null ? new JSONObject(
                                    jPublishedVersionInfo).getJSONObject(APK.KEY_UPDATEINFO)
                                    : null);
                        } catch (JSONException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                });

    }

    public static boolean isPublishedVersionNewer(int versionCODE,
                                                  JSONObject publishedUpdateInfo) {

        if (publishedUpdateInfo != null) {
            if (publishedUpdateInfo.has(APK.KEY_VERSIONCODE)) {
                int newVersionCODE = 0;
                try {
                    newVersionCODE = publishedUpdateInfo
                            .getInt(APK.KEY_VERSIONCODE);
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return newVersionCODE > versionCODE;
            }
        }
        return true;
    }

    public static boolean isPublishedVersionSame(int versionCODE,
                                                 JSONObject publishedUpdateInfo) {

        if (publishedUpdateInfo != null) {
            if (publishedUpdateInfo.has(APK.KEY_VERSIONCODE)) {
                int newVersionCODE = 0;
                try {
                    newVersionCODE = publishedUpdateInfo
                            .getInt(APK.KEY_VERSIONCODE);
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return newVersionCODE == versionCODE;
            }
        }
        return false;
    }

    public static String getUpdateMessage(JSONObject publishedUpdateInfo) {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append(String.format("%s: %s\n\n",
                    publishedUpdateInfo.getString(APK.KEY_APPNAME),
                    publishedUpdateInfo.getString(APK.KEY_APPDESCRIPTION)));
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            sb.append(String.format("%s\n\n",
                    publishedUpdateInfo.getString(APK.KEY_UPDATEMESSAGE)));
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            sb.append(String.format("INCOMING VERSION is %s\n",
                    publishedUpdateInfo.getString(APK.KEY_VERSIONNAME)));
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return sb.toString();
    }

    public static String getUpdateURI(JSONObject publishedUpdateInfo,
                                      String defaultAPKURI) {
        return publishedUpdateInfo.optString(APK.KEY_APK_URI, defaultAPKURI);
    }
}
